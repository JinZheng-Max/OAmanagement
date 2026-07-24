# 智办 AI OA 系统 — 完整安装启动手册

> 📅 更新日期：2026-07-24
> 📦 版本：v2.0
> 👤 适用人群：电脑初级用户（跟着步骤做就能成功）

---

## 📖 本手册能帮你做什么？

跟着本手册做完，你将拥有一个完整运行的企业 OA 系统，包含以下功能：

| 功能 | 说明 |
|:-----|:------|
| 👥 **组织管理** | 部门添加/编辑，员工档案管理（含身份证、邮箱、工作年限） |
| 📋 **请假审批** | 员工提交请假 → 部门经理审批 → 超3天转总管理员（Flowable 工作流引擎驱动） |
| 📅 **考勤管理** | 每日签到/签退打卡，管理员考勤看板与补录 |
| 📢 **公告制度** | 发布、下架公告与制度，全文检索 |
| 🤖 **AI 智能助手** | 上传企业知识文档 → 自动索引 → 基于 RAG 的智能问答（流式输出） |

---

## 目录

- [第一章：需要安装的软件（共 7 个）](#第一章需要安装的软件共-7-个)
- [第二章：下载项目代码](#第二章下载项目代码)
- [第三章：配置环境](#第三章配置环境)
- [第四章：启动基础设施](#第四章启动基础设施)
- [第五章：启动后端服务](#第五章启动后端服务)
- [第六章：启动前端](#第六章启动前端)
- [第七章：登录系统](#第七章登录系统)
- [第八章：导入知识库文档（让AI能回答问题）](#第八章导入知识库文档让ai能回答问题)
- [第九章：常见问题与解决方法](#第九章常见问题与解决方法)
- [附录一：启动速查表](#附录一启动速查表)
- [附录二：端口一览](#附录二端口一览)
- [附录三：系统架构图](#附录三系统架构图)

---

## 第一章：需要安装的软件（共 7 个）

### 1.1 安装清单

| 软件 | 版本 | 下载地址 | 用途 | 安装难度 |
|:-----|:-----|:---------|:-----|:--------|
| **JDK 21** | ≥ 21 | [Oracle 官网](https://www.oracle.com/java/technologies/downloads/) | 运行后端 Java 程序 | ⭐ |
| **Node.js** | ≥ 18 | [Node.js 官网](https://nodejs.org/) | 运行前端网页 | ⭐ |
| **MySQL** | ≥ 8.0 | [MySQL 官网](https://dev.mysql.com/downloads/installer/) | 存储数据 | ⭐⭐ |
| **Redis** | ≥ 7.0 | [GitHub 下载](https://github.com/tporadowski/redis/releases) | 缓存、登录会话 | ⭐ |
| **Docker Desktop** | 最新版 | [Docker 官网](https://www.docker.com/products/docker-desktop/) | 运行 AI 向量数据库 | ⭐⭐ |
| **Nacos** | 2.x | [GitHub 下载](https://github.com/alibaba/nacos/releases) | 服务注册中心 | ⭐⭐ |
| **Git** | 最新版 | [Git 官网](https://git-scm.com/) | 下载代码 | ⭐ |

### 1.2 详细安装步骤

#### ▶ JDK 21（约 5 分钟）

1. 打开 https://www.oracle.com/java/technologies/downloads/
2. 选择 **Windows → x64 Installer**，下载 `.exe` 文件（约 180MB）
3. 双击安装，一路点"下一步"（不需要改任何设置）
4. 安装完成后，打开**命令提示符**（按 `Win + R`，输入 `cmd`，回车）
5. 输入 `java -version`，看到类似下面的信息即成功：

```
openjdk version "21.0.4" 2024-07-16
Java(TM) SE Runtime Environment (build 21.0.4+8-LTS-...)
```

**常见问题**：如果提示 `'java' 不是内部或外部命令`，说明需要手动设置环境变量：
1. 右键"此电脑" → 属性 → 高级系统设置 → 环境变量
2. 新建 `JAVA_HOME` = `C:\Program Files\Java\jdk-21`
3. 在 `Path` 中添加 `%JAVA_HOME%\bin`

#### ▶ Node.js（约 3 分钟）

1. 打开 https://nodejs.org/
2. 点击左侧绿色按钮 **LTS**（长期支持版）
3. 双击安装，一路"下一步"
4. 安装完成后，打开命令提示符输入：

```bash
node -v    # 应显示 v18.x.x 或更高
npm -v     # 应显示 9.x.x 或更高
```

#### ▶ MySQL（约 10 分钟）

1. 打开 https://dev.mysql.com/downloads/installer/
2. 下载 **MySQL Installer for Windows**（约 400MB）
3. 安装类型选 **Server only**
4. 安装过程中会要求你设置 **root 密码**，**请务必记下来！**
   - 建议用简单密码：`5201314qinjia`（本项目的 `.env` 默认使用这个密码）
   - 如果用其他密码，记得去 `.env` 文件中修改
5. 端口保持默认 **3306**
6. 安装完成后，打开命令提示符验证：

```bash
mysql -u root -p
```

输入密码后，看到 `mysql>` 提示符即成功。输入 `exit` 退出。

#### ▶ Redis（约 3 分钟）

1. 打开 https://github.com/tporadowski/redis/releases
2. 下载 **Redis-x64-xxx.msi** 文件
3. 双击安装，勾选 **"添加到 PATH 环境变量"**
4. 启动 Redis：
   - 方式一：双击 `redis-server.exe`
   - 方式二：命令行输入 `redis-server`
5. 验证（**新开一个**命令提示符窗口）：

```bash
redis-cli ping
```

返回 `PONG` 即成功。

#### ▶ Docker Desktop（约 10 分钟）

1. 打开 https://www.docker.com/products/docker-desktop/
2. 下载 **Docker Desktop for Windows**
3. 双击安装，安装完成后**需要重启电脑**
4. 重启后启动 Docker Desktop（桌面图标）
5. 等待左下角变成绿色 **Running**
6. 验证（打开命令提示符）：

```bash
docker ps
```

不报错即成功。如果提示 "WSL 2 安装不完整"，请执行：

```bash
wsl --install
```

然后重启电脑。

#### ▶ Nacos（约 5 分钟）

1. 打开 https://github.com/alibaba/nacos/releases
2. 找到最新版，下载 **nacos-server-2.x.x.zip**（不要下载 with-src 版本）
3. 解压到 `D:\nacos`（**建议这个路径**）
4. 打开命令提示符，进入 Nacos 目录：

```bash
cd D:\nacos\bin
startup.cmd -m standalone
```

5. 等待几秒后，打开浏览器访问 http://localhost:8848/nacos
6. 看到 Nacos 登录页即成功（默认账号：`nacos` / `nacos`）

#### ▶ Git（约 2 分钟）

1. 打开 https://git-scm.com/
2. 下载 Windows 版本
3. 双击安装，一路"下一步"
4. 验证：

```bash
git --version
```

---

## 第二章：下载项目代码

1. 打开命令提示符（`Win + R` → `cmd` → 回车）
2. 进入你想放代码的目录（比如 D 盘）：

```bash
D:
cd D:\QQ\download
```

3. 克隆项目（下载代码）：

```bash
git clone https://github.com/JinZheng-Max/OAmanagement.git
```

4. 进入项目目录：

```bash
cd OAmanagement
```

> 💡 **小提示**：如果下载慢，可以关掉命令行重新开，用手机热点试试。

---

## 第三章：配置环境

### 3.1 配置 .env 文件

项目根目录下有一个 `.env` 文件，用**记事本**打开，确认以下配置：

```ini
MYSQL_PASSWORD=5201314qinjia
# ↑ 这是 MySQL 密码，如果你安装 MySQL 时设置了其他密码，改成你的

JWT_SECRET=vRF4pOju7IYF/T0Jmuc60nEB6ojFL4TSgqV64zh1RyuBx5UyYTgsCVVB1TLlpF4Y
# ↑ 这个不用改

DEEPSEEK_API_KEY=你的DeepSeek_API_Key
# ↑ DeepSeek AI 的 API Key，用于 AI 问答功能（需要去 https://platform.deepseek.com 申请）

NACOS_ENABLED=false
# ↑ 保持 false
```

> ⚠️ **重要**：`.env` 文件不要删、不要改格式、不要提交到 git。

### 3.2 创建数据库并导入表结构

1. 登录 MySQL：

```bash
mysql -u root -p
```

2. 输入你的 MySQL 密码

3. 出现 `mysql>` 提示符后，复制粘贴以下命令，**一次复制一行**执行：

```sql
CREATE DATABASE IF NOT EXISTS oa_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oa_db;
SOURCE D:/QQ/download/OAmanagement/sql/data.sql;
```

执行成功后应该会显示多条 `Query OK` 信息。最后输入 `exit` 退出。

4. 验证是否成功：

```bash
mysql -u root -p oa_db -e "SHOW TABLES;"
```

应该看到 13 张表，包括 `sys_user`、`oa_employee`、`oa_leave` 等。

> 💡 **这条命令一次性完成**：创建数据库 → 建表 → 插入初始数据（管理员账号、部门、员工）

### 3.3 添加 hosts（如果遇到 Nacos 连不上的问题）

用**管理员身份**打开记事本，编辑 `C:\Windows\System32\drivers\etc\hosts`，确保末尾有：

```
127.0.0.1 localhost
```

---

## 第四章：启动基础设施

> ⚠️ 以下服务需要**全部启动成功后**才能进入第五章。
> 每个服务启动后，**不要关掉对应的窗口**。

### 4.1 启动 Nacos

```bash
cd D:\nacos\bin
startup.cmd -m standalone
```

**等待时间**：约 10 秒
**验证方法**：打开浏览器访问 http://localhost:8848/nacos ，能看到 Nacos 登录页
**登录账号**：`nacos` / `nacos`（登录不是必须的，能打开页面就行）

### 4.2 启动 Docker 容器（ChromaDB + Embedding）

```bash
cd D:\QQ\download\OAmanagement
docker compose up -d
```

**等待时间**：首次运行需要下载镜像，约 3~10 分钟（看网速）。后续启动约 10 秒。

**验证方法**：

```bash
docker ps
```

应该看到以下两个容器状态为 `Up`：

| 容器名 | 端口 | 用途 |
|:-------|:----:|:-----|
| `oa-chroma` | 8000 | 向量数据库（存AI知识） |
| `oa-embedding` | 8080 | 文本转向量服务 |

### 4.3 启动 Redis

打开命令提示符，执行：

```bash
redis-server
```

保持窗口不关。然后**新开一个**命令提示符验证：

```bash
redis-cli ping
```

返回 `PONG` 即成功。

> 💡 如果 `redis-server` 提示找不到命令，去 Redis 安装目录双击 `redis-server.exe`

---

## 第五章：启动后端服务

> ⚠️ **必须严格按照顺序启动！**
> 每个服务启动需要几十秒，请耐心等待。

打开**三个**新的命令提示符窗口（编号 1、2、3），分别启动三个服务。

### 5.1 identity-service（认证 + 部门 + 员工 + 请假 + 工作流）

**窗口 1** 执行：

```bash
cd D:\QQ\download\OAmanagement
mvnw spring-boot:run -pl identity-service -Dmaven.test.skip=true
```

**等待标志**：看到以下信息（约 30~50 秒）：

```
Started IdentityServiceApplication in 16 seconds
```

**端口**：8851
**这个服务负责**：用户登录、部门管理、员工管理、请假审批、Flowable 工作流引擎

### 5.2 oa-content-service（公告制度 + AI 知识库）

**窗口 2** 执行：

```bash
cd D:\QQ\download\OAmanagement
mvnw spring-boot:run -pl oa-content-service -Dmaven.test.skip=true -Dspring-boot.run.jvmArguments="-Xmx1g -Xms256m"
```

> 最后的 `-Xmx1g` 表示给这个服务分配 1GB 内存，因为 AI 解析文档需要较多内存。

**等待标志**：

```
Started OaContentServiceApplication in 20 seconds
```

**端口**：8853
**这个服务负责**：公告制度发布、全文检索、AI 知识库问答（RAG）、文档解析与向量化

### 5.3 oa-gateway（API 网关）

**窗口 3** 执行：

```bash
cd D:\QQ\download\OAmanagement
mvnw spring-boot:run -pl oa-gateway -Dmaven.test.skip=true
```

**等待标志**：

```
Started OaGatewayApplication in 13 seconds
```

**端口**：10002
**这个服务负责**：统一 API 入口，前端所有请求都先经过网关再转发到各后端服务

---

## 第六章：启动前端

打开**第四个**命令提示符窗口，执行：

```bash
cd D:\QQ\download\OAmanagement\newFrontEnd
npm install        # 第一次运行需要安装依赖（仅安装一次）
npm run dev        # 启动开发服务器
```

**等待标志**：

```
VITE v8.x.x  ready in xxx ms
Local:   http://localhost:5173/
```

> 💡 前端使用的是 `newFrontEnd` 目录，旧版 `oa-web` 已不再使用。

---

## 第七章：登录系统

打开浏览器，访问 **http://localhost:5173**

### 默认账号

| 账号 | 密码 | 角色 | 说明 |
|:-----|:-----|:-----|:-----|
| `admin` | `Admin@123456` | 超级管理员 | 拥有全部权限，管理整个系统 |
| `zhangsan` | `emp123` | 部门经理 | 可以审批本部门请假 |
| `lisi` | `emp123` | 普通员工 | 只能查看和编辑自己的信息 |

### 登录后可以做些什么？

| 菜单 | 位置 | 说明 |
|:-----|:------|:------|
| 📊 **仪表盘** | 首页 | 公司概览、今日考勤、待办审批、快捷入口 |
| 🏢 **组织管理** | 左侧菜单 | 管理部门（添加/编辑/停用）+ 管理员工 |
| 📋 **请假审批** | 左侧菜单 | 提交请假 → 部门经理审批 →（超3天转总管理员二审）|
| 📅 **考勤管理** | 左侧菜单 | 签到/签退打卡、查看考勤记录、管理员补录 |
| 📢 **公告制度** | 左侧菜单 | 发布公告/制度、全文检索、草稿管理 |
| 🤖 **AI 智能助手** | 左侧菜单 | 基于企业知识库的智能问答（需要先上传文档） |

### 快速体验流程

```
1. 先用 admin 登录 → 进入"组织管理" → 添加几个员工
2. 切换到 lisi 账号 → 进入"请假审批" → 提交请假申请
3. 切换到 zhangsan 账号 → 查看待办任务 → 审批该请假
4. 如果请假天数>3天 → 切回 admin → 二审
5. 进入"AI 智能助手" → 如果已导入知识库即可提问
```

---

## 第八章：导入知识库文档（让AI能回答问题）

> 想让 AI 能回答公司制度、流程等问题，需要先上传企业知识文档。

### 方式一：通过页面手动上传（推荐新手）

1. 登录后点击左侧菜单 → **AI 智能助手**
2. 切换到"知识文档管理"标签页
3. 点击右上角 **上传文档** 按钮
4. 选择你的 docx/pdf 文件（支持 .doc .docx .pdf）
5. 等待约 1 分钟，页面显示文档状态变为 **"已就绪"**
6. 回到"AI 问答"标签页，输入问题测试

### 方式二：批量导入（大量文档时使用）

1. 将你的所有 docx/pdf 文件放入 `D:\QQ\download\知识库word\` 目录
2. 打开命令提示符，执行：

```bash
curl -X POST http://localhost:8853/api/ai/batch-import
```

3. 等待约 2~3 分钟（文档越多越久）
4. 打开页面验证索引状态

### 验证 AI 问答

导入完成后，在 AI 问答页面输入：

```
公司的考勤制度是什么？
```

如果能正确回答并标注来源 [S1]、[S2] 等，说明一切正常。

---

## 第九章：常见问题与解决方法

### Q1: 端口被占用

**现象**：启动时提示 `Address already in use`。

**解决方法**：

```bash
# 查看哪个进程占用了端口（比如 8853）
netstat -ano | findstr 8853
# 输出类似：TCP 0.0.0.0:8853  LISTENING  12345
# 最后的 12345 是进程 PID

# 结束该进程
taskkill /F /PID 12345
```

### Q2: 连不上 MySQL

**现象**：启动时报 `Cannot create PoolDataSource` 或 `Access denied`。

**检查**：

```bash
# 1. MySQL 是否在运行？
tasklist | findstr mysql

# 2. 如果没运行，启动 MySQL 服务
net start MySQL

# 3. 验证登录
mysql -u root -p
```

### Q3: 连不上 Redis

**现象**：启动时报 `RedisException: Connection refused`。

**解决**：去 Redis 安装目录双击 `redis-server.exe`，或用命令 `redis-server`

### Q4: Nacos 启动失败

**现象**：执行 `startup.cmd` 后窗口闪退。

**解决**：检查 `JAVA_HOME` 环境变量是否设置。打开"系统属性 → 高级 → 环境变量"，新建 `JAVA_HOME` = `C:\Program Files\Java\jdk-21`。

### Q5: 前端白屏

**解决**：

```bash
cd newFrontEnd
npm install
npm run dev
```

### Q6: AI 回答"知识库中暂无相关内容"

**原因**：知识库中没有文档，或文档还没索引完。
**解决**：上传文档（见第八章），等待状态变为"已就绪"。

### Q7: 请假审批流程不生效

**解决**：手动添加 `proc_inst_id` 字段：

```sql
ALTER TABLE oa_leave ADD COLUMN proc_inst_id VARCHAR(64) NULL COMMENT '流程实例ID';
```

### Q8: Maven 下载依赖慢

**解决**：配置阿里云镜像。在 `C:\Users\你的用户名\.m2\settings.xml` 中添加阿里云 mirror。

### Q9: 编译时报 OutOfMemoryError

**原因**：内存不足。
**解决**：编译时加参数 `-Dspring-boot.run.jvmArguments="-Xmx1g"`

---

## 附录一：启动速查表

| 序号 | 服务 | 命令 | 启动时间 | 验证 |
|:----:|:-----|:-----|:--------:|:-----|
| 0 | Nacos | `startup.cmd -m standalone` | 10秒 | 浏览器 `localhost:8848/nacos` |
| 1 | Docker | `docker compose up -d` | 10秒 | `docker ps` |
| 2 | Redis | `redis-server` | 3秒 | `redis-cli ping` → PONG |
| 3 | identity | `mvnw spring-boot:run -pl identity-service -DskipTests` | 40秒 | `localhost:8851/actuator/health` |
| 4 | content | `mvnw spring-boot:run -pl oa-content-service -DskipTests -Dspring-boot.run.jvmArguments="-Xmx1g"` | 20秒 | `localhost:8853/api/ai/health` |
| 5 | gateway | `mvnw spring-boot:run -pl oa-gateway -DskipTests` | 15秒 | `localhost:10002/actuator/health` |
| 6 | 前端 | `cd newFrontEnd && npm run dev` | 5秒 | `localhost:5173` |

---

## 附录二：端口一览

| 服务 | 端口 | 用途 |
|:-----|:----:|:------|
| 前端 | 5173 | 网页开发服务器 |
| 网关 | 10002 | API 统一入口 |
| identity-service | 8851 | 用户/部门/员工/请假 |
| content-service | 8853 | 公告/AI/知识库 |
| attendance-service | 8849 | 考勤管理 |
| Nacos | 8848 | 服务注册中心 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| ChromaDB | 8000 | AI 向量库 |
| Embedding | 8080 | 文本向量化 |

---

## 附录三：系统架构图

```
浏览器 (localhost:5173)
  │
  ▼
网关 (localhost:10002)
  │
  ├── identity-service (8851) ← MySQL + Redis + Nacos
  │     └── Flowable 工作流引擎 (ACT_* 表)
  │
  ├── content-service (8853) ← MySQL + ChromaDB(8000) + Embedding(8080)
  │     └── AI 问答 / RAG / OSS 存储
  │
  └── attendance-service (8849) ← MySQL
```

> 💡 **全部服务启动完成后，只需打开 http://localhost:5173 即可使用，无需记住各个服务的端口号。**
