-- =============================================================================
-- v9 家长端 + 角色权限 迁移脚本
-- 日期：2026-10-18
-- 说明：合并组员1「家长端-我的孩子」「角色权限」功能所需的最小结构变更，
--       另补齐上周工作日的考勤演示数据（此前种子数据只有 10-17/10-18 两天）。
--       幂等：可重复执行。
-- =============================================================================

-- 1. sys_user 增加 child_id（家长账号绑定幼儿；其他角色为 NULL） -------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'child_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN child_id BIGINT NULL COMMENT ''家长账号绑定的幼儿ID，非家长角色为NULL'' AFTER role',
    'SELECT ''sys_user.child_id 已存在，跳过'' AS info');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 家长账号(parent)绑定幼儿1（张小明）--------------------------------------
UPDATE sys_user SET child_id = 1 WHERE username = 'parent' AND (child_id IS NULL OR child_id = 0);

-- 3. 补齐近一周工作日（2026-10-12 ~ 10-16）考勤演示数据 ----------------------
--    以 10-17 已有记录为模板，同幼儿同状态复制到各工作日；
--    NOT EXISTS 保证幂等（已有该日记录的幼儿不重复生成）。
INSERT INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark)
SELECT a.child_id, a.clazz_id, d.att_date, a.status, a.check_in_time, '演示数据：由 10-17 记录补齐'
FROM attendance a
CROSS JOIN (
    SELECT DATE '2026-10-12' AS att_date
    UNION ALL SELECT DATE '2026-10-13'
    UNION ALL SELECT DATE '2026-10-14'
    UNION ALL SELECT DATE '2026-10-15'
    UNION ALL SELECT DATE '2026-10-16'
) d
WHERE a.att_date = '2026-10-17' AND a.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM attendance x
      WHERE x.child_id = a.child_id AND x.att_date = d.att_date AND x.deleted = 0);

-- 4. 自检：应为 1 -------------------------------------------------------------
SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'child_id') = 1,
          1, 0)                                          AS 'sys_user.child_id 已添加';
SELECT IF((SELECT child_id FROM sys_user WHERE username = 'parent') = 1,
          1, 0)                                          AS 'parent 已绑定幼儿1';
SELECT IF((SELECT COUNT(DISTINCT att_date) FROM attendance WHERE deleted = 0
           AND att_date BETWEEN '2026-10-12' AND '2026-10-16') = 5,
          1, 0)                                          AS '上周工作日考勤已补齐';
