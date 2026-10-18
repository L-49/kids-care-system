-- ============================================================
-- v10 同步组员1（成员1）数据库内容到本项目 kids_care
-- ------------------------------------------------------------
-- 来源脚本：schema.sql / v3_demo_data / v4_today_data / v5_more_people
--           / v6_weekly_menu / v7_clazz_teacher_upgrade
-- 原则：
--   1) 只补「本库没有的」数据，已有数据一律不覆盖；
--   2) 全部语句可重复执行（NOT EXISTS / INSERT IGNORE）；
--   3) 不改任何表结构 —— 本库结构已是成员1 schema 的超集
--      （teacher 多 address；notice 多 status/target_type/target_id；
--        另有 child_change_log 表；成员1 的 sys_log.error_msg 本项目实体未使用，故不加）。
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 kids_care < v10_sync_member1_data.sql
-- ============================================================
USE kids_care;

-- ============================================================
-- 0. 历史重复清理：基线 schema 曾被重复执行，fee 出现完全相同的两套账单
--    （仅删除【所有字段都相同】的重复行，保留 id 最小的一条）
-- ============================================================
DELETE f1 FROM fee f1
JOIN fee f2
  ON f1.child_id  = f2.child_id
 AND f1.fee_type  = f2.fee_type
 AND f1.fee_month = f2.fee_month
 AND f1.amount    = f2.amount
 AND f1.pay_status = f2.pay_status
 AND f1.deleted   = f2.deleted
WHERE f1.id > f2.id;

-- ============================================================
-- 1. 教职工 +7 人（v5 ①）
-- ============================================================
DROP TEMPORARY TABLE IF EXISTS tmp_teacher_v10;
CREATE TEMPORARY TABLE tmp_teacher_v10 (
  name VARCHAR(50), gender VARCHAR(4), phone VARCHAR(20),
  role VARCHAR(20), entry_date DATE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO tmp_teacher_v10 VALUES
('孙晓梅', '女', '13900000007', '副园长', '2017-09-01'),
('周慧',   '女', '13900000008', '教师',   '2022-09-01'),
('吴强',   '男', '13900000009', '教师',   '2024-03-01'),
('郑春燕', '女', '13900000010', '保育员', '2021-09-01'),
('何丽',   '女', '13900000011', '保育员', '2023-03-01'),
('马涛',   '男', '13900000012', '厨师',   '2020-09-01'),
('段国栋', '男', '13900000013', '保安',   '2024-09-01');

INSERT INTO teacher (name, gender, phone, role, entry_date, status)
SELECT t.name, t.gender, t.phone, t.role, t.entry_date, 1
FROM tmp_teacher_v10 t
WHERE NOT EXISTS (
  SELECT 1 FROM teacher x WHERE x.deleted = 0 AND x.name = t.name AND x.phone = t.phone
);

-- ============================================================
-- 2. 幼儿 +23 人（v5 ②：小班 5 / 中班 7 / 大班 7 / 托班 4）
-- ============================================================
DROP TEMPORARY TABLE IF EXISTS tmp_child_v10;
CREATE TEMPORARY TABLE tmp_child_v10 (
  name VARCHAR(50), gender VARCHAR(4), birthday DATE, clazz_id BIGINT,
  guardian_name VARCHAR(50), guardian_phone VARCHAR(20), address VARCHAR(200),
  allergy VARCHAR(200), enroll_date DATE
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO tmp_child_v10 VALUES
-- 小班（3-4 岁）
('何思远', '男', '2022-11-08', 1, '何志强', '13800001001', '阳光路 88 号 3 栋 502', NULL,       '2024-09-01'),
('许安然', '女', '2023-02-14', 1, '许文慧', '13800001002', '和平街 12 号 1 栋 301', '花生过敏', '2024-09-01'),
('林知夏', '女', '2022-12-03', 1, '林海涛', '13800001003', '育才路 45 号 6 栋 1102', NULL,      '2024-09-01'),
('高梓睿', '男', '2023-01-19', 1, '高建国', '13800001004', '阳光路 102 号 2 栋 802', NULL,      '2024-09-01'),
('蒋可欣', '女', '2022-10-27', 1, '蒋丽娟', '13800001005', '建设北路 7 号 4 栋 403', '鸡蛋过敏', '2024-09-01'),
-- 中班（4-5 岁）
('谢雨辰', '男', '2021-09-12', 2, '谢明辉', '13800001006', '文化路 25 号 2 栋 601', NULL,       '2023-09-01'),
('沈嘉树', '男', '2022-03-05', 2, '沈志远', '13800001007', '育才路 68 号 5 栋 202', '海鲜过敏', '2023-09-01'),
('韩语桐', '女', '2021-11-23', 2, '韩雪梅', '13800001008', '和平街 33 号 3 栋 901', NULL,      '2023-09-01'),
('罗一鸣', '男', '2022-01-08', 2, '罗建华', '13800001009', '幸福里 9 号 1 栋 504', NULL,       '2023-09-01'),
('唐可儿', '女', '2021-10-15', 2, '唐文娟', '13800001010', '阳光路 76 号 8 栋 1003', NULL,     '2023-09-01'),
('曹睿泽', '男', '2022-02-20', 2, '曹永刚', '13800001011', '建设北路 44 号 2 栋 706', NULL,    '2023-09-01'),
('邓沐阳', '男', '2021-12-30', 2, '邓丽萍', '13800001012', '文化路 101 号 7 栋 305', NULL,    '2023-09-01'),
-- 大班（5-6 岁）
('苏诗涵', '女', '2020-10-09', 3, '苏振华', '13800001013', '阳光路 55 号 4 栋 601', NULL,      '2022-09-01'),
('卢子墨', '男', '2021-01-17', 3, '卢伟东', '13800001014', '和平街 66 号 5 栋 402', '牛奶过敏', '2022-09-01'),
('白若溪', '女', '2020-12-25', 3, '白立强', '13800001015', '育才路 12 号 2 栋 903', NULL,      '2022-09-01'),
('潘宇轩', '男', '2021-03-11', 3, '潘俊杰', '13800001016', '幸福里 21 号 6 栋 201', NULL,      '2022-09-01'),
('石雨萌', '女', '2021-02-06', 3, '石美华', '13800001017', '文化路 88 号 3 栋 502', NULL,      '2022-09-01'),
('崔浩然', '男', '2020-11-19', 3, '崔志刚', '13800001018', '建设北路 15 号 1 栋 803', '花粉过敏', '2022-09-01'),
('汪静怡', '女', '2021-04-02', 3, '汪秀兰', '13800001019', '阳光路 33 号 7 栋 1005', NULL,     '2022-09-01'),
-- 托班（2-3 岁）
('徐小满', '女', '2023-08-16', 4, '徐国庆', '13800001020', '和平街 5 号 2 栋 401', NULL,       '2025-09-01'),
('万嘉禾', '男', '2023-09-28', 4, '万丽君', '13800001021', '育才路 90 号 4 栋 302', NULL,      '2025-09-01'),
('顾清越', '女', '2024-01-05', 4, '顾明远', '13800001022', '幸福里 16 号 1 栋 1101', '坚果过敏', '2025-09-01'),
('严念慈', '女', '2024-02-11', 4, '严文斌', '13800001023', '文化路 40 号 5 栋 604', NULL,      '2025-09-01');

INSERT INTO child (name, gender, birthday, clazz_id, guardian_name, guardian_phone,
                   address, allergy, enroll_date, status)
SELECT t.name, t.gender, t.birthday, t.clazz_id, t.guardian_name, t.guardian_phone,
       t.address, t.allergy, t.enroll_date, 1
FROM tmp_child_v10 t
WHERE NOT EXISTS (
  SELECT 1 FROM child c WHERE c.deleted = 0 AND c.name = t.name AND c.guardian_phone = t.guardian_phone
);

-- ============================================================
-- 3. 教职工人事字段补值（v7 数据部分，覆盖原有 6 人 + 本次新增 7 人）
-- ============================================================
-- 园长
UPDATE teacher SET title = '高级教师', education = '本科',
       id_card = '110101198503120012', emergency_contact = '李建国', emergency_phone = '13800001001'
WHERE role = '园长' AND (title IS NULL OR title = '');

-- 女教师
UPDATE teacher SET title = '一级教师', education = '本科',
       id_card = CONCAT('110101199209015', LPAD(id, 3, '0')),
       emergency_contact = '王立', emergency_phone = CONCAT('138000010', LPAD(id + 1, 2, '0'))
WHERE role = '教师' AND gender = '女' AND (title IS NULL OR title = '');

-- 男教师
UPDATE teacher SET title = '二级教师', education = '本科',
       id_card = CONCAT('110101199111030', LPAD(id, 3, '0')),
       emergency_contact = '陈红', emergency_phone = CONCAT('138000010', LPAD(id + 2, 2, '0'))
WHERE role = '教师' AND gender = '男' AND (title IS NULL OR title = '');

-- 保育员
UPDATE teacher SET title = '二级保育员', education = '中专',
       id_card = CONCAT('110101199408220', LPAD(id, 3, '0')),
       emergency_contact = '刘强', emergency_phone = CONCAT('138000010', LPAD(id + 3, 2, '0'))
WHERE role = '保育员' AND (title IS NULL OR title = '');

-- 保健医
UPDATE teacher SET title = '保健医师', education = '大专',
       id_card = CONCAT('110101198807170', LPAD(id, 3, '0')),
       emergency_contact = '赵刚', emergency_phone = CONCAT('138000010', LPAD(id + 5, 2, '0'))
WHERE role = '保健医' AND (title IS NULL OR title = '');

-- 厨师
UPDATE teacher SET title = '一级厨师', education = '中专',
       id_card = CONCAT('110101199005110', LPAD(id, 3, '0')),
       emergency_contact = '周平', emergency_phone = CONCAT('138000010', LPAD(id + 6, 2, '0'))
WHERE role = '厨师' AND (title IS NULL OR title = '');

-- 副园长 / 保安：职称按岗位语义
UPDATE teacher SET title = '高级教师', education = '本科' WHERE role = '副园长' AND (title IS NULL OR title = '');
UPDATE teacher SET title = '中级保安员', education = '高中' WHERE role = '保安'   AND (title IS NULL OR title = '');

-- 副园长 / 保安 / 厨师 的身份证与紧急联系人（v7 未覆盖，补齐让页面字段完整）
UPDATE teacher SET id_card = CONCAT('11010119821010', LPAD(id, 4, '0')),
                   emergency_contact = '孙立平', emergency_phone = CONCAT('138000021', LPAD(id, 2, '0'))
WHERE role = '副园长' AND (id_card IS NULL OR id_card = '');
UPDATE teacher SET id_card = CONCAT('11010119930715', LPAD(id, 4, '0')),
                   emergency_contact = '段丽', emergency_phone = CONCAT('138000022', LPAD(id, 2, '0'))
WHERE role = '保安' AND (id_card IS NULL OR id_card = '');
UPDATE teacher SET id_card = CONCAT('11010119880520', LPAD(id, 4, '0')),
                   emergency_contact = '马晓东', emergency_phone = CONCAT('138000023', LPAD(id, 2, '0'))
WHERE role = '厨师' AND (id_card IS NULL OR id_card = '');

-- 兜底：任何仍缺职称的教职工
UPDATE teacher SET title = '二级教师', education = IFNULL(education, '大专'),
       id_card = IFNULL(id_card, CONCAT('110101199201010', LPAD(id, 3, '0'))),
       emergency_contact = IFNULL(emergency_contact, '紧急联系人'),
       emergency_phone = IFNULL(emergency_phone, CONCAT('138000019', LPAD(id, 2, '0')))
WHERE title IS NULL OR title = '';

-- 紧急电话统一按编号生成，避免重复
UPDATE teacher SET emergency_phone = CONCAT('138000020', LPAD(id, 2, '0')) WHERE id >= 2;

-- ============================================================
-- 4. 班级特色与开班日期（v7 数据部分）
-- ============================================================
UPDATE clazz SET open_date = '2024-09-01', remark = '新生适应为主，注重生活自理能力培养' WHERE id = 1 AND open_date IS NULL;
UPDATE clazz SET open_date = '2023-09-01', remark = '强化规则意识与同伴交往'             WHERE id = 2 AND open_date IS NULL;
UPDATE clazz SET open_date = '2022-09-01', remark = '幼小衔接为重点，培养学习习惯'       WHERE id = 3 AND open_date IS NULL;
UPDATE clazz SET open_date = '2025-03-01', remark = '低龄照护，师生比 1:1.5'            WHERE id = 4 AND open_date IS NULL;

-- ============================================================
-- 5. 考勤
-- ------------------------------------------------------------
-- 5.1 先移除本项目上一轮为补数据生成的模板考勤（10-12~10-16 各 8 条，
--     create_time 均为 2026-10-18 14:32:15），改由成员1 的原始演示数据覆盖，
--     让状态分布（迟到/请假/缺勤）更真实。10-17、10-18 的原始数据不动。
-- ============================================================
DELETE FROM attendance WHERE create_time = '2026-10-18 14:32:15';

-- 5.2 v3：原有 12 名幼儿近 6 天考勤（唯一键 uk_att 保证幂等）
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (1, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '迟到', '08:17', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (2, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:27', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (3, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '迟到', '08:17', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (4, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:22', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (5, 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:14', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (6, 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:05', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (7, 4, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:09', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (8, 4, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:42', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (9, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:00', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (10, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:37', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (11, 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:16', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (12, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '出勤', '08:00', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (1, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '出勤', '08:26', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (2, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '迟到', '08:23', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (3, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '请假', '', '家长请假');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (4, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '请假', '', '家长请假');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (5, 3, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '出勤', '08:08', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (6, 3, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '迟到', '08:39', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (7, 4, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '出勤', '08:29', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (8, 4, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '迟到', '08:25', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (9, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '出勤', '08:25', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (10, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '出勤', '08:06', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (11, 3, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '出勤', '08:40', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (12, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '出勤', '08:03', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (1, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:04', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (2, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:28', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (3, 2, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:07', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (4, 2, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:38', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (5, 3, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:06', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (6, 3, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:36', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (7, 4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:34', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (8, 4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:23', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (9, 2, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '请假', '', '家长请假');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (10, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:04', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (11, 3, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:39', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (12, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '出勤', '08:09', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (1, 1, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:22', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (2, 1, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '请假', '', '家长请假');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (3, 2, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:30', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (4, 2, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:07', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (5, 3, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:29', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (6, 3, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:30', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (7, 4, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:05', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (8, 4, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:06', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (9, 2, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:47', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (10, 1, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:30', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (11, 3, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:33', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (12, 1, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '出勤', '08:13', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (1, 1, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '迟到', '08:23', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (2, 1, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '出勤', '08:44', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (3, 2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '迟到', '08:01', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (4, 2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '迟到', '08:19', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (5, 3, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '出勤', '08:44', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (6, 3, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '出勤', '08:33', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (7, 4, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '出勤', '08:10', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (8, 4, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '出勤', '08:49', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (9, 2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '出勤', '08:34', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (10, 1, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '迟到', '08:49', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (11, 3, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '迟到', '08:21', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (12, 1, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '出勤', '08:39', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (1, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:15', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (2, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:47', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (3, 2, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:12', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (4, 2, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '迟到', '08:31', '路上拥堵');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (5, 3, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:46', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (6, 3, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:01', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (7, 4, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:30', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (8, 4, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:12', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (9, 2, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '请假', '', '家长请假');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (10, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:28', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (11, 3, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:23', '');
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES (12, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '出勤', '08:14', '');

-- 5.3 v5：新增 23 名幼儿最近 5 个工作日的考勤
INSERT IGNORE INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark, create_time)
SELECT
  c.id,
  c.clazz_id,
  d.dt,
  CASE MOD(c.id * 7 + DAYOFYEAR(d.dt) * 3, 40)
       WHEN 0 THEN '迟到'
       WHEN 1 THEN '请假'
       WHEN 2 THEN '缺勤'
       ELSE '出勤'
  END,
  CASE MOD(c.id * 7 + DAYOFYEAR(d.dt) * 3, 40)
       WHEN 0 THEN CONCAT('08:', 35 + MOD(c.id, 10))
       WHEN 1 THEN NULL
       WHEN 2 THEN NULL
       ELSE CONCAT('08:', LPAD(MOD(c.id * 3 + DAYOFYEAR(d.dt), 20), 2, '0'))
  END,
  CASE MOD(c.id * 7 + DAYOFYEAR(d.dt) * 3, 40)
       WHEN 0 THEN '入园超时'
       WHEN 1 THEN '家长已提前报备'
       ELSE ''
  END,
  d.dt + INTERVAL 8 HOUR
FROM child c
JOIN tmp_child_v10 t ON t.name = c.name
CROSS JOIN (
  SELECT dt FROM (
    SELECT CURDATE() - INTERVAL n DAY AS dt
    FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
          UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) a
  ) b
  WHERE DAYOFWEEK(dt) BETWEEN 2 AND 6
  ORDER BY dt DESC
  LIMIT 5
) d
WHERE c.deleted = 0;

-- ============================================================
-- 6. 收费
-- ------------------------------------------------------------
-- 6.1 v3：原有 5 名幼儿近 3 个月账单（本月已有，NOT EXISTS 自动跳过）
-- ============================================================
INSERT INTO fee (child_id, fee_type, amount, fee_month, pay_status, pay_time, remark)
SELECT x.child_id, x.fee_type, x.amount, x.fee_month, x.pay_status,
       IF(x.pay_status = 1, NOW(), NULL), IF(x.pay_status = 1, '', '待缴')
FROM (
    -- 上月：4 号幼儿未缴
    SELECT c.id AS child_id, t.fee_type, t.amount,
           DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m') AS fee_month,
           CASE WHEN c.id = 4 THEN 0 ELSE 1 END AS pay_status
    FROM child c CROSS JOIN (SELECT '保教费' AS fee_type, 1800.00 AS amount
                             UNION ALL SELECT '餐费', 600.00) t
    WHERE c.id BETWEEN 1 AND 5
    UNION ALL
    -- 前 2 月：3 号幼儿未缴
    SELECT c.id, t.fee_type, t.amount,
           DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 2 MONTH), '%Y-%m'),
           CASE WHEN c.id = 3 THEN 0 ELSE 1 END
    FROM child c CROSS JOIN (SELECT '保教费' AS fee_type, 1800.00 AS amount
                             UNION ALL SELECT '餐费', 600.00) t
    WHERE c.id BETWEEN 1 AND 5
    UNION ALL
    -- 前 3 月：2 号幼儿未缴
    SELECT c.id, t.fee_type, t.amount,
           DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 3 MONTH), '%Y-%m'),
           CASE WHEN c.id = 2 THEN 0 ELSE 1 END
    FROM child c CROSS JOIN (SELECT '保教费' AS fee_type, 1800.00 AS amount
                             UNION ALL SELECT '餐费', 600.00) t
    WHERE c.id BETWEEN 1 AND 5
) x
WHERE NOT EXISTS (
  SELECT 1 FROM fee f WHERE f.deleted = 0
    AND f.child_id = x.child_id AND f.fee_type = x.fee_type AND f.fee_month = x.fee_month
);

-- 6.2 v5：新增幼儿本月（70% 已缴）+ 上月（全部已缴）
INSERT INTO fee (child_id, fee_type, amount, fee_month, pay_status, pay_time, remark)
SELECT c.id, f.fee_type, f.amount,
       DATE_FORMAT(CURDATE(), '%Y-%m'),
       CASE WHEN MOD(c.id * 13, 10) < 7 THEN 1 ELSE 0 END,
       CASE WHEN MOD(c.id * 13, 10) < 7
            THEN CURDATE() - INTERVAL MOD(c.id, 12) DAY + INTERVAL 9 HOUR
            ELSE NULL END,
       IF(MOD(c.id * 13, 10) < 7, '', '待缴')
FROM child c
JOIN tmp_child_v10 t ON t.name = c.name
CROSS JOIN (SELECT '保教费' AS fee_type, 1800.00 AS amount
            UNION ALL SELECT '餐费', 600.00) f
WHERE c.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM fee x
    WHERE x.deleted = 0 AND x.child_id = c.id
      AND x.fee_type = f.fee_type AND x.fee_month = DATE_FORMAT(CURDATE(), '%Y-%m')
  );

INSERT INTO fee (child_id, fee_type, amount, fee_month, pay_status, pay_time, remark)
SELECT c.id, f.fee_type, f.amount,
       DATE_FORMAT(CURDATE() - INTERVAL 1 MONTH, '%Y-%m'),
       1,
       (CURDATE() - INTERVAL 1 MONTH) + INTERVAL (MOD(c.id, 10) + 3) DAY + INTERVAL 10 HOUR,
       ''
FROM child c
JOIN tmp_child_v10 t ON t.name = c.name
CROSS JOIN (SELECT '保教费' AS fee_type, 1800.00 AS amount
            UNION ALL SELECT '餐费', 600.00) f
WHERE c.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM fee x
    WHERE x.deleted = 0 AND x.child_id = c.id
      AND x.fee_type = f.fee_type AND x.fee_month = DATE_FORMAT(CURDATE() - INTERVAL 1 MONTH, '%Y-%m')
  );

-- ============================================================
-- 7. 下周一各班课程（v5 ⑤）
-- ============================================================
SET @next_mon_v10 = CURDATE() + INTERVAL ((9 - DAYOFWEEK(CURDATE())) % 7) DAY;

INSERT INTO course (title, clazz_id, teacher_id, course_date, start_time, end_time, content, status)
SELECT x.title, x.clazz_id, x.teacher_id, @next_mon_v10, x.start_time, x.end_time, x.content, 1
FROM (
  SELECT '快乐阅读'   AS title, 1 AS clazz_id, 2 AS teacher_id, '09:00' AS start_time, '09:40' AS end_time, '绘本《月亮的味道》共读与角色扮演' AS content
  UNION ALL SELECT '户外体能', 1, 5, '10:10', '10:50', '钻爬与平衡组合练习'
  UNION ALL SELECT '手工黏土', 2, 3, '09:00', '09:40', '捏制秋天的果实，锻炼精细动作'
  UNION ALL SELECT '科学小实验', 3, 5, '09:00', '09:40', '沉与浮：哪些物体会浮起来'
  UNION ALL SELECT '音乐律动', 4, 4, '09:30', '10:00', '跟着节奏走线，培养乐感'
) x
WHERE NOT EXISTS (
  SELECT 1 FROM course c
  WHERE c.deleted = 0 AND c.course_date = @next_mon_v10
    AND c.clazz_id = x.clazz_id AND c.title = x.title
);

-- ============================================================
-- 8. 今日请假申请（v4 ②，待审批）
-- ============================================================
INSERT INTO leave_request (child_id, start_date, end_date, days, reason, status, create_time)
SELECT 9, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 4 DAY, 3,
       '随家长回老家探亲，需请假三天，已与班主任沟通', 0,
       CURDATE() + INTERVAL 9 HOUR + INTERVAL 5 MINUTE
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM leave_request l
    WHERE l.child_id = 9 AND l.deleted = 0
      AND l.reason = '随家长回老家探亲，需请假三天，已与班主任沟通'
);

-- ============================================================
-- 9. 通知公告（v4 ④ + v5 ⑥）
-- ============================================================
INSERT INTO notice (title, content, type, publisher, publish_time, create_time)
SELECT '下周一升旗仪式暨秋季运动会动员',
       '各位老师、家长：下周一（10 月 19 日）上午 8:30 在操场举行升旗仪式，同时启动秋季运动会报名。请各班老师组织幼儿准时到场，家长可通过班级群了解运动会项目与报名方式。',
       '园务', '王敏',
       CURDATE() + INTERVAL 10 HOUR + INTERVAL 30 MINUTE,
       CURDATE() + INTERVAL 10 HOUR + INTERVAL 30 MINUTE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notice n WHERE n.deleted = 0 AND n.title = '下周一升旗仪式暨秋季运动会动员');

INSERT INTO notice (title, content, type, publisher, publish_time, create_time)
SELECT '小班秋游活动通知', '小班定于下周三上午前往城市公园秋游，请家长为孩子准备轻便外套、水杯与少量零食，8:20 前到园集合。', '班级', '王敏',
       CURDATE() + INTERVAL 11 HOUR + INTERVAL 20 MINUTE, CURDATE() + INTERVAL 11 HOUR + INTERVAL 20 MINUTE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notice n WHERE n.deleted = 0 AND n.title = '小班秋游活动通知');

INSERT INTO notice (title, content, type, publisher, publish_time, create_time)
SELECT '流感疫苗接种提醒', '近期气温变化较大，建议家长带孩子到社区卫生服务中心接种流感疫苗。园内每日已加强晨检与通风消毒。', '健康', '赵雪',
       CURDATE() + INTERVAL 12 HOUR + INTERVAL 10 MINUTE, CURDATE() + INTERVAL 12 HOUR + INTERVAL 10 MINUTE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notice n WHERE n.deleted = 0 AND n.title = '流感疫苗接种提醒');

-- ============================================================
-- 10. 本周（周一至周五）食谱（v6，uk_menu 保证幂等）
-- ============================================================
SET @mon_v10 = CURDATE() - INTERVAL ((DAYOFWEEK(CURDATE()) + 5) % 7) DAY;

INSERT IGNORE INTO menu_plan (menu_date, meal_type, dishes, note, publisher) VALUES
(DATE_ADD(@mon_v10, INTERVAL 0 DAY), '早餐', '牛奶燕麦粥、水煮蛋、小馒头', '优质蛋白+碳水，上午精力更集中', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 0 DAY), '午餐', '软米饭、红烧鸡翅、清炒油麦菜、冬瓜虾皮汤', '虾皮补钙，注意海鲜过敏幼儿替换', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 0 DAY), '午点', '苹果、小饼干', '补充膳食纤维与能量', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 1 DAY), '早餐', '蔬菜瘦肉粥、全麦吐司', '易消化，适合晨间肠胃', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 1 DAY), '午餐', '二米饭、番茄炒蛋、清蒸鳕鱼、紫菜蛋花汤', '鳕鱼刺少，DHA 有助大脑发育；鸡蛋过敏幼儿去蛋', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 1 DAY), '午点', '酸奶、香蕉', '补充钙与钾', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 2 DAY), '早餐', '现磨豆浆、甜玉米、鸡蛋饼', '粗粮搭配，饱腹感好', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 2 DAY), '午餐', '软米饭、土豆炖牛肉、蒜蓉西兰花、菌菇汤', '牛肉含铁量高，有助于预防贫血', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 2 DAY), '午点', '南瓜小米粥', '温和养胃，秋冬适宜', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 3 DAY), '早餐', '小米粥、肉松、凉拌黄瓜', '清淡开胃', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 3 DAY), '午餐', '软米饭、糖醋里脊、上汤娃娃菜、萝卜排骨汤', '糖醋口味偏甜，注意控制用糖量', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 3 DAY), '午点', '雪梨、全麦饼干', '雪梨润燥，适合干燥季节', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 4 DAY), '早餐', '鲜肉小馄饨、卤鹌鹑蛋', '现包现煮，温度适宜', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 4 DAY), '午餐', '扬州炒饭、清炒西葫芦、海带豆腐汤', '荤素搭配，注意为牛奶过敏幼儿换饮品', '刘芳'),
(DATE_ADD(@mon_v10, INTERVAL 4 DAY), '午点', '酸奶、橙子', '补充维生素 C', '刘芳');

-- ============================================================
-- 11. 清理测试脏数据：child.id = 22（姓名/监护人/过敏均为 "1"）
--     先查引用，本库仅 child_change_log 有 1 条记录，一并删除
-- ============================================================
DELETE FROM child_change_log WHERE child_id = 22;
DELETE FROM child WHERE id = 22 AND name = '1';

DROP TEMPORARY TABLE IF EXISTS tmp_teacher_v10;
DROP TEMPORARY TABLE IF EXISTS tmp_child_v10;

-- ============================================================
-- 12. 自检输出
-- ============================================================
SELECT '教职工总数' AS item, COUNT(*) AS cnt FROM teacher WHERE deleted = 0
UNION ALL SELECT '幼儿总数', COUNT(*) FROM child WHERE deleted = 0
UNION ALL SELECT '班级数', COUNT(*) FROM clazz WHERE deleted = 0
UNION ALL SELECT '考勤记录', COUNT(*) FROM attendance
UNION ALL SELECT '收费记录', COUNT(*) FROM fee WHERE deleted = 0
UNION ALL SELECT '课程活动', COUNT(*) FROM course WHERE deleted = 0
UNION ALL SELECT '通知公告', COUNT(*) FROM notice WHERE deleted = 0
UNION ALL SELECT '请假申请', COUNT(*) FROM leave_request WHERE deleted = 0
UNION ALL SELECT '食谱记录', COUNT(*) FROM menu_plan WHERE deleted = 0
UNION ALL SELECT '仍有 null 职称的教职工', COUNT(*) FROM teacher WHERE deleted = 0 AND (title IS NULL OR title = '')
UNION ALL SELECT '仍有 null 开班日期的班级', COUNT(*) FROM clazz WHERE deleted = 0 AND open_date IS NULL
UNION ALL SELECT '姓名残留 "1" 的幼儿', COUNT(*) FROM child WHERE name = '1';
