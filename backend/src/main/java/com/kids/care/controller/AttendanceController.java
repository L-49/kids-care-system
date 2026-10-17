package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
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

    /** 分页查询 */
    @GetMapping("/page")
    public Result<List<Attendance>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long clazzId,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String attDate) {
        IPage<Attendance> page = attendanceService.pageQuery(pageNum, pageSize, keyword, clazzId, status, attDate);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @GetMapping("/all")
    public Result<List<Attendance>> all() {
        return Result.ok(attendanceService.listAll());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Attendance> info(@PathVariable Long id) {
        return Result.ok(attendanceService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@RequestBody Attendance entity) {
        attendanceService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @PutMapping
    public Result<Void> edit(@RequestBody Attendance entity) {
        attendanceService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        attendanceService.removeByIds(ids);
        return Result.ok();
    }
}
