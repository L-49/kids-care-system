# 幼儿园机构管理系统（前后端分离）

Spring Boot 3.5.16 + MyBatis-Plus 3.5.12 + MySQL 8.0 + Vue 3 + Element Plus + ECharts

## 环境要求

- JDK 17+
- MySQL 8.0（root 账号可执行建表脚本）
- Node.js 18+（建议 20/22）
- Maven 3.8+（或使用 IDEA 自带 Maven）

## 运行步骤

### 1. 初始化数据库

用 MySQL 客户端（Navicat / IDEA 数据库控制台 / 命令行均可）执行：

```
backend/src/main/resources/db/schema.sql
```

脚本会自动建库 `kids_care`、建 13 张表并插入演示数据（12 名幼儿、4 个班级、
6 名教职工、近期考勤与 3 个月收费记录等）。

> 注意：如果你的 MySQL root 密码不是 050816，请打开
> `backend/src/main/resources/application.yml` 修改 `spring.datasource.password`。

### 2. 启动后端（端口 9090）

```bash
cd backend
mvn spring-boot:run
```

或用 IDEA 打开 backend 目录，直接运行 `KidsCareApplication`。

### 3. 启动前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

### 4. 访问系统

浏览器打开 http://localhost:5173

内置账号（密码均为 123456）：

| 账号 | 密码 | 角色 |
|------|--------|------|
| admin | 123456 | 园长（管理员，全部菜单） |
| teacher | 123456 | 教师 |
| parent | 123456 | 家长 |

## 功能模块

登录与权限、幼儿档案、班级管理、教职工管理、考勤管理、课程活动、
每周食谱（含过敏儿童标注）、食材库存（预警）、收费管理（缴费/欠费提醒）、
请假审批、通知公告、统计报表、首页看板、数据字典、操作日志、数据备份。

## 目录结构

```
backend/   后端：Spring Boot 3 + MyBatis-Plus（src/main/resources/db/schema.sql 为建库脚本）
frontend/  前端：Vue 3 + Vite + Element Plus
docs/      报告与截图（可忽略）
```
