package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.MenuPlan;

import java.time.LocalDate;
import java.util.List;

/**
 * 每周食谱 业务接口
 */
public interface IMenuPlanService extends IService<MenuPlan> {

    /** 分页查询 */
    IPage<MenuPlan> pageQuery(int pageNum, int pageSize, String keyword, String mealType, String menuDate);

    /** 全部列表 */
    List<MenuPlan> listAll();

    /**
     * 按周查询：weekStart 为周一，返回该周 7 天的全部食谱
     */
    List<MenuPlan> listWeek(LocalDate weekStart);

    /**
     * 把上一周同星期几的食谱复制到本周（已排餐的日期跳过，不覆盖人工调整）
     *
     * @return 实际复制条数
     */
    int copyFromLastWeek(LocalDate weekStart);
}
