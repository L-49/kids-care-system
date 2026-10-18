package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import com.kids.care.entity.MenuPlan;
import com.kids.care.service.IMenuPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 每周食谱 接口（三端可查看，园长/教师可维护）
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@RequireRole({Roles.ADMIN, Roles.TEACHER, Roles.PARENT})
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

    /** 周视图：weekStart 为周一，如 2026-10-12 */
    @GetMapping("/week")
    public Result<List<MenuPlan>> week(@RequestParam String weekStart) {
        return Result.ok(menuPlanService.listWeek(LocalDate.parse(weekStart)));
    }

    /** 一键复制上周食谱到本周，返回复制条数 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PostMapping("/copy-last-week")
    public Result<Integer> copyLastWeek(@RequestParam String weekStart) {
        return Result.ok(menuPlanService.copyFromLastWeek(LocalDate.parse(weekStart)));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<MenuPlan> info(@PathVariable Long id) {
        return Result.ok(menuPlanService.getById(id));
    }

    /** 新增 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PostMapping
    public Result<Void> add(@RequestBody MenuPlan entity) {
        menuPlanService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PutMapping
    public Result<Void> edit(@RequestBody MenuPlan entity) {
        menuPlanService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        menuPlanService.removeByIds(ids);
        return Result.ok();
    }
}
