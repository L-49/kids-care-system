package com.kids.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教职工
 */
@Data
@TableName("teacher")
public class Teacher {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 姓名 */
    private String name;

    /** 性别 */
    private String gender;

    /** 联系电话 */
    private String phone;

    /** 岗位：园长/教师/保育员/保健医 */
    private String role;

    /** 入职日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;

    /** 状态：1在职 0离职 */
    private Integer status;

    /** 身份证号 */
    private String idCard;

    /** 学历 */
    private String education;

    /** 职称 */
    private String title;

    /** 家庭住址 */
    private String address;

    /** 紧急联系人 */
    private String emergencyContact;

    /** 紧急联系电话 */
    private String emergencyPhone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;
}
