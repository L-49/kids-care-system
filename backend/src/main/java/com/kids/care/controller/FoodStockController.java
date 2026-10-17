package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
import com.kids.care.entity.FoodStock;
import com.kids.care.service.IFoodStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /** 详情 */
    @GetMapping("/{id}")
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
}
