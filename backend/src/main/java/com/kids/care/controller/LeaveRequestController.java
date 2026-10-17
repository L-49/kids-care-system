package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
import com.kids.care.entity.LeaveRequest;
import com.kids.care.service.ILeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 请假申请 接口
 */
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final ILeaveRequestService leaveRequestService;

    /** 分页查询 */
    @GetMapping("/page")
    public Result<List<LeaveRequest>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status) {
        IPage<LeaveRequest> page = leaveRequestService.pageQuery(pageNum, pageSize, keyword, status);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @GetMapping("/all")
    public Result<List<LeaveRequest>> all() {
        return Result.ok(leaveRequestService.listAll());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<LeaveRequest> info(@PathVariable Long id) {
        return Result.ok(leaveRequestService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@RequestBody LeaveRequest entity) {
        leaveRequestService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @PutMapping
    public Result<Void> edit(@RequestBody LeaveRequest entity) {
        leaveRequestService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        leaveRequestService.removeByIds(ids);
        return Result.ok();
    }

    /** 审批（pass=true 通过，false 驳回） */
    @PutMapping("/approve/{id}")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam boolean pass,
                                @RequestParam String approver) {
        leaveRequestService.approve(id, pass, approver);
        return Result.ok();
    }
}
