package com.kids.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "教职工姓名不能为空")
    @Size(max = 50, message = "教职工姓名不能超过 50 个字符")
    private String name;

    /** 性别 */
    @Pattern(regexp = "^$|男|女", message = "性别只能是男或女")
    private String gender;

    /** 联系电话 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String phone;

    /** 岗位：园长/教师/保育员/保健医 */
    @Size(max = 20, message = "岗位不能超过 20 个字符")
    private String role;

    /** 入职日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;

    /** 状态：1在职 0离职 */
    private Integer status;

    /** 身份证号 */
    @Pattern(regexp = "^$|^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    /** 学历 */
    @Size(max = 20, message = "学历不能超过 20 个字符")
    private String education;

    /** 职称 */
    @Size(max = 20, message = "职称不能超过 20 个字符")
    private String title;

    /** 家庭住址 */
    @Size(max = 200, message = "家庭住址不能超过 200 个字符")
    private String address;

    /** 紧急联系人 */
    @Size(max = 50, message = "紧急联系人不能超过 50 个字符")
    private String emergencyContact;

    /** 紧急联系电话 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "紧急联系电话格式不正确")
    private String emergencyPhone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;
}
