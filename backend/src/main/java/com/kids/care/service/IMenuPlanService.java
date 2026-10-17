package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.MenuPlan;

import java.util.List;

/**
 * 每周食谱 业务接口
 */
public interface IMenuPlanService extends IService<MenuPlan> {

    /** 分页查询 */
    IPage<MenuPlan> pageQuery(int pageNum, int pageSize, String keyword, String mealType, String menuDate);

    /** 全部列表 */
    List<MenuPlan> listAll();
}
