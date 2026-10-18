package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.common.BizException;
import com.kids.care.common.TokenStore;
import com.kids.care.common.UserContext;
import com.kids.care.entity.FoodStock;
import com.kids.care.entity.StockRecord;
import com.kids.care.mapper.FoodStockMapper;
import com.kids.care.mapper.StockRecordMapper;
import com.kids.care.service.IFoodStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 食材库存 业务实现
 */
@Service
@RequiredArgsConstructor
public class FoodStockServiceImpl extends ServiceImpl<FoodStockMapper, FoodStock> implements IFoodStockService {

    private final StockRecordMapper stockRecordMapper;

    @Override
    public IPage<FoodStock> pageQuery(int pageNum, int pageSize, String keyword, String category, Boolean warnOnly) {
        LambdaQueryWrapper<FoodStock> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(FoodStock::getName, keyword);
        }
        if (StringUtils.hasText(category)) {
            qw.eq(FoodStock::getCategory, category);
        }
        if (Boolean.TRUE.equals(warnOnly)) {
            qw.apply("quantity <= warn_line");
        }
        qw.orderByAsc(FoodStock::getCategory).orderByAsc(FoodStock::getId);
        IPage<FoodStock> page = page(new Page<>(pageNum, pageSize), qw);
        page.getRecords().forEach(s -> s.setWarn(
                s.getQuantity() != null && s.getWarnLine() != null
                        && s.getQuantity().compareTo(s.getWarnLine()) <= 0));
        return page;
    }

    @Override
    public List<FoodStock> listAll() {
        return list(new LambdaQueryWrapper<FoodStock>()
                .orderByAsc(FoodStock::getCategory).orderByAsc(FoodStock::getId));
    }

    /**
     * 入库 / 出库：先原子更新库存（出库不允许减成负数），再写一条流水。
     * 两步在同一事务内，流水失败会回滚库存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockRecord changeStock(Long foodId, String type, BigDecimal quantity, String remark) {
        if (foodId == null) {
            throw new BizException("请选择食材");
        }
        boolean in = StockRecord.TYPE_IN.equalsIgnoreCase(type);
        boolean out = StockRecord.TYPE_OUT.equalsIgnoreCase(type);
        if (!in && !out) {
            throw new BizException("操作类型只能是入库或出库");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("数量必须大于 0");
        }
        FoodStock food = getById(foodId);
        if (food == null) {
            throw new BizException("食材不存在或已删除");
        }

        BigDecimal delta = in ? quantity : quantity.negate();
        int rows = baseMapper.addQuantity(foodId, delta);
        if (rows == 0) {
            throw new BizException("库存不足：「" + food.getName() + "」当前库存 "
                    + food.getQuantity() + food.getUnit() + "，无法出库 " + quantity + food.getUnit());
        }

        // 以更新后的实际库存为准，避免并发下 before/after 记错
        FoodStock after = getById(foodId);
        StockRecord record = new StockRecord();
        record.setFoodId(foodId);
        record.setFoodName(food.getName());
        record.setType(in ? StockRecord.TYPE_IN : StockRecord.TYPE_OUT);
        record.setTypeText(in ? "入库" : "出库");
        record.setQuantity(quantity);
        record.setBeforeQty(after.getQuantity().subtract(delta));
        record.setAfterQty(after.getQuantity());
        record.setUnit(food.getUnit());
        TokenStore.LoginUser user = UserContext.get();
        record.setOperator(user == null ? "系统" : user.realName());
        record.setRemark(StringUtils.hasText(remark) ? remark : (in ? "采购入库" : "厨房领用"));
        record.setCreateTime(LocalDateTime.now());
        stockRecordMapper.insert(record);
        return record;
    }

    @Override
    public IPage<StockRecord> recordPage(int pageNum, int pageSize, Long foodId, String type,
                                         String startDate, String endDate) {
        LambdaQueryWrapper<StockRecord> qw = new LambdaQueryWrapper<>();
        if (foodId != null) {
            qw.eq(StockRecord::getFoodId, foodId);
        }
        if (StringUtils.hasText(type)) {
            qw.eq(StockRecord::getType, type);
        }
        if (StringUtils.hasText(startDate)) {
            qw.ge(StockRecord::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            qw.le(StockRecord::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        qw.orderByDesc(StockRecord::getId);
        IPage<StockRecord> page = stockRecordMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        page.getRecords().forEach(r -> r.setTypeText(StockRecord.TYPE_IN.equals(r.getType()) ? "入库" : "出库"));
        return page;
    }

    @Override
    public Map<String, Object> overview() {
        long kinds = count();
        long warn = count(new LambdaQueryWrapper<FoodStock>().apply("quantity <= warn_line"));

        long inTimes = 0;
        long outTimes = 0;
        BigDecimal inQty = BigDecimal.ZERO;
        BigDecimal outQty = BigDecimal.ZERO;
        for (Map<String, Object> row : stockRecordMapper.monthSummary()) {
            Object type = row.get("type");
            Object times = row.get("times");
            Object qty = row.get("qty");
            if (StockRecord.TYPE_IN.equals(type)) {
                inTimes = times == null ? 0 : ((Number) times).longValue();
                inQty = qty == null ? BigDecimal.ZERO : new BigDecimal(qty.toString());
            } else if (StockRecord.TYPE_OUT.equals(type)) {
                outTimes = times == null ? 0 : ((Number) times).longValue();
                outQty = qty == null ? BigDecimal.ZERO : new BigDecimal(qty.toString());
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kinds", kinds);
        data.put("warnCount", warn);
        data.put("monthInTimes", inTimes);
        data.put("monthInQty", inQty);
        data.put("monthOutTimes", outTimes);
        data.put("monthOutQty", outQty);
        data.put("month", LocalDate.now().toString().substring(0, 7));
        return data;
    }
}
