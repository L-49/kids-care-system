package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
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

    /** 分页查询 */
    @GetMapping("/page")
    public Result<List<Notice>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String type) {
        IPage<Notice> page = noticeService.pageQuery(pageNum, pageSize, keyword, type);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @GetMapping("/all")
    public Result<List<Notice>> all() {
        return Result.ok(noticeService.listAll());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Notice> info(@PathVariable Long id) {
        return Result.ok(noticeService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Notice entity) {
        noticeService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody Notice entity) {
        noticeService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        noticeService.removeByIds(ids);
        return Result.ok();
    }
}
