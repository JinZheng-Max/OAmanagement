# 智办 AI OA 系统 — 完整安装启动手册

> **适用人群**：电脑初级用户  
> **更新日期**：2026-07-23  
> **项目版本**：v2.0（含 AI 知识库、Flowable 工作流、OSS 存储）

---

## 目录

1. [需要安装的软件](#1-需要安装的软件)
2. [下载项目代码](#2-下载项目代码)
3. [配置环境](#3-配置环境)
4. [启动基础设施](#4-启动基础设施)
5. [启动后端服务](#5-启动后端服务)
6. [启动前端](#6-启动前端)
7. [打开系统](#7-打开系统)
8. [导入知识库文档（可选）](#8-导入知识库文档可选)
9. [常见问题](#9-常见问题)
10. [附录：系统架构](#10-附录系统架构)

---

## 1. 需要安装的软件

| 软件 | 版本要求 | 下载地址 | 用途 |
|:-----|:---------|:---------|:-----|
| **JDK 21** | ≥ 21 | https://www.oracle.com/java/technologies/downloads/ | 运行后端 Java 服务 |
| **Node.js** | ≥ 18 | https://nodejs.org/ | 运行前端网页 |
| **MySQL** | ≥ 8.0 | https://dev.mysql.com/downloads/installer/ | 数据存储 |
| **Redis** | ≥ 7.0 | https://github.com/tporadowski/redis/releases | 缓存与登录会话 |
| **Docker Desktop** | 最新版 | https://www.docker.com/products/docker-desktop/ | 运行 AI 向量数据库 |
| **Git** | 最新版 | https://git-scm.com/ | 下载代码 |
| **Nacos** | 2.x | https://github.com/alibaba/nacos/releases | 服务注册与配置中心 |

### 安装验证

安装完成后，打开命令提示符（`Win+R` → `cmd`），输入以下命令验证：

```bash
java -version          # 应显示 openjdk version "21"
node -v                # 应显示 v18 或更高
npm -v                 # 应显示对应版本号
mysql -V              # 应显示 mysql 版本
redis-cli -v          # 应显示 redis 版本
docker --version      # 应显示 Docker 版本
git --version         # 应显示 git 版本
```

---

## 2. 下载项目代码

打开命令行，执行：

```bash
cd D:\QQ\download
git clone https://github.com/JinZheng-Max/OAmanagement.git
cd OAmanagement
```

> 💡 如果你已有项目文件夹，直接进入项目目录即可

---

## 3. 配置环境

### 3.1 创建数据库

```bash
mysql -u root -p
```

输入你的 MySQL 密码进入 `mysql>` 提示符后，执行：

```sql
CREATE DATABASE IF NOT EXISTS oa_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oa_db;
SOURCE sql/data.sql;
```

### 3.2 配置 .env 文件

项目根目录下的 `.env` 文件，用记事本打开，修改以下配置：

```ini
MYSQL_PASSWORD=你的MySQL密码       # ← 改成你的密码
JWT_SECRET=vRF4pOju7IYF/...       # 保持不动或用你自己的密钥
DEEPSEEK_API_KEY=你的API密钥       # ← DeepSeek API Key，用于AI问答
NACOS_ENABLED=false               # 不需要改
```

### 3.3 配置 hosts 文件（如果 Nacos 报错）

用管理员身份打开 `C:\Windows\System32\drivers\etc\hosts`，确保有：

```
127.0.0.1 localhost
```

---

## 4. 启动基础设施

按顺序启动以下服务，**每个服务启动后不要关掉窗口**。

### 4.1 启动 Nacos

```bash
cd D:\nacos\bin
startup.cmd -m standalone
```

等待 10 秒后，浏览器访问 http://localhost:8848/nacos 能看到登录页即成功。

### 4.2 启动 Docker 容器（AI 向量库）

```bash
cd D:\QQ\download\OAmanagement-main
docker compose up -d
```

这会启动两个服务：
- **ChromaDB**（向量数据库）→ `localhost:8000`
- **Embedding**（文本转向量）→ `localhost:8080`

验证：

```bash
docker ps
```

看到 `oa-chroma` 和 `oa-embedding` 状态为 `Up` 即成功。

### 4.3 启动 Redis

双击 `D:\software\Redis\redis-server.exe`，或执行：

```bash
redis-server
```

验证：

```bash
redis-cli ping
```

返回 `PONG` 即成功。

---

## 5. 启动后端服务

> ⚠️ **必须按顺序启动**，前一个启动成功后再启动下一个。

### 5.1 启动 identity-service（认证 + 部门 + 员工 + 请假 + Flowable 工作流）

```bash
cd D:\QQ\download\OAmanagement-main
mvnw spring-boot:run -pl identity-service -Dmaven.test.skip=true
```

等待约 30~50 秒，看到 `Started IdentityServiceApplication` 即为成功。

端口：**8851**

### 5.2 启动 oa-content-service（公告制度 + AI 知识库）

```bash
mvnw spring-boot:run -pl oa-content-service -Dmaven.test.skip=true -Dspring-boot.run.jvmArguments="-Xmx1g -Xms256m"
```

等待约 20~30 秒，看到 `Started OaContentServiceApplication` 即为成功。

端口：**8853**

### 5.3 启动 oa-gateway（API 网关）

```bash
mvnw spring-boot:run -pl oa-gateway -Dmaven.test.skip=true
```

等待约 15~20 秒，看到 `Started OaGatewayApplication` 即为成功。

端口：**10002**

---

## 6. 启动前端

前端采用 **Vue 3 + Element Plus + Vite**，位于 `newFrontEnd` 目录（已弃用旧版 `oa-web`）。

```bash
cd D:\QQ\download\OAmanagement-main\newFrontEnd
npm install        # 第一次运行需要安装依赖（仅一次）
npm run dev
```

看到 `http://localhost:5173` 即成功。

---

## 7. 打开系统

1. 打开浏览器访问 **http://localhost:5173**
2. 登录账号：

| 账号 | 密码 | 角色 |
|:-----|:-----|:-----|
| `admin` | `Admin@123456` | 超级管理员 |
| `zhangsan` | `emp123` | 部门经理 |
| `lisi` | `emp123` | 普通员工 |

### 功能模块总览

| 菜单 | 功能说明 |
|:-----|:---------|
| 📊 仪表盘 | 公司概览、待办任务、快捷入口 |
| 👥 组织管理 | 部门 CRUD + 员工 CRUD（含身份证/邮箱/工作年限） |
| 📋 请假审批 | 提交请假 → 部门经理审批 → (超3天)总管理员审批 |
| 📅 考勤管理 | 签到/签退/考勤看板/管理员补录 |
| 📢 公告制度 | 公告与制度的发布/下架/全文检索 |
| 🤖 AI 智能助手 | 基于 RAG 知识库的智能问答 + SSE 流式输出 |

---

## 8. 导入知识库文档（可选）

1. 准备企业 docx/pdf 文档
2. 放入 `D:\QQ\download\知识库word\` 目录
3. 调用批量导入接口（或通过前端"知识文档管理"页面上传）：

```bash
curl -X POST http://localhost:8853/api/ai/batch-import
```

4. 等待约 1~2 分钟，系统自动完成：解析 → 分片 → 向量化 → 存入 ChromaDB
5. 回到 **AI 智能助手** 页面即可提问

---

## 9. 常见问题

### Q1: 启动时端口被占用

```bash
netstat -ano | findstr 8853
```

找到占用端口的 PID，在任务管理器中结束该进程。

### Q2: 连不上数据库

检查：
- MySQL 服务是否运行（任务管理器找 `mysqld.exe`）
- `.env` 文件中的 `MYSQL_PASSWORD` 是否正确
- 数据库 `oa_db` 是否已创建

### Q3: AI 问答返回"知识库中暂无相关内容"

原因：ChromaDB 中没有向量数据。
解决：先上传知识文档（见第 8 节），等待索引完成（状态变为"已就绪"）。

### Q4: 文档索引失败

如果知识库页面显示"异常"状态：
1. 点击文档后的 **重索引** 按钮
2. 等待 1~2 分钟
3. 刷新页面查看状态

### Q5: 前端页面白屏

```bash
cd newFrontEnd
npm install   # 重新安装依赖
npm run dev   # 重新启动
```

### Q6: Docker 启动报错

确保 Docker Desktop 已启动（任务栏右下角有 Docker 图标）。
如果提示 "WSL 2 安装不完整"，请安装 WSL 2：

```bash
wsl --install
```

---

## 10. 附录：系统架构

```
┌──────────────────────────────────────────────────────────┐
│                    浏览器 (localhost:5173)                 │
│                    (newFrontEnd - Vue 3)                  │
└─────────────────────────┬────────────────────────────────┘
                          │ HTTP / API
                          ▼
┌──────────────────────────────────────────────────────────┐
│              API 网关 (oa-gateway :10002)                │
└────┬────────────┬──────────────┬─────────────────────────┘
     │            │              │
     ▼            ▼              ▼
┌──────────┐ ┌──────────┐ ┌──────────────┐
│identity  │ │content   │ │attendance    │
│:8851     │ │:8853     │ │:8849         │
│          │ │          │ │              │
│• 认证    │ │• 公告制度│ │• 签到签退   │
│• 部门    │ │• AI问答  │ │• 考勤看板   │
│• 员工    │ │• 知识库  │ │• 补录       │
│• 请假    │ │• OSS存储 │ │              │
│• Flowable│ │          │ │              │
└────┬─────┘ └────┬─────┘ └──────────────┘
     │            │
     ▼            ▼
┌──────────┐ ┌──────────┐
│  MySQL   │ │ ChromaDB │
│  Redis   │ │ :8000    │
│  Nacos   │ │Embedding │
│  :8848   │ │ :8080    │
└──────────┘ └──────────┘
```

### 端口一览

| 服务 | 端口 | 说明 |
|:-----|:----:|:-----|
| 前端 (Vite) | 5173 | 开发服务器 |
| 网关 | 10002 | API 统一入口 |
| identity-service | 8851 | 用户/部门/员工/请假 |
| oa-attendance-service | 8849 | 考勤管理 |
| oa-content-service | 8853 | 公告/AI/知识库 |
| Nacos | 8848 | 注册中心 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| ChromaDB | 8000 | AI 向量库 |
| Embedding | 8080 | 文本向量化 |

### 启动顺序速查

```
1. Nacos          → startup.cmd -m standalone
2. Docker         → docker compose up -d
3. Redis          → redis-server
4. identity       → mvnw spring-boot:run -pl identity-service ...
5. content        → mvnw spring-boot:run -pl oa-content-service ...
6. gateway        → mvnw spring-boot:run -pl oa-gateway ...
7. newFrontEnd    → cd newFrontEnd && npm run dev
```
