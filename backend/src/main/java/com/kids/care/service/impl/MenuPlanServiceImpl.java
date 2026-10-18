package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.common.BizException;
import com.kids.care.entity.MenuPlan;
import com.kids.care.mapper.MenuPlanMapper;
import com.kids.care.service.IMenuPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
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
        return page(new Page<>(pageNum, pageSize), qw);
    }

    @Override
    public List<MenuPlan> listAll() {
        return list(new LambdaQueryWrapper<MenuPlan>()
                .orderByDesc(MenuPlan::getMenuDate).last("limit 60"));
    }

    @Override
    public List<MenuPlan> listWeek(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return list(new LambdaQueryWrapper<MenuPlan>()
                .between(MenuPlan::getMenuDate, weekStart, weekEnd)
                .last("order by menu_date, field(meal_type, '早餐', '午餐', '午点')"));
    }

    /**
     * 复制上周食谱：按"同星期几"平移 7 天。
     * 目标日期已有食谱时跳过，避免覆盖人工调整过的排餐。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyFromLastWeek(LocalDate weekStart) {
        LocalDate srcStart = weekStart.minusDays(7);
        LocalDate srcEnd = weekStart.minusDays(1);
        List<MenuPlan> source = list(new LambdaQueryWrapper<MenuPlan>()
                .between(MenuPlan::getMenuDate, srcStart, srcEnd)
                .orderByAsc(MenuPlan::getMenuDate));
        if (source.isEmpty()) {
            throw new BizException("上一周（" + srcStart + " ~ " + srcEnd + "）没有食谱，无法复制");
        }

        int copied = 0;
        for (MenuPlan src : source) {
            LocalDate target = src.getMenuDate().plusDays(7);
            boolean exists = count(new LambdaQueryWrapper<MenuPlan>()
                    .eq(MenuPlan::getMenuDate, target)
                    .eq(MenuPlan::getMealType, src.getMealType())) > 0;
            if (exists) {
                continue;
            }
            Long deletedId = baseMapper.findDeletedId(target, src.getMealType());
            if (deletedId != null) {
                baseMapper.revive(deletedId, src.getDishes(), src.getNote(), src.getPublisher());
            } else {
                MenuPlan plan = new MenuPlan();
                plan.setMenuDate(target);
                plan.setMealType(src.getMealType());
                plan.setDishes(src.getDishes());
                plan.setNote(src.getNote());
                plan.setPublisher(src.getPublisher());
                super.save(plan);
            }
            copied++;
        }
        return copied;
    }

    /**
     * 新增食谱：同一日期同一餐次只能有一条。
     * 唯一键 uk_menu 不含 deleted，因此"删了想重录"时必须复活旧记录，而不是插入新行。
     */
    @Override
    public boolean save(MenuPlan entity) {
        if (entity.getMenuDate() == null || !StringUtils.hasText(entity.getMealType())) {
            throw new BizException("请选择日期和餐次");
        }
        if (!StringUtils.hasText(entity.getDishes())) {
            throw new BizException("请填写菜品");
        }
        boolean exists = count(new LambdaQueryWrapper<MenuPlan>()
                .eq(MenuPlan::getMenuDate, entity.getMenuDate())
                .eq(MenuPlan::getMealType, entity.getMealType())) > 0;
        if (exists) {
            throw new BizException("该日期这一餐次已有食谱，请直接编辑原记录");
        }
        Long deletedId = baseMapper.findDeletedId(entity.getMenuDate(), entity.getMealType());
        if (deletedId != null) {
            return baseMapper.revive(deletedId, entity.getDishes(), entity.getNote(), entity.getPublisher()) > 0;
        }
        return super.save(entity);
    }
}
