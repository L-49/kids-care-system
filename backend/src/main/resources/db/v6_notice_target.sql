-- ============================================================
-- v6：通知公告定向可见（全园 / 指定班级）
-- 说明：本脚本可重复执行；只做两档，教师维度需先补 sys_user 与 teacher 的绑定关系
-- ============================================================
USE kids_care;

-- 1. target_type
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'target_type');
SET @sql := IF(@col = 0,
    'ALTER TABLE notice ADD COLUMN target_type VARCHAR(10) NOT NULL DEFAULT ''all'' COMMENT ''可见范围：all全园 class指定班级'' AFTER status',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. target_id
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'target_id');
SET @sql := IF(@col = 0,
    'ALTER TABLE notice ADD COLUMN target_id BIGINT NULL COMMENT ''指定班级ID（target_type=class 时有效）'' AFTER target_type',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 存量公告一律视为全园可见
UPDATE notice SET target_type = 'all' WHERE target_type IS NULL OR target_type NOT IN ('all', 'class');

-- 4. 校验
SELECT target_type, COUNT(*) AS cnt FROM notice GROUP BY target_type;
