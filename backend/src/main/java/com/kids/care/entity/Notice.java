package com.kids.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 通知公告
 */
@Data
@TableName("notice")
public class Notice {

    /** 状态：草稿 */
    public static final int STATUS_DRAFT = 0;
    /** 状态：定时待发（到点由定时任务自动发布） */
    public static final int STATUS_SCHEDULED = 1;
    /** 状态：已发布（唯一对外可见的状态） */
    public static final int STATUS_PUBLISHED = 2;
    /** 状态：已撤回（保留记录，不再对外展示） */
    public static final int STATUS_REVOKED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "标题不能超过 100 个字符")
    private String title;

    /** 内容 */
    @Size(max = 1000, message = "公告内容不能超过 1000 个字符")
    private String content;

    /** 类型：取自数据字典 notice_type（通知/活动/食谱/放假） */
    @NotBlank(message = "请选择公告类型")
    private String type;

    /** 发布人 */
    private String publisher;

    /** 公告状态：0草稿 1定时待发 2已发布 3已撤回 */
    private Integer status;

    /** 发布时间：定时发布时表示计划发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;
}
