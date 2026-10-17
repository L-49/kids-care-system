-- ============================================================
-- v4：通知公告增加状态字段
-- 目的：支持「草稿 / 定时待发 / 已发布 / 已撤回」状态机
-- 说明：本脚本可重复执行（先查 information_schema 再决定是否 ALTER）
-- 执行：mysql -u root -p123456 --default-character-set=utf8mb4 < v4_notice_status.sql
-- ============================================================
USE kids_care;

-- 1. 增加 status 列
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'notice'
               AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0,
    'ALTER TABLE notice ADD COLUMN status TINYINT NOT NULL DEFAULT 2 COMMENT ''0草稿 1定时待发 2已发布 3已撤回'' AFTER publisher',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 增加联合索引（列表按状态过滤 + 按发布时间排序，定时任务也走这个索引）
SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'notice'
               AND INDEX_NAME = 'idx_notice_status');
SET @sql := IF(@idx = 0,
    'ALTER TABLE notice ADD INDEX idx_notice_status (status, publish_time)',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 存量数据兜底：历史公告一律视为「已发布」
UPDATE notice SET status = 2 WHERE status IS NULL OR status NOT IN (0, 1, 2, 3);

-- 4. 校验
SELECT status, COUNT(*) AS cnt FROM notice GROUP BY status;
