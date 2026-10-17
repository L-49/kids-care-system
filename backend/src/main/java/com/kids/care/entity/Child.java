package com.kids.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 幼儿档案
 */
@Data
@TableName("child")
public class Child {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幼儿姓名 */
    @NotBlank(message = "幼儿姓名不能为空")
    @Size(max = 50, message = "幼儿姓名不能超过 50 个字符")
    private String name;

    /** 性别 */
    @Pattern(regexp = "男|女", message = "性别只能是男或女")
    private String gender;

    /** 出生日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /** 所在班级ID */
    @NotNull(message = "请选择所在班级")
    private Long clazzId;

    /** 家长姓名 */
    @Size(max = 50, message = "家长姓名不能超过 50 个字符")
    private String guardianName;

    /** 家长电话 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "家长电话格式不正确")
    private String guardianPhone;

    /** 家庭住址 */
    @Size(max = 200, message = "家庭住址不能超过 200 个字符")
    private String address;

    /** 过敏史/禁忌 */
    @Size(max = 200, message = "过敏史不能超过 200 个字符")
    private String allergy;

    /** 入园日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate enrollDate;

    /** 状态：1在园 0离园 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;


    /** 班级名称 */
    @TableField(exist = false)
    private String clazzName;
}
