package com.kids.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "班级名称不能为空")
    @Size(max = 50, message = "班级名称不能超过 50 个字符")
    private String name;

    /** 适龄段，如 3-4岁 */
    @Size(max = 20, message = "适龄段不能超过 20 个字符")
    private String ageRange;

    /** 教室 */
    @Size(max = 50, message = "教室不能超过 50 个字符")
    private String room;

    /** 额定人数 */
    @NotNull(message = "请填写额定人数")
    @Min(value = 1, message = "额定人数必须大于 0")
    @Max(value = 200, message = "额定人数不能超过 200")
    private Integer capacity;

    /** 班主任ID */
    private Long headTeacherId;

    /** 开班日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate openDate;

    /** 班级特色 */
    @Size(max = 200, message = "班级特色不能超过 200 个字符")
    private String remark;

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

    /** 满员状态：0正常 1满员 2超载（服务层派生，非数据库字段） */
    @TableField(exist = false)
    private Integer fullStatus;
}
