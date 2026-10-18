-- ============================================================
-- v11_plan_a.sql
-- 方案 A（选择性移植整合包改进）配套表结构变更
--
--   1. sys_log 增加 error_msg：记录失败请求的真实原因，
--      配合 GlobalExceptionHandler 写入的 request attribute("logError")
--   2. sys_user.password 放宽到 VARCHAR(200)：
--      PBKDF2 摘要格式为 pbkdf2$120000$<24位盐>$<44位摘要>（约 83 字符），
--      原 VARCHAR(64) 会写入失败（Data too long）
--
-- 幂等：可重复执行（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS）
-- 执行：mysql -uroot -p kids_care < v11_plan_a.sql
-- ============================================================

-- 1. sys_log.error_msg
SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `sys_log` ADD COLUMN `error_msg` VARCHAR(300) NULL COMMENT ''失败原因'' AFTER `status`',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'error_msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. sys_user.password 放宽长度
SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `sys_user` MODIFY COLUMN `password` VARCHAR(200) NOT NULL COMMENT ''口令摘要（PBKDF2，兼容历史 MD5）''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'password'
      AND CHARACTER_MAXIMUM_LENGTH >= 200);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 验证
SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'error_msg';
SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'password';
