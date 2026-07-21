# 模块 0：工程基线与认证加固

## 当前服务

- `oa-gateway`：10001，统一路由、TraceId、JWT 与 Redis 会话前置校验。
- `identity-service`：8850，登录、注销、当前用户和修改密码。
- `oa-attendance-service`：8849，本模块只完成服务注册骨架。
- `oa-web`：Vue 3 + TypeScript 登录和权限路由骨架。

## 启动准备

1. 安装 JDK 21+、Node 22+、MySQL 8、Redis 7.2 和 Nacos Server 3.0.3。
2. 复制 `.env.example` 并在 IDE/终端中设置变量；不得提交真实密码或 JWT 密钥。
3. 创建 `oa_identity` 数据库。identity-service 启动时由 Flyway 建表。
4. 首次启动可设置 `OA_BOOTSTRAP_ADMIN_USERNAME` 与 `OA_BOOTSTRAP_ADMIN_PASSWORD`；账号存在后删除这两个变量。
5. 启动 Nacos、Redis、identity-service、oa-attendance-service、oa-gateway，最后在 `oa-web` 执行 `npm run dev`。

## 当前接口

- `POST /api/auth/login`：公开；成功后在 Redis 创建 30 分钟会话。
- `POST /api/auth/logout`：撤销当前 JTI，会话立即失效。
- `GET /api/auth/me`：返回 DTO，不包含密码摘要。
- `PUT /api/auth/password`：校验原密码并撤销该用户全部会话。
- `/api/auth/register` 已删除；后续账号仅由管理员在模块 1 开通。

统一响应为 `ApiResult<T>{code,message,data,traceId}`，HTTP 状态码与协议结果保持一致。

## 验证命令

```powershell
.\mvnw.cmd -B test
cd oa-web
npm ci
npm test
npm run build
npm run test:e2e
```
