package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.Attendance;

import java.util.List;

/**
 * 考勤记录 业务接口
 */
public interface IAttendanceService extends IService<Attendance> {

    /** 分页查询（childId 非空时只查该幼儿，用于家长数据隔离） */
    IPage<Attendance> pageQuery(int pageNum, int pageSize, String keyword, Long clazzId,
                                String status, String attDate, Long childId);

    /** 全部列表（用于下拉框） */
    List<Attendance> listAll();
}
