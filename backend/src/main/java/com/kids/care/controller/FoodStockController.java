package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
import com.kids.care.entity.FoodStock;
import com.kids.care.entity.StockRecord;
import com.kids.care.service.IFoodStockService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 食材库存 接口
 */
@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
public class FoodStockController {

    private final IFoodStockService foodStockService;

    /** 分页查询 */
    @GetMapping("/page")
    public Result<List<FoodStock>> page(@RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) Boolean warnOnly) {
        IPage<FoodStock> page = foodStockService.pageQuery(pageNum, pageSize, keyword, category, warnOnly);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @GetMapping("/all")
    public Result<List<FoodStock>> all() {
        return Result.ok(foodStockService.listAll());
    }

    /** 库存概览：品类数、预警数、本月出入库统计 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(foodStockService.overview());
    }

    /** 出入库流水分页 */
    @GetMapping("/record/page")
    public Result<List<StockRecord>> recordPage(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) Long foodId,
                                                @RequestParam(required = false) String type,
                                                @RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        IPage<StockRecord> page = foodStockService.recordPage(pageNum, pageSize, foodId, type, startDate, endDate);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 入库 / 出库 */
    @PostMapping("/{id}/stock")
    public Result<StockRecord> changeStock(@PathVariable Long id, @RequestBody StockChangeRequest req) {
        return Result.ok(foodStockService.changeStock(id, req.getType(), req.getQuantity(), req.getRemark()));
    }

    /** 详情 */
    @GetMapping("/{id:\\d+}")
    public Result<FoodStock> info(@PathVariable Long id) {
        return Result.ok(foodStockService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@RequestBody FoodStock entity) {
        foodStockService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @PutMapping
    public Result<Void> edit(@RequestBody FoodStock entity) {
        foodStockService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        foodStockService.removeByIds(ids);
        return Result.ok();
    }

    /** 出入库请求体 */
    @Data
    public static class StockChangeRequest {
        /** IN 入库 / OUT 出库 */
        private String type;
        /** 数量（正数） */
        private BigDecimal quantity;
        /** 备注 */
        private String remark;
    }
}
