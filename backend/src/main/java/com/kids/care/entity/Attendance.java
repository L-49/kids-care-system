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
 * 考勤记录
 */
@Data
@TableName("attendance")
public class Attendance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幼儿ID */
    private Long childId;

    /** 班级ID（冗余字段，便于按班统计） */
    private Long clazzId;

    /** 考勤日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate attDate;

    /** 出勤状态：出勤/迟到/请假/缺勤 */
    private String status;

    /** 入园时间 HH:mm */
    private String checkInTime;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;


    /** 幼儿姓名 */
    @TableField(exist = false)
    private String childName;

    /** 班级名称 */
    @TableField(exist = false)
    private String clazzName;
}
