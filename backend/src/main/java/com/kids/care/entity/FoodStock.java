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
 * 食材库存
 */
@Data
@TableName("food_stock")
public class FoodStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 食材名称 */
    private String name;

    /** 分类：主食/蔬菜/肉类/水果/其他 */
    private String category;

    /** 库存数量 */
    private BigDecimal quantity;

    /** 单位 */
    private String unit;

    /** 预警线 */
    private BigDecimal warnLine;

    /** 供应商 */
    private String supplier;

    /** 最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;


    /** 是否低于预警线 */
    @TableField(exist = false)
    private Boolean warn;
}
