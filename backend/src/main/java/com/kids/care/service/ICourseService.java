package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.Course;

import java.util.List;

/**
 * 课程活动 业务接口
 */
public interface ICourseService extends IService<Course> {

    /** 分页查询 */
    IPage<Course> pageQuery(int pageNum, int pageSize, String keyword, Long clazzId, Long teacherId, String courseDate);

    /** 全部列表（用于下拉框） */
    List<Course> listAll();
}
