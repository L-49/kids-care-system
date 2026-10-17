package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
import com.kids.care.entity.MenuPlan;
import com.kids.care.service.IMenuPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 每周食谱 接口
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuPlanController {

    private final IMenuPlanService menuPlanService;

    /** 分页查询 */
    @GetMapping("/page")
    public Result<List<MenuPlan>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String mealType,
                                         @RequestParam(required = false) String menuDate) {
        IPage<MenuPlan> page = menuPlanService.pageQuery(pageNum, pageSize, keyword, mealType, menuDate);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @GetMapping("/all")
    public Result<List<MenuPlan>> all() {
        return Result.ok(menuPlanService.listAll());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<MenuPlan> info(@PathVariable Long id) {
        return Result.ok(menuPlanService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@RequestBody MenuPlan entity) {
        menuPlanService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @PutMapping
    public Result<Void> edit(@RequestBody MenuPlan entity) {
        menuPlanService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        menuPlanService.removeByIds(ids);
        return Result.ok();
    }
}
