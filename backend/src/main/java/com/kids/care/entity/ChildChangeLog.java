package com.kids.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 幼儿档案变更留痕。
 *
 * <p>只记录 8 个关键字段的字段级差异（姓名、性别、出生日期、班级、家长姓名、
 * 家长电话、家庭住址、过敏史），不记录 create_time / deleted 这类系统字段，
 * 否则日志表噪音过大、反而看不出业务变化。
 *
 * <p>为什么不用现成的 sys_log：那张表的 params 是 VARCHAR(500) 的请求参数快照，
 * 新旧值混在一起，既不能按字段检索也塞不下多次修改。它是"请求日志"，不是"数据审计"。
 */
@Data
@TableName("child_change_log")
public class ChildChangeLog {

    /** 变更类型：新增 */
    public static final String TYPE_CREATE = "新增";
    /** 变更类型：修改 */
    public static final String TYPE_UPDATE = "修改";
    /** 变更类型：批量导入 */
    public static final String TYPE_IMPORT = "导入";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long childId;

    /** 留档时的姓名，档案被逻辑删除后仍能看懂这条记录是谁的 */
    private String childName;

    private String changeType;

    /** 字段英文名；整条记录（新增/导入）时为空 */
    private String fieldName;

    private String fieldLabel;

    private String oldValue;

    private String newValue;

    private String operator;

    /** 批量导入批次号，一次导入的所有记录共用 */
    private String batchId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
