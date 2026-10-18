package com.kids.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食材出入库流水（只增不改，不做逻辑删除）
 */
@Data
@TableName("stock_record")
public class StockRecord {

    /** 入库 */
    public static final String TYPE_IN = "IN";

    /** 出库 */
    public static final String TYPE_OUT = "OUT";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 食材ID */
    private Long foodId;

    /** 食材名称（冗余保存，食材改名不影响历史流水） */
    private String foodName;

    /** 类型：IN入库 / OUT出库 */
    private String type;

    /** 本次数量（正数，方向由 type 决定） */
    private BigDecimal quantity;

    /** 操作前库存 */
    private BigDecimal beforeQty;

    /** 操作后库存 */
    private BigDecimal afterQty;

    /** 单位 */
    private String unit;

    /** 操作人 */
    private String operator;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 类型中文名（前端展示用） */
    @TableField(exist = false)
    private String typeText;
}
