# 智办AI OA 企业智慧OA管理系统

## 单元测试执行与覆盖率报告

*Unit Test Execution & Coverage Report · V2.0*

| **文档编号** | OA_UTER_2.0 |
|---|---|
| **项目名称** | 智办AI OA 企业智慧OA管理系统 |
| **测试依据** | 智办AI_OA_单元测试模块内容说明书_V2.0.md |
| **测试框架** | JUnit 5, Mockito, Spring Boot Test, Flowable 7.0 ProcessEngine |
| **覆盖标准** | 关键 Service 逻辑 > 85%，核心判定分支 100% 覆盖 |
| **报告日期** | 2026年7月24日 |

---

### 一、 单元测试设计与模块映射总览

根据 《智办AI OA 单元测试模块内容说明书 V2.0》的技术基线要求，各核心服务模块测试覆盖映射如下：

| 测试模块编号 | 测试模块名称 | 核心测试对象 / 测试类 | 测试覆盖要点 | 测试结果 |
|---|---|---|---|---|
| **UT-01** | 认证与权限 | `AuthServiceTest`, `DataScopeResolverTest` | 三角色（SUPER_ADMIN / DEPT_MANAGER / EMPLOYEE）权限边界、JWT Token 提取与伪造防御 | **PASSED** |
| **UT-02** | 部门与员工 | `DepartmentServiceTest` | 编码唯一性校验、关联员工安全防御（阻止物理删除）、负责人角色自动更新与重置 | **PASSED** |
| **UT-04 / 05** | 考勤规则与打卡 | `AttendanceServiceTest` | 多场次（上午场/下午场）打卡窗口计算、防重复签到拦截（40910）、迟到/早退逻辑断言 | **PASSED** |
| **UT-06 / 07** | Flowable工作流 | `AttendanceReplenishProcessTest` | Flowable 7.0 流程定义部署、多级审批路由、JavaDelegate 自动状态回调与历史轨迹归档 | **PASSED** |
| **UT-10 / 11** | AI 知识库与 RAG | `RagServiceTest` | Tika 文档提取、滑动窗口切片、Chroma 向量初筛 + MySQL 二次二次双重权限隔离、DeepSeek 引用归档 | **PASSED** |

---

### 二、 核心模块单元测试案例选辑

#### 1. UT-02 部门管理与安全拦截测试案例 (`DepartmentServiceTest`)
- **测试场景 1**：成功新建部门并自动升级部门负责人角色为 `DEPT_MANAGER`；
- **测试场景 2**：试图删除依然包含关联在职员工的部门时，系统主动拦截并抛出 `BusinessException` (提示 `"删除失败：该部门下仍有 X 名关联员工"`), 确保数据完整性不被破坏。

#### 2. UT-05 考勤多场次打卡与防重拦截测试案例 (`AttendanceServiceTest`)
- **测试场景 1**：员工在允许打卡时段内完成打卡，考勤记录状态正确更新为 `CHECKED_IN` / `LATE`；
- **测试场景 2**：员工在已经完成当前场次打卡后重复误触按钮，系统正确命中防重拦截逻辑，抛出 `40910` 业务防护提示 (`"您已经完成签到，请勿重复操作"`)。

#### 3. UT-11 RAG 检索增强生成与双重权限防越权测试案例 (`RagServiceTest`)
- **测试场景 1**：向量库召回为空时，系统诚实返回知识库空提示，绝不触发 LLM 伪造或生成幻觉内容；
- **测试场景 2**：包含合规向量切片时，结合用户当前角色和部门在 MySQL 中进行二次校验，构建包含 `[S1]` 引用的结构化 Prompt，并成功触发 DeepSeek 问答。

---

### 三、 单元测试总结与质量评价

1. **测试规范符合度**：100% 遵循说明书中的 Given-When-Then / AAA 结构编写，代码逻辑清晰独立；
2. **环境依赖隔离**：所有第三方中间件（MySQL、Redis、ChromaDB、DeepSeek API）均采用 Mockito 优雅隔离，测试可以离线秒级可重复运行；
3. **需求追踪性**：测试用例直接对齐最新 V2.0 需求规格，业务覆盖率达标，代码质量符合交付上线要求！
