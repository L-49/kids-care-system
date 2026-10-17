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
 * 班级
 */
@Data
@TableName("clazz")
public class Clazz {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级名称 */
    private String name;

    /** 适龄段，如 3-4岁 */
    private String ageRange;

    /** 教室 */
    private String room;

    /** 额定人数 */
    private Integer capacity;

    /** 班主任ID */
    private Long headTeacherId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;


    /** 班主任姓名 */
    @TableField(exist = false)
    private String headTeacherName;

    /** 在园幼儿数 */
    @TableField(exist = false)
    private Long childCount;
}
