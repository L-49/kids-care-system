package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
import com.kids.care.entity.Child;
import com.kids.care.service.IChildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 幼儿档案 接口
 */
@RestController
@RequestMapping("/api/child")
@RequiredArgsConstructor
public class ChildController {

    private final IChildService childService;

    /** 分页查询（incompleteOnly=true 只看待补全档案） */
    @GetMapping("/page")
    public Result<List<Child>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long clazzId,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) Boolean incompleteOnly) {
        IPage<Child> page = childService.pageQuery(pageNum, pageSize, keyword, clazzId, status, incompleteOnly);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @GetMapping("/all")
    public Result<List<Child>> all() {
        return Result.ok(childService.listAll());
    }

    /** 有过敏史的幼儿（膳食模块标注用） */
    @GetMapping("/allergy")
    public Result<List<Child>> allergy() {
        return Result.ok(childService.listAllergy());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Child> info(@PathVariable Long id) {
        return Result.ok(childService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Child entity) {
        childService.saveChild(entity);
        return Result.ok();
    }

    /** 修改 */
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody Child entity) {
        childService.updateChild(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        childService.removeByIds(ids);
        return Result.ok();
    }
}
