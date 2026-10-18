package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import com.kids.care.common.UserContext;
import com.kids.care.entity.Fee;
import com.kids.care.service.IFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收费记录 接口
 */
@RestController
@RequestMapping("/api/fee")
@RequiredArgsConstructor
public class FeeController {

    private final IFeeService feeService;

    /** 分页查询：家长只能看到自己孩子的费用；教师可查看全部以便代收 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER, Roles.PARENT})
    @GetMapping("/page")
    public Result<List<Fee>> page(@RequestParam(defaultValue = "1") int pageNum,
                                  @RequestParam(defaultValue = "10") int pageSize,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String feeMonth,
                                  @RequestParam(required = false) Integer payStatus) {
        IPage<Fee> page = feeService.pageQuery(
                pageNum, pageSize, keyword, feeMonth, payStatus, UserContext.scopeChildId());
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/all")
    public Result<List<Fee>> all() {
        return Result.ok(feeService.listAll());
    }

    /** 详情 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/{id}")
    public Result<Fee> info(@PathVariable Long id) {
        return Result.ok(feeService.getById(id));
    }

    /** 新增 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PostMapping
    public Result<Void> add(@RequestBody Fee entity) {
        feeService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PutMapping
    public Result<Void> edit(@RequestBody Fee entity) {
        feeService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        feeService.removeByIds(ids);
        return Result.ok();
    }

    /** 缴费登记 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PutMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id) {
        feeService.pay(id);
        return Result.ok();
    }
}
