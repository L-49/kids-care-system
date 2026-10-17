package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.dto.ImportResult;
import com.kids.care.entity.Child;
import com.kids.care.entity.ChildChangeLog;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
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

    /** Excel 批量导入：逐行校验，成功的入库，失败的返回行号与原因 */
    ImportResult importChildren(MultipartFile file);

    /** 输出导入模板（xlsx） */
    void writeImportTemplate(OutputStream out);

    /** 某幼儿的变更记录（按时间倒序） */
    List<ChildChangeLog> listHistory(Long childId);
}
