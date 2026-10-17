package com.kids.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kids.care.entity.SysLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {

    /** 清空日志（物理删除） */
    @Delete("truncate table sys_log")
    void truncate();
}
