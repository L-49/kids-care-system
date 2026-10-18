package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.LeaveRequest;

import java.util.List;

/**
 * 请假申请 业务接口
 */
public interface ILeaveRequestService extends IService<LeaveRequest> {

    /** 分页查询（childId 非空时只查该幼儿，用于家长数据隔离） */
    IPage<LeaveRequest> pageQuery(int pageNum, int pageSize, String keyword, Integer status, Long childId);

    /** 全部列表（用于下拉框） */
    List<LeaveRequest> listAll();

    /** 审批：pass=true 通过，false 驳回 */
    boolean approve(Long id, boolean pass, String approver);
}
