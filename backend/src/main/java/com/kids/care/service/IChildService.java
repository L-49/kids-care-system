package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.Child;

import java.util.List;

/**
 * 幼儿档案 业务接口
 */
public interface IChildService extends IService<Child> {

    /** 分页查询（incompleteOnly=true 时只返回完整度未满 100 的档案） */
    IPage<Child> pageQuery(int pageNum, int pageSize, String keyword, Long clazzId,
                           Integer status, Boolean incompleteOnly);

    /** 全部列表（用于下拉框） */
    List<Child> listAll();

    /** 有过敏史的幼儿（膳食模块标注用） */
    List<Child> listAllergy();

    /** 新增档案（校验班级存在） */
    void saveChild(Child entity);

    /** 修改档案（校验班级存在） */
    void updateChild(Child entity);
}
