package com.kids.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户（登录账号）
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名 */
    private String username;

    /** 密码（MD5 摘要，不返回给前端） */
    @JsonIgnore
    private String password;

    /** 姓名 */
    private String realName;

    /** 角色：admin/teacher/parent */
    private String role;

    /** 家长账号绑定的幼儿ID，非家长角色为 null */
    private Long childId;

    /** 手机号 */
    private String phone;

    /** 状态：1启用 0禁用 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;
}
