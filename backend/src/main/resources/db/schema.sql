-- ============================================================
-- 幼儿园 / 托育机构管理系统 建表脚本
-- 使用方法：在 MySQL 中执行本文件即可（会自动建库）
-- ============================================================
DROP DATABASE IF EXISTS kids_care;
CREATE DATABASE kids_care DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE kids_care;

-- 系统用户（登录）
CREATE TABLE sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录名',
    password    VARCHAR(64)  NOT NULL COMMENT '密码（MD5）',
    real_name   VARCHAR(50)  NOT NULL COMMENT '姓名',
    role        VARCHAR(20)  NOT NULL COMMENT '角色：admin/teacher/parent',
    phone       VARCHAR(20),
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) COMMENT '系统用户';

-- 教职工
CREATE TABLE teacher (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender      VARCHAR(4)   NOT NULL DEFAULT '女' COMMENT '性别',
    phone       VARCHAR(20)  NOT NULL COMMENT '联系电话',
    role        VARCHAR(20)  NOT NULL COMMENT '岗位：园长/教师/保育员/保健医',
    entry_date  DATE         COMMENT '入职日期',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1在职 0离职',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) COMMENT '教职工';

-- 班级
CREATE TABLE clazz (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50) NOT NULL UNIQUE COMMENT '班级名称',
    age_range       VARCHAR(20) COMMENT '适龄段，如 3-4岁',
    room            VARCHAR(50) COMMENT '教室',
    capacity        INT         NOT NULL DEFAULT 25 COMMENT '额定人数',
    head_teacher_id BIGINT      COMMENT '班主任ID',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT     NOT NULL DEFAULT 0
) COMMENT '班级';

-- 幼儿档案
CREATE TABLE child (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    name           VARCHAR(50) NOT NULL COMMENT '幼儿姓名',
    gender         VARCHAR(4)  NOT NULL DEFAULT '男',
    birthday       DATE        COMMENT '出生日期',
    clazz_id       BIGINT      COMMENT '所在班级',
    guardian_name  VARCHAR(50) COMMENT '家长姓名',
    guardian_phone VARCHAR(20) COMMENT '家长电话',
    address        VARCHAR(200) COMMENT '家庭住址',
    allergy        VARCHAR(200) COMMENT '过敏史/禁忌',
    enroll_date    DATE        COMMENT '入园日期',
    status         TINYINT     NOT NULL DEFAULT 1 COMMENT '1在园 0离园',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        TINYINT     NOT NULL DEFAULT 0,
    KEY idx_child_clazz (clazz_id)
) COMMENT '幼儿档案';

-- 考勤
CREATE TABLE attendance (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    child_id      BIGINT      NOT NULL COMMENT '幼儿ID',
    clazz_id      BIGINT      COMMENT '班级ID（冗余，便于按班统计）',
    att_date      DATE        NOT NULL COMMENT '考勤日期',
    status        VARCHAR(10) NOT NULL COMMENT '出勤/迟到/请假/缺勤',
    check_in_time VARCHAR(10) COMMENT '入园时间 HH:mm',
    remark        VARCHAR(200),
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_att (child_id, att_date)
) COMMENT '考勤记录';

-- 课程活动
CREATE TABLE course (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL COMMENT '课程名称',
    clazz_id    BIGINT       NOT NULL COMMENT '班级',
    teacher_id  BIGINT       COMMENT '授课教师',
    course_date DATE         NOT NULL COMMENT '上课日期',
    start_time  VARCHAR(10)  COMMENT '开始时间 HH:mm',
    end_time    VARCHAR(10)  COMMENT '结束时间 HH:mm',
    content     VARCHAR(500) COMMENT '活动内容',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0取消',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    KEY idx_course_date (course_date)
) COMMENT '课程活动';

-- 收费
CREATE TABLE fee (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    child_id    BIGINT        NOT NULL COMMENT '幼儿ID',
    fee_type    VARCHAR(20)   NOT NULL COMMENT '保教费/餐费/校车费/其他',
    amount      DECIMAL(10,2) NOT NULL COMMENT '金额',
    fee_month   VARCHAR(7)    NOT NULL COMMENT '所属月份 yyyy-MM',
    pay_status  TINYINT       NOT NULL DEFAULT 0 COMMENT '0未缴 1已缴',
    pay_time    DATETIME      COMMENT '缴费时间',
    remark      VARCHAR(200),
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    KEY idx_fee_month (fee_month)
) COMMENT '收费记录';

-- 请假申请
CREATE TABLE leave_request (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    child_id     BIGINT       NOT NULL COMMENT '幼儿ID',
    start_date   DATE         NOT NULL COMMENT '开始日期',
    end_date     DATE         NOT NULL COMMENT '结束日期',
    days         INT          NOT NULL DEFAULT 1 COMMENT '请假天数',
    reason       VARCHAR(300) NOT NULL COMMENT '请假事由',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审批 1已通过 2已驳回',
    approver     VARCHAR(50)  COMMENT '审批人',
    approve_time DATETIME     COMMENT '审批时间',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0
) COMMENT '请假申请';

-- 通知公告
CREATE TABLE notice (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(100) NOT NULL COMMENT '标题',
    content      VARCHAR(1000) COMMENT '内容',
    type         VARCHAR(20)  NOT NULL DEFAULT '园务' COMMENT '类型：园务/班级/健康/活动',
    publisher    VARCHAR(50)  COMMENT '发布人',
    publish_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted      TINYINT      NOT NULL DEFAULT 0
) COMMENT '通知公告';

-- ============================================================
-- 模块六：膳食管理（食谱 + 食材库存）
-- ============================================================
-- 每周食谱
CREATE TABLE menu_plan (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    menu_date   DATE         NOT NULL COMMENT '日期',
    meal_type   VARCHAR(10)  NOT NULL COMMENT '餐次：早餐/午餐/午点',
    dishes      VARCHAR(300) NOT NULL COMMENT '菜品',
    note        VARCHAR(200) COMMENT '备注/营养说明',
    publisher   VARCHAR(50) COMMENT '发布人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_menu (menu_date, meal_type)
) COMMENT '每周食谱';

-- 食材库存
CREATE TABLE food_stock (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)   NOT NULL COMMENT '食材名称',
    category    VARCHAR(20)   NOT NULL COMMENT '分类：主食/蔬菜/肉类/水果/其他',
    quantity    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '库存数量',
    unit        VARCHAR(10)   NOT NULL DEFAULT 'kg' COMMENT '单位',
    warn_line   DECIMAL(10,2) NOT NULL DEFAULT 5 COMMENT '预警线',
    supplier    VARCHAR(50) COMMENT '供应商',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0
) COMMENT '食材库存';

-- ============================================================
-- 模块十：系统设置与日志
-- ============================================================
-- 数据字典
CREATE TABLE sys_dict (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_type   VARCHAR(50) NOT NULL COMMENT '字典类型，如 meal_type',
    dict_label  VARCHAR(50) NOT NULL COMMENT '字典标签',
    dict_value  VARCHAR(50) NOT NULL COMMENT '字典值',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    remark      VARCHAR(200),
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0
) COMMENT '数据字典';

-- 操作日志
CREATE TABLE sys_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) COMMENT '操作人',
    operation   VARCHAR(200) COMMENT '操作描述',
    method      VARCHAR(10) COMMENT '请求方式',
    uri         VARCHAR(200) COMMENT '请求地址',
    params      VARCHAR(500) COMMENT '请求参数',
    ip          VARCHAR(50) COMMENT '操作IP',
    cost_time   BIGINT COMMENT '耗时(毫秒)',
    status      INT COMMENT 'HTTP状态码',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '操作日志';

-- ============================================================
-- 初始化数据
-- ============================================================
-- 密码均为 123456（MD5: e10adc3949ba59abbe56e057f20f883e）
INSERT INTO sys_user (username, password, real_name, role, phone) VALUES
('admin',   'e10adc3949ba59abbe56e057f20f883e', '园长', 'admin',   '13800000001'),
('teacher', 'e10adc3949ba59abbe56e057f20f883e', '李老师', 'teacher', '13800000002'),
('parent',  'e10adc3949ba59abbe56e057f20f883e', '张先生', 'parent',  '13800000003');

INSERT INTO teacher (name, gender, phone, role, entry_date) VALUES
('李静',  '女', '13900000001', '园长',   '2018-03-01'),
('王敏',  '女', '13900000002', '教师',   '2020-09-01'),
('张丽',  '女', '13900000003', '教师',   '2021-09-01'),
('刘芳',  '女', '13900000004', '保育员', '2022-03-01'),
('陈鹏',  '男', '13900000005', '教师',   '2023-09-01'),
('赵雪',  '女', '13900000006', '保健医', '2019-09-01');

INSERT INTO clazz (name, age_range, room, capacity, head_teacher_id) VALUES
('小班',  '3-4岁', 'A101', 25, 2),
('中班',  '4-5岁', 'A102', 25, 3),
('大班',  '5-6岁', 'A201', 30, 5),
('托班',  '2-3岁', 'A103', 18, 4);

INSERT INTO child (name, gender, birthday, clazz_id, guardian_name, guardian_phone, address, allergy, enroll_date) VALUES
('张小明', '男', '2021-05-12', 1, '张建国', '13800000003', '朝阳区阳光小区1-201', '芒果过敏',  '2024-09-01'),
('李朵朵', '女', '2021-08-20', 1, '李红梅', '13800000004', '海淀区学院路5号',     '',         '2024-09-01'),
('王梓涵', '男', '2020-11-03', 2, '王志强', '13800000005', '西城区月坛南街2号',   '花粉过敏',  '2023-09-01'),
('刘雨桐', '女', '2020-07-18', 2, '刘伟',   '13800000006', '丰台区嘉园三里8号',   '',         '2023-09-01'),
('陈子轩', '男', '2019-12-01', 3, '陈俊',   '13800000007', '朝阳区望京西园4区',   '',         '2022-09-01'),
('赵一诺', '女', '2019-09-25', 3, '赵磊',   '13800000008', '东城区和平里六区',    '牛奶过敏',  '2022-09-01'),
('孙浩然', '男', '2022-02-14', 4, '孙涛',   '13800000009', '石景山区鲁谷路10号',  '',         '2025-03-01'),
('周若曦', '女', '2022-06-09', 4, '周斌',   '13800000010', '通州区梨园南街6号',   '',         '2025-03-01'),
('吴俊熙', '男', '2020-04-22', 2, '吴刚',   '13800000011', '昌平区回龙观东大街',  '',         '2023-09-01'),
('郑欣怡', '女', '2021-01-30', 1, '郑凯',   '13800000012', '大兴区黄村西大街',    '海鲜过敏',  '2024-09-01'),
('冯梓萱', '女', '2019-10-11', 3, '冯军',   '13800000013', '海淀区中关村南大街',  '',         '2022-09-01'),
('黄子豪', '男', '2021-03-08', 1, '黄磊',   '13800000014', '朝阳区双井桥东',      '',         '2024-09-01');

INSERT INTO attendance (child_id, clazz_id, att_date, status, check_in_time, remark) VALUES
(1, 1, CURDATE(), '出勤', '08:05', ''),
(2, 1, CURDATE(), '迟到', '08:45', '家长堵车'),
(3, 2, CURDATE(), '出勤', '07:58', ''),
(4, 2, CURDATE(), '请假', '',     '感冒请假'),
(5, 3, CURDATE(), '出勤', '08:02', ''),
(6, 3, CURDATE(), '缺勤', '',     '未到园，已联系家长'),
(7, 4, CURDATE(), '出勤', '08:10', ''),
(8, 4, CURDATE(), '出勤', '08:12', '');

INSERT INTO course (title, clazz_id, teacher_id, course_date, start_time, end_time, content) VALUES
('快乐阅读',   1, 2, CURDATE(), '09:00', '09:40', '绘本《好饿的毛毛虫》共读与表演'),
('手工黏土',   2, 3, CURDATE(), '10:00', '10:40', '制作小动物，锻炼精细动作'),
('户外体能',   3, 5, CURDATE(), '15:00', '15:40', '障碍跑与平衡练习'),
('音乐律动',   4, 4, CURDATE(), '09:30', '10:00', '跟着节奏做律动，培养节奏感'),
('科学小实验', 3, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00', '09:40', '水的三态观察');

INSERT INTO fee (child_id, fee_type, amount, fee_month, pay_status, pay_time, remark) VALUES
(1, '保教费', 1800.00, DATE_FORMAT(CURDATE(), '%Y-%m'), 1, NOW(), ''),
(1, '餐费',    600.00, DATE_FORMAT(CURDATE(), '%Y-%m'), 1, NOW(), ''),
(2, '保教费', 1800.00, DATE_FORMAT(CURDATE(), '%Y-%m'), 0, NULL, '待缴'),
(3, '保教费', 1800.00, DATE_FORMAT(CURDATE(), '%Y-%m'), 1, NOW(), ''),
(4, '校车费',  300.00, DATE_FORMAT(CURDATE(), '%Y-%m'), 0, NULL, ''),
(5, '保教费', 2000.00, DATE_FORMAT(CURDATE(), '%Y-%m'), 1, NOW(), '');

INSERT INTO leave_request (child_id, start_date, end_date, days, reason, status, approver, approve_time) VALUES
(4, CURDATE(), CURDATE(), 1, '感冒发烧，需在家休息', 1, '王敏', NOW()),
(2, DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY), 2, '随家长外出', 0, NULL, NULL),
(6, DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), 1, '牙科复诊', 2, '张丽', NOW());

INSERT INTO notice (title, content, type, publisher) VALUES
('春季传染病预防通知', '近期流感高发，请家长每日晨检，如有发热、咳嗽请及时告知班级教师。', '通知', '赵雪'),
('亲子运动会报名',     '本周五下午举办亲子运动会，请家长于周四前在班级群报名。',        '活动', '李静'),
('本周食谱调整',       '因食材供应调整，周三午餐由番茄炖牛腩改为土豆烧牛肉，其余不变。',  '食谱', '刘芳'),
('国庆假期安排',       '10 月 1 日至 7 日放假，10 月 8 日正常返园，请家长提前安排接送。', '放假', '赵雪');

-- 食谱（当周）
INSERT INTO menu_plan (menu_date, meal_type, dishes, note, publisher) VALUES
(CURDATE(), '早餐', '小米粥、鸡蛋饼、凉拌黄瓜', '易消化，补充优质蛋白', '刘芳'),
(CURDATE(), '午餐', '软米饭、番茄炖牛腩、清炒西兰花、紫菜蛋花汤', '含铁与维生素C，促进吸收', '刘芳'),
(CURDATE(), '午点', '酸奶、香蕉', '补充钙质与钾', '刘芳'),
(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '早餐', '南瓜粥、菜肉包', '南瓜富含胡萝卜素', '刘芳'),
(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '午餐', '二米饭、清蒸鲈鱼、蒜蓉油麦菜、冬瓜排骨汤', '鱼类DHA有助于大脑发育', '刘芳'),
(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '午点', '红豆羹、苹果', '补充膳食纤维', '刘芳'),
(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '早餐', '燕麦牛奶、全麦面包、水煮蛋', '粗粮搭配，饱腹感好', '刘芳'),
(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '午餐', '软米饭、土豆烧鸡块、上汤娃娃菜、菌菇汤', '注意为芒果过敏幼儿替换水果', '刘芳'),
(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '午点', '小馄饨', '现包现煮，温度适宜', '刘芳');

-- 食材库存（含 2 条低于预警线）
INSERT INTO food_stock (name, category, quantity, unit, warn_line, supplier) VALUES
('大米',     '主食', 120.00, 'kg', 30, '京粮配送'),
('面粉',     '主食', 45.00,  'kg', 20, '京粮配送'),
('西红柿',   '蔬菜', 12.50,  'kg', 10, '绿源蔬菜'),
('西兰花',   '蔬菜', 6.00,   'kg', 8,  '绿源蔬菜'),
('牛腩',     '肉类', 18.00,  'kg', 10, '众品冷鲜'),
('鲈鱼',     '肉类', 9.00,   'kg', 8,  '海丰水产'),
('香蕉',     '水果', 15.00,  'kg', 10, '鲜果时光'),
('苹果',     '水果', 4.00,   'kg', 8,  '鲜果时光'),
('牛奶',     '其他', 60.00,  'L',  30, '蒙牛供应'),
('食用油',   '其他', 25.00,  'L',  10, '京粮配送');

-- 数据字典
INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort, status, remark) VALUES
('meal_type',   '早餐', '早餐', 1, 1, '食谱餐次'),
('meal_type',   '午餐', '午餐', 2, 1, '食谱餐次'),
('meal_type',   '午点', '午点', 3, 1, '食谱餐次'),
('food_category','主食', '主食', 1, 1, '食材分类'),
('food_category','蔬菜', '蔬菜', 2, 1, '食材分类'),
('food_category','肉类', '肉类', 3, 1, '食材分类'),
('food_category','水果', '水果', 4, 1, '食材分类'),
('food_category','其他', '其他', 5, 1, '食材分类'),
('fee_type',    '保教费', '保教费', 1, 1, '收费类型'),
('fee_type',    '餐费',   '餐费',   2, 1, '收费类型'),
('fee_type',    '校车费', '校车费', 3, 1, '收费类型'),
('att_status',  '出勤', '出勤', 1, 1, '考勤状态'),
('att_status',  '迟到', '迟到', 2, 1, '考勤状态'),
('att_status',  '请假', '请假', 3, 1, '考勤状态'),
('att_status',  '缺勤', '缺勤', 4, 1, '考勤状态'),
('notice_type', '通知', '通知', 1, 1, '公告类型'),
('notice_type', '活动', '活动', 2, 1, '公告类型'),
('notice_type', '食谱', '食谱', 3, 1, '公告类型'),
('notice_type', '放假', '放假', 4, 1, '公告类型');

