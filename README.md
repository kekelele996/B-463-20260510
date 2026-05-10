# 企业固定资产管理系统 (Enterprise Asset Management System)

一个基于 Spring Boot + Vue 3 的现代化企业资产管理平台，支持 RBAC 权限控制、设备与车辆的全生命周期管理、可视化仪表盘等功能。

## 🛠 技术栈
- **Frontend**: Vue 3 + Element Plus + ECharts + Axios
- **Backend**: Java Spring Boot 3 + MyBatis Plus + Spring Security + JWT
- **DB**: MySQL 8.0 (Dockerized)
- **Infrastructure**: Docker Compose

## 🚀 快速启动 (Docker)
1. 确保 Docker Desktop 已运行。
2. 在项目根目录打开终端，执行以下命令构建并启动容器：
   ```bash
   docker compose up --build
   ```
3. 等待容器启动完成后（控制台不再快速滚动日志）：
   - **访问前端页面**：http://localhost:3002
   - **访问后端 API**：http://localhost:8082/api

> **注意**：如果端口 3002 或 8082 被占用，请修改 `docker-compose.yml` 中的端口映射。

## 🧪 测试账号

系统预置了两个角色的账号，用于测试不同的权限流程。

| 角色 | 用户名 | 密码 | 权限说明 |
| :--- | :--- | :--- | :--- |
| **系统管理员** | `admin` | `123456` | 拥有所有权限 (用户管理、设备增删改查、车辆管理) |
| **普通用户** | `user1` | `123456` | 仅限查看/编辑设备与车辆状态 (无法删除，无法管理用户) |

## 📸 功能介绍

### 1. 仪表盘 (Dashboard)
- 实时展示设备总数、在线/离线状态统计。
- 使用 ECharts 可视化图表展示车辆状态分布。

### 2. 用户权限管理 (RBAC)
- **管理员视图**: 可以查看“用户管理”菜单，添加新用户，分配角色。
- **权限隔离**: 普通用户登录后，侧边栏自动隐藏“用户管理”入口；尝试直接访问受限 API 会被拦截 (403 Forbidden)。

### 3. 资产全生命周期管理
- **设备管理**:
  - 支持设备的新增、编辑、删除（仅管理员）。
  - **维护追踪**: 记录最后一次维护操作的人员，确保责任到人。
- **车辆管理**:
  - 车辆状态跟踪（行驶中、停泊中、维护中）。
  - **数据校验**: 后端严格校验车牌号、型号等必填字段，防止无效数据录入。

### 4. 安全与稳定性特性
- **JWT 认证**: 所有 API 请求均需携带 Token，实现无状态认证。
- **防 SQL 注入**: 底层使用 MyBatis Plus 预编译 SQL。
- **交互安全**: 前端实现按钮防抖，防止重复提交；API 错误统一拦截处理。
- **编码规范**: 全链路强制 UTF-8 编码，确保中文字符（如角色名、错误提示）正确显示。

## 📂 目录结构

```
.
├── backend/            # Java Spring Boot 后端源码
├── frontend/           # Vue 3 前端源码
├── docker-compose.yml  # 容器编排配置
├── init.sql            # 数据库初始化脚本
└── README.md           # 项目说明文档
```
