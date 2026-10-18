package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import com.kids.care.common.UserContext;
import com.kids.care.entity.Attendance;
import com.kids.care.service.IAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考勤记录 接口
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final IAttendanceService attendanceService;

    /** 分页查询：家长只能看到自己孩子的考勤 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER, Roles.PARENT})
    @GetMapping("/page")
    public Result<List<Attendance>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long clazzId,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String attDate) {
        IPage<Attendance> page = attendanceService.pageQuery(
                pageNum, pageSize, keyword, clazzId, status, attDate, UserContext.scopeChildId());
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/all")
    public Result<List<Attendance>> all() {
        return Result.ok(attendanceService.listAll());
    }

    /** 详情 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/{id}")
    public Result<Attendance> info(@PathVariable Long id) {
        return Result.ok(attendanceService.getById(id));
    }

    /** 新增 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PostMapping
    public Result<Void> add(@RequestBody Attendance entity) {
        attendanceService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PutMapping
    public Result<Void> edit(@RequestBody Attendance entity) {
        attendanceService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        attendanceService.removeByIds(ids);
        return Result.ok();
    }
}
