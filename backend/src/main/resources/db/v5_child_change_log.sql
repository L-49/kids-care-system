-- ============================================================
-- v5：幼儿档案变更留痕
-- 目的：记录关键字段的逐次变更（谁、何时、从什么改成什么）
-- 说明：本脚本可重复执行
-- ============================================================
USE kids_care;

CREATE TABLE IF NOT EXISTS child_change_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    child_id    BIGINT       NOT NULL COMMENT '幼儿ID',
    child_name  VARCHAR(50)  COMMENT '留档时的姓名（档案被删也能追溯）',
    change_type VARCHAR(10)  NOT NULL COMMENT '新增/修改/导入',
    field_name  VARCHAR(30)  COMMENT '字段英文名，整条记录时为空',
    field_label VARCHAR(30)  COMMENT '字段中文名',
    old_value   VARCHAR(500) COMMENT '变更前',
    new_value   VARCHAR(500) COMMENT '变更后',
    operator    VARCHAR(50)  COMMENT '操作人',
    batch_id    VARCHAR(40)  COMMENT '批量导入批次号，一次导入共用一个',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ccl_child (child_id, create_time),
    KEY idx_ccl_batch (batch_id)
) COMMENT '幼儿档案变更留痕';

SELECT COUNT(*) AS table_exists FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'child_change_log';
