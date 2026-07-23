# OA 智能管理系统 — 完整启动方案

> 本文档面向**计算机新手**，手把手教你从零搭建并启动整个项目。
> 只需按顺序操作，每个步骤都有详细说明。

---

## 📦 需要安装的软件

| 软件 | 版本要求 | 下载地址 | 用途 |
|:-----|:---------|:---------|:-----|
| **JDK 21** | ≥ 21 | https://www.oracle.com/java/technologies/downloads/ | 运行后端 Java 服务 |
| **Node.js** | ≥ 18 | https://nodejs.org/ | 运行前端网页 |
| **MySQL** | ≥ 8.0 | https://dev.mysql.com/downloads/installer/ | 存储数据 |
| **Redis** | ≥ 7.0 | https://github.com/tporadowski/redis/releases | 缓存、登录会话 |
| **Docker Desktop** | 最新版 | https://www.docker.com/products/docker-desktop/ | 运行 AI 向量库 |
| **Nacos** | 2.x | https://github.com/alibaba/nacos/releases | 服务注册与配置 |
| **Git** | 最新版 | https://git-scm.com/ | 下载代码 |

---

## 第一步：安装环境（电脑小白版）

### 1.1 安装 JDK 21

1. 打开 https://www.oracle.com/java/technologies/downloads/
2. 选择 **Windows → x64 Installer**，下载 `.exe` 文件
3. 双击安装，一路点"下一步"
4. **记住安装路径**，例如 `C:\Program Files\Java\jdk-21`
5. 验证：打开命令提示符（按 `Win+R`，输入 `cmd`，回车），输入：
   ```
   java -version
   ```
   看到 `openjdk version "21"` 即成功

### 1.2 安装 Node.js

1. 打开 https://nodejs.org/
2. 下载 **LTS 版本**（左侧按钮）
3. 双击安装，一路"下一步"
4. 验证：
   ```
   node -v
   npm -v
   ```
   看到版本号即成功

### 1.3 安装 MySQL

1. 打开 https://dev.mysql.com/downloads/installer/
2. 下载 **MySQL Installer for Windows**（约 400MB）
3. 安装类型选 **Server only**
4. 设置 **root 密码**（务必记住！）
5. 保持默认端口 **3306**
6. 默认字符集选 **UTF-8**
7. 验证：
   ```
   mysql -u root -p
   ```
   输入密码后能进入 `mysql>` 提示符即成功

### 1.4 安装 Redis

1. 下载 https://github.com/tporadowski/redis/releases （选择 `.msi` 或 `.zip`）
2. 如果是 `.msi`：双击安装，勾选"添加到 PATH"
3. 如果是 `.zip`：解压到 `D:\software\Redis`
4. 启动 Redis（两种方式任选）：
   - **方式一**：双击 `redis-server.exe`
   - **方式二**：命令行运行 `redis-server`
5. 验证（新开一个命令行）：
   ```
   redis-cli ping
   ```
   返回 `PONG` 即成功

### 1.5 安装 Docker Desktop

1. 打开 https://www.docker.com/products/docker-desktop/
2. 下载 **Docker Desktop for Windows**
3. 双击安装，需要 **重启电脑**
4. 启动 Docker Desktop（桌面图标）
5. 等待左下角显示绿色 **Running**
6. 验证（打开命令行）：
   ```
   docker ps
   ```
   不报错即成功

### 1.6 安装 Nacos

1. 打开 https://github.com/alibaba/nacos/releases
2. 下载 **nacos-server-2.x.x.zip**（最新版）
3. 解压到 `D:\nacos`
4. 启动 Nacos（进入 `D:\nacos\bin` 目录）：
   ```
   startup.cmd -m standalone
   ```
5. 等待几秒，打开浏览器访问 http://localhost:8848/nacos
6. 默认登录：用户名 `nacos`，密码 `nacos`
7. 验证：看到 Nacos 控制台即成功

### 1.7 安装 Git

1. 打开 https://git-scm.com/
2. 下载 Windows 版本
3. 双击安装，一路"下一步"
4. 验证：
   ```
   git --version
   ```

---

## 第二步：下载项目代码

1. 打开命令行（`Win+R` → `cmd`）
2. 进入你想放代码的目录，例如：
   ```
   cd D:\QQ\download
   ```
3. 克隆项目：
   ```
   git clone https://github.com/JinZheng-Max/OAmanagement.git
   ```
4. 进入项目目录：
   ```
   cd OAmanagement
   ```

> 💡 如果你已经拿到本项目代码文件夹，直接跳到下一步

---

## 第三步：配置环境

### 3.1 配置 .env 文件

项目根目录下已有 `.env` 文件，检查并修改以下内容（用**记事本**打开）：

```
# ─── 数据库 ───
MYSQL_URL=jdbc:mysql://localhost:3306/oa_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
MYSQL_USERNAME=root
MYSQL_PASSWORD=你的MySQL密码       ← 改为你自己的密码

# ─── Redis ───
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=                    ← 没设密码就留空

# ─── JWT密钥 ───
JWT_SECRET=vRF4pOju7IYF/T0Jmuc60nEB6ojFL4TSgqV64zh1RyuBx5UyYTgsCVVB1TLlpF4Y

# ─── DeepSeek AI密钥 ───
DEEPSEEK_API_KEY=你的API密钥       ← 如果需要AI问答，填写你的DeepSeek API Key

# ─── Nacos ───
NACOS_ENABLED=true
```

### 3.2 创建数据库

1. 登录 MySQL：
   ```
   mysql -u root -p
   ```
2. 输入密码进入 `mysql>` 提示符
3. 创建数据库：
   ```sql
   CREATE DATABASE IF NOT EXISTS oa_db DEFAULT CHARSET utf8mb4;
   USE oa_db;
   ```
4. 执行初始化脚本（不要关mysql，继续在 `mysql>` 中执行）：
   ```
   source D:\QQ\download\OAmanagement\sql\更新.sql;
   ```
   或者打开 `sql\更新.sql` 文件，把全部内容复制粘贴到 mysql 命令行中执行

5. 验证：
   ```sql
   SHOW TABLES;
   ```
   应该能看到 `sys_user`、`oa_ai_source`、`oa_department` 等 20+ 张表

### 3.3 添加到 hosts（如果 Nacos 报错）

用**管理员身份**打开记事本，编辑 `C:\Windows\System32\drivers\etc\hosts`，在末尾添加：

```
127.0.0.1 localhost
```

---

## 第四步：启动基础设施

### 4.1 启动 Docker 容器（AI 向量库 + 文本向量化）

打开命令行，进入项目目录：

```
cd D:\QQ\download\OAmanagement
docker compose up -d
```

这会启动两个服务：
- **ChromaDB** → http://localhost:8000（向量数据库）
- **Embedding** → http://localhost:8080（文本转向量）

验证：
```
docker ps
```
应该看到 `oa-chroma` 和 `oa-embedding` 两个容器状态为 `Up`

### 4.2 确保 Redis 在运行

```
redis-cli ping
```
返回 `PONG` 就行。没有的话，双击 `redis-server.exe` 启动

### 4.3 确保 Nacos 在运行

浏览器打开 http://localhost:8848/nacos ，能看到登录页就行

---

## 第五步：启动后端服务（按顺序）

> 💡 每个服务会打开一个新命令行窗口，不要关掉它们

### 5.1 启动 identity-service（身份认证服务）

打开**第一个命令行**，执行：

```
cd D:\QQ\download\OAmanagement
mvnw spring-boot:run -pl identity-service -Dmaven.test.skip=true
```

等待看到 `Started IdentityServiceApplication`（约 30~50 秒），出现这个提示就说明启动成功了。

端口：**8851**

### 5.2 启动 oa-content-service（AI 内容服务）

打开**第二个命令行**，执行：

```
cd D:\QQ\download\OAmanagement
mvnw spring-boot:run -pl oa-content-service -Dmaven.test.skip=true -Dspring-boot.run.jvmArguments="-Xmx1g -Xms256m"
```

等待看到 `Started OaContentServiceApplication`（约 20~30 秒）。

端口：**8853**

### 5.3 启动 oa-gateway（网关服务）

打开**第三个命令行**，执行：

```
cd D:\QQ\download\OAmanagement
mvnw spring-boot:run -pl oa-gateway -Dmaven.test.skip=true
```

等待看到 `Started OaGatewayApplication`（约 15~20 秒）。

端口：**10002**

---

## 第六步：启动前端

打开**第四个命令行**，执行：

```
cd D:\QQ\download\OAmanagement\oa-web
npm install        ← 第一次运行需要安装依赖（仅一次）
npm run dev
```

等待看到 `http://localhost:5173` 即成功。

---

## 第七步：打开系统

1. 打开浏览器，访问 **http://localhost:5173**
2. 登录账号：
   - **用户名**：`admin`
   - **密码**：`Admin@123456`
3. 进入系统后，左侧菜单可以看到：
   - **🤖 AI 智能问答** — 基于知识库的 AI 问答
   - **📚 知识文档管理** — 上传和管理企业知识文档

---

## 第八步：（可选）导入企业知识库

1. 在 `D:\QQ\download\OAservice\知识库word` 目录下放入你的 docx/pdf 文件
2. 启动系统后，打开 **知识文档管理** 页面
3. 点击 **上传文档** 按钮，选择文件上传
4. 系统会自动：
   - 解析文档内容
   - 进行文本分片
   - 生成向量并存入 ChromaDB
   - 等待约 1 分钟后，状态变为"已就绪"
5. 回到 **AI 智能问答** 页面，就可以提问了

### 快速批量导入

如果你有很多文档，可以直接调用批量导入接口（在命令行执行）：

```bash
curl -X POST http://localhost:8853/api/ai/batch-import
```

这会将 `D:/QQ/download/OA-service/知识库word` 目录下的所有文档批量导入。

---

## 📊 系统架构图

```
浏览器 (http://localhost:5173)
  │
  ▼
Vue 前端 → 网关 (10002) → identity-service (8851) ← MySQL ← Redis
                │
                ├→ oa-content-service (8853) ← ChromaDB (8000)
                │                              ← Embedding (8080)
                │                              ← DeepSeek API
                │
                └→ oa-attendance-service (8849)
```

---

## ❓ 常见问题

### Q1: 启动时提示"端口被占用"

每个服务有不同的端口号，检查哪个端口被占用了：

```
netstat -ano | findstr 8853
```

如果端口被占用，可以修改对应 `application.yml` 中的 `server.port`。

### Q2: 启动时提示"连不上数据库"

检查：
1. MySQL 是否已启动（任务管理器看是否有 `mysqld.exe`）
2. `.env` 中的 `MYSQL_PASSWORD` 是否填对了
3. 数据库 `oa_db` 是否已创建

### Q3: 启动时提示"连不上 Redis"

双击 `redis-server.exe` 启动 Redis，或者检查 Redis 是否已在运行：
```
redis-cli ping
```

### Q4: 前端页面空白或报错

1. 先确认 `npm install` 已执行过
2. 确认命令行的 `npm run dev` 没有报错
3. 确认后端服务都已启动（5.1~5.3 的执行窗口没有异常退出）
4. 按 `F12` 打开浏览器开发者工具 → `Console` 标签，看红色报错信息

### Q5: AI 问答返回"知识库中暂无相关内容"

1. 检查 **知识文档管理** 页面是否有文档且状态为"已就绪"
2. 没有文档的话，先上传文档（见第八步）
3. 文档状态不对的话，点击"重索引"按钮重新处理

### Q6: 文档索引失败

在 **知识文档管理** 页面，如果文档状态显示"异常"：

1. 点击文档后的**重索引**按钮
2. 等待 1~2 分钟
3. 刷新页面查看状态

### Q7: 想重置所有数据

如果你想清空所有数据重新开始：

```sql
-- 在 MySQL 中执行
USE oa_db;
DELETE FROM oa_ai_source_chunk;
DELETE FROM oa_ai_source;
DELETE FROM oa_ai_citation;
DELETE FROM oa_ai_session;
```

然后重新上传文档即可。

---

## 🔄 服务启动汇总（速查表）

| 序号 | 服务 | 命令 | 端口 | 启动耗时 |
|:----:|:-----|:-----|:----:|:--------:|
| 0 | Docker 容器 | `docker compose up -d` | 8000,8080 | 30秒 |
| 1 | identity-service | `mvnw spring-boot:run -pl identity-service -DskipTests` | 8851 | 40秒 |
| 2 | oa-content-service | `mvnw spring-boot:run -pl oa-content-service -DskipTests -Dspring-boot.run.jvmArguments="-Xmx1g"` | 8853 | 20秒 |
| 3 | oa-gateway | `mvnw spring-boot:run -pl oa-gateway -DskipTests` | 10002 | 15秒 |
| 4 | 前端 | `cd oa-web && npm run dev` | 5173 | 5秒 |

> ⚠️ **必须按顺序启动**，前一个服务启动成功后再启动下一个

---

## 📝 技术说明

| 功能模块 | 技术实现 |
|:---------|:---------|
| 用户认证 | JWT Token + Redis 会话 |
| 服务注册 | Nacos 注册中心 |
| 数据库 | MySQL + Flyway 迁移 |
| 缓存 | Redis |
| 服务间调用 | OpenFeign |
| AI 向量库 | ChromaDB（Docker） |
| 文本向量化 | HuggingFace Text Embeddings Inference（Docker） |
| AI 对话 | DeepSeek API |
| 文件解析 | Apache Tika |
| 前端框架 | Vue 3 + Element Plus |
| 构建工具 | Maven + Vite |
