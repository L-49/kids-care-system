package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.FoodStock;

import java.util.List;

/**
 * 食材库存 业务接口
 */
public interface IFoodStockService extends IService<FoodStock> {

    /** 分页查询 */
    IPage<FoodStock> pageQuery(int pageNum, int pageSize, String keyword, String category, Boolean warnOnly);

    /** 全部列表 */
    List<FoodStock> listAll();
}
