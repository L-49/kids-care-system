package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import com.kids.care.common.UserContext;
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

    /** 分页查询：家长只能看到自己孩子的请假 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER, Roles.PARENT})
    @GetMapping("/page")
    public Result<List<LeaveRequest>> page(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status) {
        IPage<LeaveRequest> page = leaveRequestService.pageQuery(
                pageNum, pageSize, keyword, status, UserContext.scopeChildId());
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/all")
    public Result<List<LeaveRequest>> all() {
        return Result.ok(leaveRequestService.listAll());
    }

    /** 详情 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/{id}")
    public Result<LeaveRequest> info(@PathVariable Long id) {
        return Result.ok(leaveRequestService.getById(id));
    }

    /** 新增（家长可为自己孩子提交请假） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER, Roles.PARENT})
    @PostMapping
    public Result<Void> add(@RequestBody LeaveRequest entity) {
        Long childId = UserContext.scopeChildId();
        if (childId != null) {
            // 家长只能给自己孩子请假，忽略请求体中的 childId
            entity.setChildId(childId);
        }
        leaveRequestService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PutMapping
    public Result<Void> edit(@RequestBody LeaveRequest entity) {
        leaveRequestService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        leaveRequestService.removeByIds(ids);
        return Result.ok();
    }

    /** 审批：审批人取当前登录人，前端不可伪造 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PutMapping("/approve/{id}")
    public Result<Void> approve(@PathVariable Long id, @RequestParam boolean pass) {
        leaveRequestService.approve(id, pass, UserContext.require().realName());
        return Result.ok();
    }
}
