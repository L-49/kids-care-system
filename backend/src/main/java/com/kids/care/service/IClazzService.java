package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.Clazz;

import java.util.List;

/**
 * 班级 业务接口
 */
public interface IClazzService extends IService<Clazz> {

    /** 分页查询 */
    IPage<Clazz> pageQuery(int pageNum, int pageSize, String keyword);

    /** 全部列表（用于下拉框） */
    List<Clazz> listAll();
}
