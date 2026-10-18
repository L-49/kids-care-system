# 幼儿园机构管理系统（前后端分离）

Spring Boot 3.5 + MyBatis-Plus 3.5 + MySQL 8.0 + Vue 3 + Element Plus + ECharts

## 环境要求

- JDK 17+
- MySQL 8.0
- Node.js 18+（建议 20 / 22）
- Maven 3.8+（或使用 IDEA 自带 Maven）

## 快速开始

### 1. 初始化数据库

按版本顺序执行 `backend/src/main/resources/db/` 下的脚本（除 `schema.sql` 外均为幂等脚本，可重复执行）：

```bash
mysql -uroot -p < backend/src/main/resources/db/schema.sql       # 建库 + 13 张表 + 基础演示数据
mysql -uroot -p kids_care < backend/src/main/resources/db/v2_diet_system.sql
mysql -uroot -p kids_care < backend/src/main/resources/db/v3_demo_data.sql
mysql -uroot -p kids_care < backend/src/main/resources/db/v4_notice_status.sql
mysql -uroot -p kids_care < backend/src/main/resources/db/v5_child_change_log.sql
mysql -uroot -p kids_care < backend/src/main/resources/db/v6_notice_target.sql
mysql -uroot -p kids_care < backend/src/main/resources/db/v8_member1_modules.sql
mysql -uroot -p kids_care < backend/src/main/resources/db/v9_parent_portal.sql
mysql -uroot -p kids_care < backend/src/main/resources/db/v10_sync_member1_data.sql
mysql -uroot -p kids_care < backend/src/main/resources/db/v11_plan_a.sql
```

全部执行后约 35 名幼儿、13 名教职工、4 个班级、128 条收费记录、195 条考勤记录。

### 2. 配置数据库连接

`backend/src/main/resources/application.yml` 使用环境变量优先，本地默认值可直接运行：

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/kids_care?...` | 连接串 |
| `DB_USERNAME` | `root` | 账号 |
| `DB_PASSWORD` | `123456` | 密码 |

部署时用环境变量覆盖，不要改文件。

### 3. 启动后端（端口 9090）

```bash
cd backend
mvn spring-boot:run
```

或用 IDEA 打开 `backend` 目录，运行 `KidsCareApplication`。

### 4. 启动前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

### 5. 访问

浏览器打开 http://localhost:5173

内置账号（密码均为 `123456`）：

| 账号 | 角色 | 可见范围 |
|---|---|---|
| admin | 园长 | 全部菜单 |
| teacher | 教师 | 幼儿档案、班级、考勤、课程、食谱、库存、收费、请假、公告、我的孩子看板；**无教职工管理、无统计报表** |
| parent | 家长 | 仅「我的孩子」（考勤、请假、食谱、公告） |

> 首次登录后口令会由 MD5 自动升级为 PBKDF2-HmacSHA256（兼容历史 MD5 账号）。

## 功能模块

登录与权限（RBAC）、幼儿档案（导入导出、调班、变更留痕）、班级管理（满员状态、幼儿名单）、
教职工管理（人事档案、任教班级、任教课程）、考勤管理、课程活动（排课时段冲突校验）、
每周食谱（过敏儿童标注、复制上周）、食材库存（出入库台账、低库存预警）、
收费管理（缴费登记、欠费统计）、请假审批、通知公告（发布/撤回）、
统计报表、首页看板、数据字典、操作日志、数据备份、家长端「我的孩子」。

## 目录结构

```
backend/    Spring Boot 3 + MyBatis-Plus
  src/main/java/com/kids/care/
    common/       鉴权拦截器、全局异常、日志拦截、口令工具
    controller/   15 个 REST 控制器，75 个端点
    service/      业务逻辑（含校验与统计聚合）
    entity/       MyBatis-Plus 实体
  src/main/resources/
    db/           建库与版本升级脚本
    logback-spring.xml   控制台 + 按天滚动文件日志
frontend/   Vue 3 + Vite + Element Plus
  src/views/       14 个业务页面
  src/components/CrudPage.vue  通用增删改查页组件（支持只读模式）
```

## 日志

- 运行日志：`backend/logs/kids-care.log`（按天滚动，保留 30 天，单文件 50MB）
- 错误日志：`backend/logs/kids-care-error.log`
- 操作审计：数据库 `sys_log` 表，记录操作人、动作、URI、耗时、HTTP 状态与失败原因

## 开发约定

- 数据库变更一律新增 `vN_xxx.sql` 幂等脚本，不改历史脚本
- 接口权限用 `@RequireRole` 注解声明，前后端菜单同步控制
- 逻辑删除统一 `deleted` 字段，MyBatis-Plus 全局配置
