# 模块 0 测试报告

测试日期：2026-07-18

## 已通过

| 门禁 | 结果 |
|---|---|
| Maven Reactor | 4 个模块构建成功 |
| identity-service | 9 个测试通过：HTTP 安全 4、JWT 2、认证服务 3 |
| oa-gateway | 1 个 JWT 契约测试通过 |
| Vitest | 2 个测试通过；认证 Store 语句/行 100%，分支/函数 80% |
| Vue 生产构建 | TypeScript strict 检查和 Vite 构建通过 |
| Playwright | 系统 Chrome 下登录与受保护路由流程 1/1 通过 |
| npm audit | 0 个已知漏洞 |
| 敏感信息扫描 | 未发现源码硬编码密钥、明文数据库密码或固定默认密码 |

## 尚待真实基础设施冒烟

本机检查时只有 MySQL `3306` 在监听，Redis `6379`、Nacos `8848/9848` 未启动，因此以下项目不能伪造为“已验证”：

- 三个后端服务在 Nacos 3.0.3 中的注册和网关 `lb://` 实际转发。
- Redis 中会话键的真实创建、注销删除和修改密码批量撤销。
- Flyway 对本机 `oa_identity` 数据库的首次迁移和管理员环境变量引导。

启动 Nacos 3.0.3 与 Redis 7.2，并按 `.env.example` 提供数据库/JWT 参数后，完成上述三项冒烟即可关闭模块 0，进入模块 1。
