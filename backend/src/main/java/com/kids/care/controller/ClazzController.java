package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
import com.kids.care.entity.Clazz;
import com.kids.care.service.IClazzService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级 接口
 */
@RestController
@RequestMapping("/api/clazz")
@RequiredArgsConstructor
public class ClazzController {

    private final IClazzService clazzService;

    /** 分页查询 */
    @GetMapping("/page")
    public Result<List<Clazz>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword) {
        IPage<Clazz> page = clazzService.pageQuery(pageNum, pageSize, keyword);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @GetMapping("/all")
    public Result<List<Clazz>> all() {
        return Result.ok(clazzService.listAll());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Clazz> info(@PathVariable Long id) {
        return Result.ok(clazzService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@RequestBody Clazz entity) {
        clazzService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @PutMapping
    public Result<Void> edit(@RequestBody Clazz entity) {
        clazzService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        clazzService.removeByIds(ids);
        return Result.ok();
    }
}
