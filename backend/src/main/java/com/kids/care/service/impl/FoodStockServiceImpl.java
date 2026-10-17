package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.FoodStock;
import com.kids.care.mapper.FoodStockMapper;
import com.kids.care.service.IFoodStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 食材库存 业务实现
 */
@Service
@RequiredArgsConstructor
public class FoodStockServiceImpl extends ServiceImpl<FoodStockMapper, FoodStock> implements IFoodStockService {

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
}
