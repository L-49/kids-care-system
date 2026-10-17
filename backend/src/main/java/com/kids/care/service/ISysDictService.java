package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.SysDict;

import java.util.List;

/**
 * 数据字典 业务接口
 */
public interface ISysDictService extends IService<SysDict> {

    /** 分页查询 */
    IPage<SysDict> pageQuery(int pageNum, int pageSize, String keyword, String dictType);

    /** 全部列表 */
    List<SysDict> listAll();

    /** 按字典类型取启用项（下拉框用，按 sort 升序） */
    List<SysDict> listByType(String dictType);
}
