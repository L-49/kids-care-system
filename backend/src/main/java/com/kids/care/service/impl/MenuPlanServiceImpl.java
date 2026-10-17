package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.MenuPlan;
import com.kids.care.mapper.MenuPlanMapper;
import com.kids.care.service.IMenuPlanService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 每周食谱 业务实现
 */
@Service
@RequiredArgsConstructor
public class MenuPlanServiceImpl extends ServiceImpl<MenuPlanMapper, MenuPlan> implements IMenuPlanService {

    @Override
    public IPage<MenuPlan> pageQuery(int pageNum, int pageSize, String keyword, String mealType, String menuDate) {
        LambdaQueryWrapper<MenuPlan> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(MenuPlan::getDishes, keyword);
        }
        if (StringUtils.hasText(mealType)) {
            qw.eq(MenuPlan::getMealType, mealType);
        }
        if (StringUtils.hasText(menuDate)) {
            qw.eq(MenuPlan::getMenuDate, LocalDate.parse(menuDate));
        }
        // 先按日期倒序，同一天内按早餐、午餐、午点顺序排列
        qw.last("order by menu_date desc, field(meal_type, '早餐', '午餐', '午点')");
        IPage<MenuPlan> page = page(new Page<>(pageNum, pageSize), qw);
        return page;
    }

    @Override
    public List<MenuPlan> listAll() {
        return list(new LambdaQueryWrapper<MenuPlan>()
                .orderByDesc(MenuPlan::getMenuDate).last("limit 60"));
    }
}
