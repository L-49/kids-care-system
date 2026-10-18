package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import com.kids.care.entity.SysLog;
import com.kids.care.service.ISysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作日志 接口
 */
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
@RequireRole({Roles.ADMIN})
public class SysLogController {

    private final ISysLogService sysLogService;

    /** 分页查询 */
    @GetMapping("/page")
    public Result<List<SysLog>> page(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String method) {
        IPage<SysLog> page = sysLogService.pageQuery(pageNum, pageSize, keyword, method);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 最近 50 条 */
    @GetMapping("/all")
    public Result<List<SysLog>> all() {
        return Result.ok(sysLogService.listAll());
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        sysLogService.removeByIds(ids);
        return Result.ok();
    }

    /** 清空日志 */
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        sysLogService.clean();
        return Result.ok();
    }
}
