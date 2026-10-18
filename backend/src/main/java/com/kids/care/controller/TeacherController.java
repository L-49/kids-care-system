package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import com.kids.care.entity.Teacher;
import com.kids.care.service.ITeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教职工 接口（教师可查看通讯录，仅园长可维护）
 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole({Roles.ADMIN, Roles.TEACHER})
public class TeacherController {

    private final ITeacherService teacherService;

    /** 分页查询 */
    @GetMapping("/page")
    public Result<List<Teacher>> page(@RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) Integer status) {
        IPage<Teacher> page = teacherService.pageQuery(pageNum, pageSize, keyword, role, status);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @GetMapping("/all")
    public Result<List<Teacher>> all() {
        return Result.ok(teacherService.listAll());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Teacher> info(@PathVariable Long id) {
        return Result.ok(teacherService.getById(id));
    }

    /** 新增（仅园长） */
    @RequireRole({Roles.ADMIN})
    @PostMapping
    public Result<Void> add(@RequestBody Teacher entity) {
        teacherService.save(entity);
        return Result.ok();
    }

    /** 修改（仅园长） */
    @RequireRole({Roles.ADMIN})
    @PutMapping
    public Result<Void> edit(@RequestBody Teacher entity) {
        teacherService.updateById(entity);
        return Result.ok();
    }

    /** 删除（仅园长） */
    @RequireRole({Roles.ADMIN})
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        teacherService.removeByIds(ids);
        return Result.ok();
    }
}
