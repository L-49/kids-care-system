package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.SysLog;

import java.util.List;

/**
 * 操作日志 业务接口
 */
public interface ISysLogService extends IService<SysLog> {

    /** 分页查询 */
    IPage<SysLog> pageQuery(int pageNum, int pageSize, String keyword, String method);

    /** 全部列表 */
    List<SysLog> listAll();

    /** 清空日志 */
    void clean();
}
