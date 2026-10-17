package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.Teacher;

import java.util.List;

/**
 * 教职工 业务接口
 */
public interface ITeacherService extends IService<Teacher> {

    /** 分页查询 */
    IPage<Teacher> pageQuery(int pageNum, int pageSize, String keyword, String role, Integer status);

    /** 全部列表（用于下拉框） */
    List<Teacher> listAll();
}
