package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import com.kids.care.common.UserContext;
import com.kids.care.dto.ImportResult;
import com.kids.care.entity.Child;
import com.kids.care.entity.ChildChangeLog;
import com.kids.care.service.IChildService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 幼儿档案 接口
 */
@RestController
@RequestMapping("/api/child")
@RequiredArgsConstructor
public class ChildController {

    private final IChildService childService;

    /** 家长端：我的孩子（档案 + 今日考勤 + 近 7 天趋势 + 账单 + 请假） */
    @RequireRole({Roles.PARENT})
    @GetMapping("/mine")
    public Result<Map<String, Object>> mine() {
        return Result.ok(childService.myChildDashboard(UserContext.scopeChildId()));
    }

    /** 分页查询（incompleteOnly=true 只看待补全档案） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/page")
    public Result<List<Child>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long clazzId,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) Boolean incompleteOnly) {
        IPage<Child> page = childService.pageQuery(pageNum, pageSize, keyword, clazzId, status, incompleteOnly);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/all")
    public Result<List<Child>> all() {
        return Result.ok(childService.listAll());
    }

    /** 有过敏史的幼儿（膳食模块标注用） */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/allergy")
    public Result<List<Child>> allergy() {
        return Result.ok(childService.listAllergy());
    }

    /** 下载 Excel 导入模板 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/import/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("幼儿档案导入模板", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        childService.writeImportTemplate(response.getOutputStream());
    }

    /** Excel 批量导入：逐行校验，成功的入库，失败的返回行号与原因 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PostMapping("/import")
    public Result<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.ok(childService.importChildren(file));
    }

    /**
     * 导出 Excel：跟随列表当前的筛选条件（关键字/班级/状态/仅看待补全），不是导全表。
     *
     * <p>先在内存里把 xlsx 生成完，成功后才写响应头与响应体。原因是全局异常处理返回的是
     * HTTP 200 + JSON：如果一边写流一边出错，前端会下到一个「能保存但打不开」的假 xlsx。
     */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long clazzId,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Boolean incompleteOnly,
                       HttpServletResponse response) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        childService.exportChildren(keyword, clazzId, status, incompleteOnly, buffer);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String name = "幼儿档案_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        response.setContentLength(buffer.size());
        buffer.writeTo(response.getOutputStream());
    }

    /** 变更留痕：某幼儿的完整变更记录 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/{id}/history")
    public Result<List<ChildChangeLog>> history(@PathVariable Long id) {
        return Result.ok(childService.listHistory(id));
    }

    /** 详情 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @GetMapping("/{id}")
    public Result<Child> info(@PathVariable Long id) {
        return Result.ok(childService.getById(id));
    }

    /** 新增 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Child entity) {
        childService.saveChild(entity);
        return Result.ok();
    }

    /** 修改 */
    @RequireRole({Roles.ADMIN, Roles.TEACHER})
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody Child entity) {
        childService.updateChild(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @RequireRole({Roles.ADMIN})
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        childService.removeByIds(ids);
        return Result.ok();
    }
}
