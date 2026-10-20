package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import com.kids.care.entity.Notice;
import com.kids.care.service.INoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知公告 接口
 */
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final INoticeService noticeService;

    /** 分页查询（status 为空时不展示已撤回） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER, Roles.PARENT})
    @GetMapping("/page")
    public Result<List<Notice>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) Integer status) {
        IPage<Notice> page = noticeService.pageQuery(pageNum, pageSize, keyword, type, status);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（对外可见：仅已发布且已到点） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER, Roles.PARENT})
    @GetMapping("/all")
    public Result<List<Notice>> all() {
        return Result.ok(noticeService.listAll());
    }

    /** 详情（家长只能看到已发布且已到点的公告，其余按不存在处理） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER, Roles.PARENT})
    @GetMapping("/{id}")
    public Result<Notice> info(@PathVariable Long id) {
        return Result.ok(noticeService.visibleDetail(id));
    }

    /** 新增 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Notice entity) {
        noticeService.saveNotice(entity);
        return Result.ok();
    }

    /** 修改 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody Notice entity) {
        noticeService.updateNotice(entity);
        return Result.ok();
    }

    /** 立即发布：草稿 / 定时公告立刻对外可见 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publishNow(id);
        return Result.ok();
    }

    /** 撤回：已发布公告置为已撤回（保留记录，不物理删除） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PostMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        noticeService.revoke(id);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @RequireRole({Roles.ADMIN})
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        noticeService.removeByIds(ids);
        return Result.ok();
    }
}
