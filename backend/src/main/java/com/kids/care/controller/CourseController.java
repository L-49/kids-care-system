package com.kids.care.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kids.care.common.Result;
import com.kids.care.entity.Course;
import com.kids.care.service.ICourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程活动 接口
 */
@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final ICourseService courseService;

    /** 分页查询 */
    @GetMapping("/page")
    public Result<List<Course>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long clazzId,
                                         @RequestParam(required = false) Long teacherId,
                                         @RequestParam(required = false) String courseDate) {
        IPage<Course> page = courseService.pageQuery(pageNum, pageSize, keyword, clazzId, teacherId, courseDate);
        return Result.page(page.getRecords(), page.getTotal());
    }

    /** 全部列表（下拉框用） */
    @GetMapping("/all")
    public Result<List<Course>> all() {
        return Result.ok(courseService.listAll());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Course> info(@PathVariable Long id) {
        return Result.ok(courseService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@RequestBody Course entity) {
        courseService.save(entity);
        return Result.ok();
    }

    /** 修改 */
    @PutMapping
    public Result<Void> edit(@RequestBody Course entity) {
        courseService.updateById(entity);
        return Result.ok();
    }

    /** 删除（支持批量） */
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        courseService.removeByIds(ids);
        return Result.ok();
    }
}
