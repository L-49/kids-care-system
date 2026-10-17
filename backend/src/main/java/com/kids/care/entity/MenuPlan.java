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
 * 每周食谱
 */
@Data
@TableName("menu_plan")
public class MenuPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate menuDate;

    /** 餐次：早餐/午餐/午点 */
    private String mealType;

    /** 菜品 */
    private String dishes;

    /** 备注/营养说明 */
    private String note;

    /** 发布人 */
    private String publisher;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;
}
