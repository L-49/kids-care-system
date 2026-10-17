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
 * 收费记录
 */
@Data
@TableName("fee")
public class Fee {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幼儿ID */
    private Long childId;

    /** 费用类型：保教费/餐费/校车费/其他 */
    private String feeType;

    /** 金额 */
    private BigDecimal amount;

    /** 所属月份 yyyy-MM */
    private String feeMonth;

    /** 缴费状态：0未缴 1已缴 */
    private Integer payStatus;

    /** 缴费时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

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
}
