package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.FoodStock;
import com.kids.care.entity.StockRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 食材库存 业务接口
 */
public interface IFoodStockService extends IService<FoodStock> {

    /** 分页查询 */
    IPage<FoodStock> pageQuery(int pageNum, int pageSize, String keyword, String category, Boolean warnOnly);

    /** 全部列表 */
    List<FoodStock> listAll();

    /**
     * 入库 / 出库
     *
     * @param foodId   食材ID
     * @param type     IN 入库、OUT 出库
     * @param quantity 数量（正数）
     * @param remark   备注
     * @return 本次流水
     */
    StockRecord changeStock(Long foodId, String type, BigDecimal quantity, String remark);

    /** 出入库流水分页 */
    IPage<StockRecord> recordPage(int pageNum, int pageSize, Long foodId, String type,
                                  String startDate, String endDate);

    /** 库存概览：品类数、预警数、本月出入库笔数与数量 */
    Map<String, Object> overview();
}
