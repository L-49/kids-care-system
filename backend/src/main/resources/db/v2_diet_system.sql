USE kids_care;
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
('att_status',  '缺勤', '缺勤', 4, 1, '考勤状态');

