package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
import com.kids.care.dto.ImportResult;
import com.kids.care.entity.Child;
import com.kids.care.service.IChildService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 幼儿档案 接口
 */
@RestController
@RequestMapping("/api/child")
@RequiredArgsConstructor
public class ChildController {

    private final IChildService childService;

    /** 分页查询（incompleteOnly=true 只看待补全档案） */
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
    @GetMapping("/all")
    public Result<List<Child>> all() {
        return Result.ok(childService.listAll());
    }

    /** 有过敏史的幼儿（膳食模块标注用） */
    @GetMapping("/allergy")
    public Result<List<Child>> allergy() {
        return Result.ok(childService.listAllergy());
    }

    /** 下载 Excel 导入模板 */
    @GetMapping("/import/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("幼儿档案导入模板", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        childService.writeImportTemplate(response.getOutputStream());
    }

    /** Excel 批量导入：逐行校验，成功的入库，失败的返回行号与原因 */
    @PostMapping("/import")
    public Result<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.ok(childService.importChildren(file));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Child> info(@PathVariable Long id) {
        return Result.ok(childService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Child entity) {
        childService.saveChild(entity);
        return Result.ok();
    }

    /** 修改 */
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody Child entity) {
        childService.updateChild(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        childService.removeByIds(ids);
        return Result.ok();
    }
}
