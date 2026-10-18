-- ============================================================
-- v8_member1_modules.sql
-- 合并「组员1」四个模块所需的表结构变更
--   食谱管理 / 食材库存 / 班级管理 / 教职工管理
--
-- 原则：
--   1. 只加不删：只做 ADD COLUMN / CREATE TABLE，不 drop、不改现有数据
--   2. 幂等：可重复执行，已存在的列/表自动跳过
--   3. 不碰账号体系：sys_user 保持 MD5 密码，不加 child_id
--
-- 关于写法：
--   MySQL 8.0 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS
--   （那是 MariaDB 语法，组员1 的 v7_merge_clazz_teacher.sql 在 MySQL 上会报
--    ERROR 1064，已实测），因此这里统一用 information_schema 判断后再执行。
--
-- 执行方式：Navicat / mysql 命令行 / IDEA 数据库控制台
--   命令行示例：
--   mysql -h 127.0.0.1 -u root -p kids_care < v8_member1_modules.sql
-- ============================================================

-- ------------------------------------------------------------
-- 1. 班级表 clazz：新增「开班日期」「班级特色」
--    用到的地方：班级管理页的列表列、新增/编辑弹窗
-- ------------------------------------------------------------
SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `clazz` ADD COLUMN `open_date` DATE NULL COMMENT ''开班日期''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'clazz' AND COLUMN_NAME = 'open_date');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `clazz` ADD COLUMN `remark` VARCHAR(200) NULL COMMENT ''班级特色''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'clazz' AND COLUMN_NAME = 'remark');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 2. 教职工表 teacher：新增 6 个人事档案字段
--    用到的地方：教职工管理页的「人事档案」区域
--    注意：id_card 不加唯一约束——历史数据可能为空，且业务上未要求
-- ------------------------------------------------------------
SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `teacher` ADD COLUMN `id_card` VARCHAR(20) NULL COMMENT ''身份证号''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'id_card');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `teacher` ADD COLUMN `education` VARCHAR(50) NULL COMMENT ''学历''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'education');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `teacher` ADD COLUMN `title` VARCHAR(50) NULL COMMENT ''职称''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'title');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `teacher` ADD COLUMN `address` VARCHAR(200) NULL COMMENT ''家庭住址''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'address');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `teacher` ADD COLUMN `emergency_contact` VARCHAR(50) NULL COMMENT ''紧急联系人''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'emergency_contact');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `teacher` ADD COLUMN `emergency_phone` VARCHAR(20) NULL COMMENT ''紧急联系电话''',
        'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'emergency_phone');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 3. 食材出入库流水表 stock_record（新表）
--    设计要点（沿用组员1）：
--      * 只增不改，不做逻辑删除——流水就是账本，不允许事后抹掉
--      * 冗余 food_name：食材改名/删除后历史流水仍可读
--      * before_qty / after_qty 双记：可直接对账，不必回溯计算
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `stock_record` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `food_id`     BIGINT        NOT NULL COMMENT '食材ID',
    `food_name`   VARCHAR(50)   NOT NULL COMMENT '食材名称（冗余，食材改名不影响历史）',
    `type`        VARCHAR(10)   NOT NULL COMMENT 'IN入库 / OUT出库',
    `quantity`    DECIMAL(10,2) NOT NULL COMMENT '本次数量',
    `before_qty`  DECIMAL(10,2) NOT NULL COMMENT '操作前库存',
    `after_qty`   DECIMAL(10,2) NOT NULL COMMENT '操作后库存',
    `unit`        VARCHAR(10)   DEFAULT NULL COMMENT '单位',
    `operator`    VARCHAR(50)   DEFAULT NULL COMMENT '操作人',
    `remark`      VARCHAR(200)  DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_record_food` (`food_id`),
    KEY `idx_record_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '食材出入库流水';

-- ------------------------------------------------------------
-- 4. 刻意“不”做的变更（写在这里避免以后被误加）
--
--   sys_user.child_id      —— 组员1 用它做「家长端·我的孩子」。本次不合并该页面，
--                             账号体系沿用我方的 MD5 + 三角色，故不加。
--   sys_user.password 加长 —— 同上，PBKDF2 摘要不引入，避免现有账号登录失败。
--   sys_log.error_msg      —— 组员1 版 LogInterceptor 记录失败原因用。本次保留我方
--                             LogInterceptor，故不加列；若日后要换，再加此列。
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 5. 自检：每项应输出 1
-- ------------------------------------------------------------
SELECT 'clazz.open_date' AS item,
       (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'clazz' AND COLUMN_NAME = 'open_date') AS ok
UNION ALL
SELECT 'clazz.remark',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'clazz' AND COLUMN_NAME = 'remark')
UNION ALL
SELECT 'teacher.id_card',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'id_card')
UNION ALL
SELECT 'teacher.education',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'education')
UNION ALL
SELECT 'teacher.title',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'title')
UNION ALL
SELECT 'teacher.address',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'address')
UNION ALL
SELECT 'teacher.emergency_contact',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'emergency_contact')
UNION ALL
SELECT 'teacher.emergency_phone',
       (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher' AND COLUMN_NAME = 'emergency_phone')
UNION ALL
SELECT 'table.stock_record',
       (SELECT COUNT(*) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_record');
