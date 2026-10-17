package com.kids.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("sys_log")
public class SysLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 */
    private String username;

    /** 操作描述，如：新增幼儿档案 */
    private String operation;

    /** 请求方式 */
    private String method;

    /** 请求地址 */
    private String uri;

    /** 请求参数（查询串） */
    private String params;

    /** 操作IP */
    private String ip;

    /** 耗时（毫秒） */
    private Long costTime;

    /** HTTP 状态码 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
