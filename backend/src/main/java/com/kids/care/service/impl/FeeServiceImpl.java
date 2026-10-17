package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.Fee;
import com.kids.care.mapper.FeeMapper;
import com.kids.care.service.IFeeService;
import com.kids.care.common.BizException;
import com.kids.care.entity.Child;
import com.kids.care.mapper.ChildMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收费记录 业务实现
 */
@Service
@RequiredArgsConstructor
public class FeeServiceImpl extends ServiceImpl<FeeMapper, Fee> implements IFeeService {

    private final ChildMapper childMapper;
    @Override
    public IPage<Fee> pageQuery(int pageNum, int pageSize, String keyword, String feeMonth, Integer payStatus) {
        LambdaQueryWrapper<Fee> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            List<Long> childIds = childMapper.selectList(new LambdaQueryWrapper<Child>()
                            .select(Child::getId).like(Child::getName, keyword))
                    .stream().map(Child::getId).toList();
            if (childIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            qw.in(Fee::getChildId, childIds);
        }
        if (StringUtils.hasText(feeMonth)) {
            qw.eq(Fee::getFeeMonth, feeMonth);
        }
        if (payStatus != null) {
            qw.eq(Fee::getPayStatus, payStatus);
        }
        qw.orderByDesc(Fee::getFeeMonth).orderByAsc(Fee::getPayStatus).orderByDesc(Fee::getId);
        IPage<Fee> page = page(new Page<>(pageNum, pageSize), qw);
        fillChildName(page.getRecords());
        return page;
    }

    @Override
    public List<Fee> listAll() {
        List<Fee> list = list(new LambdaQueryWrapper<Fee>().orderByDesc(Fee::getId).last("limit 200"));
        fillChildName(list);
        return list;
    }

    /** 缴费登记 */
    @Override
    public boolean pay(Long id) {
        Fee fee = getById(id);
        if (fee == null) {
            throw new BizException("收费记录不存在");
        }
        if (fee.getPayStatus() != null && fee.getPayStatus() == 1) {
            throw new BizException("该笔费用已缴纳，请勿重复操作");
        }
        fee.setPayStatus(1);
        fee.setPayTime(LocalDateTime.now());
        return updateById(fee);
    }

    private void fillChildName(List<Fee> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> childIds = list.stream().map(Fee::getChildId).filter(Objects::nonNull).distinct().toList();
        if (childIds.isEmpty()) {
            return;
        }
        Map<Long, String> names = childMapper.selectBatchIds(childIds).stream()
                .collect(Collectors.toMap(Child::getId, Child::getName));
        list.forEach(f -> f.setChildName(names.get(f.getChildId())));
    }
}
