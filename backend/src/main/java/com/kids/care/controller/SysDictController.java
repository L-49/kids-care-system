package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import com.kids.care.entity.SysDict;
import com.kids.care.service.ISysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典 接口
 */
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final ISysDictService sysDictService;

    /** 分页查询 */
    @RequireRole({Roles.ADMIN})
    @GetMapping("/page")
    public Result<List<SysDict>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String dictType) {
        IPage<SysDict> page = sysDictService.pageQuery(pageNum, pageSize, keyword, dictType);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @RequireRole({Roles.ADMIN})
    @GetMapping("/all")
    public Result<List<SysDict>> all() {
        return Result.ok(sysDictService.listAll());
    }

    /** 按类型取字典项（前端下拉框，如 notice_type；各角色表单都要用，放行全部登录角色） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER, Roles.PARENT})
    @GetMapping("/type/{dictType}")
    public Result<List<SysDict>> byType(@PathVariable String dictType) {
        return Result.ok(sysDictService.listByType(dictType));
    }

    /** 详情 */
    @RequireRole({Roles.ADMIN})
    @GetMapping("/{id}")
    public Result<SysDict> info(@PathVariable Long id) {
        return Result.ok(sysDictService.getById(id));
    }

    /** 新增 */
    @RequireRole({Roles.ADMIN})
    @PostMapping
    public Result<Void> add(@RequestBody SysDict entity) {
        sysDictService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @RequireRole({Roles.ADMIN})
    @PutMapping
    public Result<Void> edit(@RequestBody SysDict entity) {
        sysDictService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @RequireRole({Roles.ADMIN})
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        sysDictService.removeByIds(ids);
        return Result.ok();
    }
}
