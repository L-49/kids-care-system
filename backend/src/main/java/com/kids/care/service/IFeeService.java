package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.Fee;

import java.util.List;

/**
 * 收费记录 业务接口
 */
public interface IFeeService extends IService<Fee> {

    /** 分页查询 */
    IPage<Fee> pageQuery(int pageNum, int pageSize, String keyword, String feeMonth, Integer payStatus);

    /** 全部列表（用于下拉框） */
    List<Fee> listAll();

    /** 缴费登记 */
    boolean pay(Long id);
}
