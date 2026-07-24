# 智办AI OA 企业智慧OA管理系统

## 单元测试模块内容说明书

*Unit Test Module Specification · V2.0*

| **文档编号** | OA_UTMS_2.0                                                                                                        |
|--------------|--------------------------------------------------------------------------------------------------------------------|

| **项目名称** | 智办AI OA 企业智慧OA管理系统                                                                                       |

| **测试类型** | 后端服务单元测试 + Flowable流程单元测试 + 前端组件单元测试                                                         |

| **技术基线** | Vue 3、Spring Boot 3.x、JUnit 5、Mockito、Flowable 7.0、MySQL、Redis、Elasticsearch、Tika、TEI、ChromaDB、DeepSeek |

| **编制依据** | 需求规格说明书 V2.0、测试用例 V1.0及最新业务变更                                                                   |

| **版本日期** | V2.0 · 2026年7月24日                                                                                               |

*新大陆软件开发实训 · 团队综合项目交付文档*

# 文档控制

本文件用于定义智办AI OA项目的单元测试范围、测试模块、测试对象、依赖隔离方式、关键断言、覆盖率要求及测试代码组织规范。其定位是连接需求规格、源代码实现和测试用例表，指导开发成员按统一口径编写可重复执行、可定位缺陷、可追踪需求的单元测试。

| **控制项** | **说明**                                                                                        |
|------------|-------------------------------------------------------------------------------------------------|
| 文档状态   | 正式测试设计基线（V2.0）                                                                        |
| 适用阶段   | 编码、自测、合并请求检查、回归测试和答辩前质量确认                                              |
| 主要读者   | 项目组长、后端开发、前端开发、测试与文档成员                                                    |
| 测试重点   | 权限边界、部门数据范围、考勤多时段规则、Flowable审批路由、公告ES检索、RAG知识权限隔离与安全降级 |
| 变更原则   | 需求、数据库、BPMN流程或接口变化时，必须同步更新本说明书与测试代码                              |

## 版本历史

| **版本** | **日期**      | **编制人** | **变更说明**                                                                                                 |
|----------|---------------|------------|--------------------------------------------------------------------------------------------------------------|
| V1.0     | 2026年7月20日 | 项目组     | 依据初始功能模块和测试用例表建立单元测试框架                                                                 |
| V2.0     | 2026年7月24日 | 项目组     | 按最新需求增加部门独立定时考勤、Flowable考勤补交与请假条件审批、ChromaDB RAG链路和三级权限隔离；统一角色命名 |

## 本次修订要点

- 角色口径统一为 SUPER_ADMIN（总负责人）、DEPT_MANAGER（部门经理）和 EMPLOYEE（普通员工），不再使用笼统的“管理员”表述。
- 考勤补交和请假审批均由 Flowable 7.0 BPMN 流程驱动，单元测试必须验证流程实例、待办任务、条件网关、服务任务、撤回和历史轨迹。
- 考勤规则改为部门独立、多时间段、版本化配置；系统按各部门发布时间自动生成任务并固化规则快照。
- 公告全文检索仅使用 Elasticsearch；AI知识问答仅检索上传的 Word/PDF 知识文件，两条链路相互独立。
- AI链路统一为 Tika解析、TEI向量化、ChromaDB召回、MySQL二次鉴权、DeepSeek生成；原有RedisStack向量索引或公告content_id引用不再纳入测试基线。

# 目录

- **第一章 单元测试概述**
- **第二章 测试策略与技术规范**
- **第三章 测试模块总览**
- **第四章 后端业务单元测试模块**
- **第五章 Flowable工作流单元测试模块**
- **第六章 公告检索与AI智慧办公单元测试模块**
- **第七章 公共平台与前端单元测试模块**
- **第八章 测试数据、Mock与覆盖率要求**
- **第九章 测试执行、报告与缺陷处理**
- 附录A 需求追踪矩阵
- 附录B 推荐测试类与目录结构

# 第一章 单元测试概述

## 1.1 编写目的

单元测试用于验证最小可测试单元在隔离依赖后的业务正确性，包括Service方法、领域规则、权限判断器、流程监听器与Delegate、任务调度器、文件解析组件、检索与鉴权组件、Vue组合式函数和页面组件。测试应在不依赖完整部署环境的前提下快速执行，并能够准确指出失败发生在哪一项业务规则。

## 1.2 测试目标

- 确保三角色菜单、接口、部门范围和本人数据范围判断在代码级正确实现，任何客户端伪造字段均不能扩大权限。
- 确保部门多时段考勤规则、独立定时任务、真实IP校验、签到签退、迟到早退和缺勤结算逻辑正确。
- 确保考勤补交和请假审批严格由Flowable流程推进，业务状态、流程状态和审计记录保持一致。
- 确保公告发布、下架、可见范围、缓存和ES索引同步逻辑正确，搜索结果不泄露草稿或其他部门公告。
- 确保知识文件上传、解析、分片、向量化、Chroma元数据、MySQL二次鉴权、AI引用和安全拒答符合最新RAG方案。
- 确保公共异常、幂等、限流、缓存降级、操作日志和前端交互在异常条件下仍保持安全边界。

## 1.3 单元测试范围

| **范围**         | **说明**                                                                                                                                           |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| 纳入单元测试     | Service/Domain规则、权限判断、DTO校验、Mapper调用编排、调度器、Flowable监听器/Delegate、检索构造器、缓存Key生成、文件安全校验、Vue组件与组合式函数 |
| 通过Mock隔离     | MySQL Mapper、Redis、Nacos配置、Elasticsearch客户端、Tika解析器、TEI客户端、Chroma客户端、DeepSeek客户端、文件存储和跨微服务调用                   |
| 使用轻量引擎验证 | Flowable流程定义部署、条件网关路由、UserTask分配、ServiceTask执行、撤回和历史查询                                                                  |
| 不属于单元测试   | 真实中间件连通、Docker Compose部署、完整数据库索引、性能压测、浏览器兼容、端到端主流程；这些应进入集成测试或系统测试                               |

## 1.4 测试原则

> **1.** 一条测试方法只验证一个明确行为，采用 Given-When-Then 或 Arrange-Act-Assert 结构。
>
> **2.** 正常、异常、权限、边界和幂等至少各覆盖一种典型情况；关键流程分支必须全部覆盖。
>
> **3.** 不得通过直接修改数据库或Flowable ACT\_\*表制造流程结果，应通过公开Service或Flowable API推进。
>
> **4.** 测试数据必须显式声明角色、部门、业务状态和流程变量，避免依赖执行顺序或共享脏数据。
>
> **5.** 外部依赖失败时优先验证安全失败，不得因为Mock异常而默认放宽权限或伪造业务事实。

# 第二章 测试策略与技术规范

## 2.1 推荐测试技术栈

| **类别**     | **建议工具**                                                        | **主要用途**                                     |
|--------------|---------------------------------------------------------------------|--------------------------------------------------|
| 后端基础     | JUnit 5、Mockito、AssertJ                                           | Service、策略类、工具类和异常分支                |
| Spring Web层 | MockMvc / WebTestClient                                             | 参数校验、统一响应、401/403/409和序列化          |
| 持久层       | MyBatis Mapper Mock；必要时@DataMybatisTest                         | SQL条件构造、唯一约束和分页参数                  |
| 工作流       | Flowable ProcessEngine、RuntimeService、TaskService、HistoryService | BPMN部署、路由、任务分配、服务任务与历史         |
| 前端         | Vitest、Vue Test Utils、Pinia Testing                               | 组件渲染、表单校验、路由守卫、权限指令和请求状态 |
| 覆盖率       | JaCoCo、Vitest Coverage                                             | 行覆盖率、分支覆盖率和未覆盖代码定位             |

## 2.2 测试分层

| **层级**            | **测试对象**                                                | **执行特点**                             |
|---------------------|-------------------------------------------------------------|------------------------------------------|
| L1 纯函数/规则测试  | 时间区间、CIDR、状态机、权限范围、缓存Key、分片与元数据构造 | 不启动Spring上下文，毫秒级执行           |
| L2 Service单元测试  | Mock Mapper、Redis、外部客户端和当前用户上下文              | 验证调用顺序、事务分支和异常转换         |
| L3 Web层切片测试    | Controller、参数校验、Spring Security和统一异常处理         | 验证HTTP状态码、响应体和权限拦截         |
| L4 流程引擎轻量测试 | 部署BPMN并使用内存库推进流程                                | 验证流程定义与业务变量，不依赖完整微服务 |
| L5 前端组件测试     | Mock API、路由、Store和计时器                               | 验证组件状态和交互分支                   |

## 2.3 命名与代码规范

- 测试类命名：被测类名 + Test，例如 AuthServiceTest、AttendanceTaskPublisherTest、LeaveApprovalProcessTest。
- 测试方法命名建议：shouldExpectedBehavior_whenCondition，例如 shouldRouteToAdmin_whenEmployeeLeaveExceedsThreeDays。
- 使用 @Nested 按正常流程、权限校验、异常分支、幂等与并发分类；避免单个测试类超过职责边界。
- 时间相关逻辑注入 Clock，随机ID注入IdGenerator，当前用户注入CurrentUserProvider，禁止直接读取系统时间或静态全局变量。
- Mock仅用于外部边界；领域规则应直接执行真实实现，避免“Mock了被测逻辑”导致测试失真。

## 2.4 测试通过口径

| **判定项** | **要求**                                                           |
|------------|--------------------------------------------------------------------|
| 断言完整   | 同时断言返回值、状态变化、关键依赖调用、未发生的越权调用及审计结果 |
| 可重复执行 | 任意顺序、多次执行结果一致；不依赖本地固定文件路径和已有Redis Key  |
| 失败可定位 | 测试名称、数据和断言能够直接对应具体业务规则                       |
| 需求可追踪 | 测试类或方法注释关联FR编号；关键流程同时关联BPMN节点或任务Key      |
| 无危险绕过 | 测试不得直接修改ACT\_\*流程表、绕过权限拦截或伪造最终状态          |

# 第三章 测试模块总览

| **编号** | **测试模块**         | **核心内容**                             | **关联需求**             | **优先级** |
|----------|----------------------|------------------------------------------|--------------------------|------------|
| UT-01    | 认证与权限           | 登录、令牌、三角色、菜单、接口与数据范围 | FR-AUTH-01～04           | P0         |
| UT-02    | 部门与员工           | 部门分级、员工档案、账号、历史保护       | FR-ORG-01～03            | P0         |
| UT-03    | 批量导入与经理任命   | 文件预检、行校验、幂等确认、角色变更     | FR-ORG-04～05            | P0         |
| UT-04    | 考勤规则与任务发布   | 多时段规则、版本快照、部门独立调度       | FR-ATT-04～05            | P0         |
| UT-05    | 考勤打卡与结算       | 真实IP、时间窗、幂等、分项结果           | FR-ATT-01～03            | P0         |
| UT-06    | 考勤补交工作流       | 启动、审批、呈报、回写、撤回、历史       | FR-ATT-06、FR-OPS-04     | P0         |
| UT-07    | 请假工作流与附件     | 条件审批、经理直达、附件与撤回           | FR-LEA-01～05、FR-OPS-04 | P0         |
| UT-08    | 公告制度与缓存       | 范围化草稿、发布、下架、详情缓存         | FR-CNT-01～04            | P0         |
| UT-09    | ES全文检索           | 关键词、筛选、高亮、范围过滤和重建       | FR-SCH-01～03            | P0         |
| UT-10    | AI知识来源与索引     | 上传、Tika、分片、TEI、Chroma、版本      | FR-AI-02                 | P0         |
| UT-11    | AI问答与学习计划     | 召回、二次鉴权、引用、拒答、降级         | FR-AI-01、03～06         | P0         |
| UT-12    | 公共平台、看板与日志 | 统一响应、幂等、Nacos、日志、看板        | FR-OPS-01～03            | P0/P1      |
| UT-13    | 前端组件与权限交互   | 路由守卫、表单、状态组件、来源展示       | 对应全部前端P0页面       | P0         |

> **模块边界说明**
>
> 单元测试模块按照代码职责划分，不完全等同于页面菜单。Flowable、ES、ChromaDB和DeepSeek相关测试需分别验证本地业务逻辑与外部接口边界；真实中间件联调仍由集成测试负责。

# 第四章 后端业务单元测试模块

## 4.1 UT-01 认证与权限模块

验证账号登录、会话失效、密码维护、三角色权限和数据范围控制。所有授权判断必须由服务端当前用户上下文产生，不信任请求中的userId、employeeId、departmentId或role。

> **建议测试对象**
>
> AuthService、TokenService、PasswordService、PermissionService、DataScopeResolver、LoginRateLimiter、AuthController

| **编号** | **测试对象/方法**              | **测试内容**                                       | **关键断言**                                                                                   | **需求**   |
|----------|--------------------------------|----------------------------------------------------|------------------------------------------------------------------------------------------------|------------|
| UT-01-01 | AuthService.login              | 有效账号密码登录；返回用户、角色、部门和权限摘要   | 断言密码校验、令牌生成和Redis会话写入各执行一次；返回SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE正确画像 | FR-AUTH-01 |
| UT-01-02 | AuthService.login              | 错误密码、账号不存在和停用账号统一失败             | 不生成令牌；不暴露账号是否存在；记录失败计数                                                   | FR-AUTH-01 |
| UT-01-03 | LoginRateLimiter               | 按账号与IP累计失败并触发短时限流                   | 达到阈值后拒绝；TTL正确；成功登录后按策略清理                                                  | FR-AUTH-01 |
| UT-01-04 | TokenService.logout            | 退出后删除会话或加入失效集合                       | 原令牌再次鉴权失败；重复退出保持幂等                                                           | FR-AUTH-02 |
| UT-01-05 | PasswordService.changePassword | 校验旧密码、强度并使旧会话失效                     | 数据库仅保存摘要；旧令牌和旧密码均失效                                                         | FR-AUTH-04 |
| UT-01-06 | PermissionService              | 三角色菜单与接口权限映射                           | 总负责人全局、部门经理本部门、员工本人；禁止返回越权动作                                       | FR-AUTH-03 |
| UT-01-07 | DataScopeResolver              | 根据当前登录用户强制生成GLOBAL/DEPARTMENT/SELF条件 | 忽略客户端departmentId；部门经理只能得到本人部门条件                                           | FR-AUTH-03 |
| UT-01-08 | ResourceOwnershipChecker       | 员工访问本人数据、部门经理访问本部门普通员工       | 跨部门、访问他人本人业务或角色不足时抛出403                                                    | FR-AUTH-03 |
| UT-01-09 | AccountService.disable         | 禁止停用唯一总负责人和部门经理自停用               | 未满足保护条件时不调用更新Mapper；产生明确业务异常                                             | FR-AUTH-04 |
| UT-01-10 | AuthController                 | 参数校验、401/403和统一响应                        | 响应含code、message、data、traceId；不返回堆栈和密码摘要                                       | 4.11.1     |

## 4.2 UT-02 部门与员工模块

验证部门和员工的分级管理、数据归属、一对一账号关系、唯一约束和历史业务保护。

> **建议测试对象**
>
> DepartmentService、EmployeeService、EmployeeAccountService、DepartmentPermissionChecker

| **编号** | **测试对象/方法**                    | **测试内容**                                     | **关键断言**                                     | **需求**  |
|----------|--------------------------------------|--------------------------------------------------|--------------------------------------------------|-----------|
| UT-02-01 | DepartmentService.create             | 总负责人新增部门                                 | 部门编码唯一；默认状态正确；写入操作日志         | FR-ORG-01 |
| UT-02-02 | DepartmentService.update             | 部门经理仅修改本人部门允许字段                   | 禁止修改leaderId、删除状态和其他部门资料         | FR-ORG-01 |
| UT-02-03 | DepartmentService.disable/delete     | 关联在职员工或历史业务时阻止物理删除             | 返回冲突；不执行delete；允许逻辑停用并保留关系   | FR-ORG-01 |
| UT-02-04 | DepartmentService.query              | 三角色查询范围                                   | 总负责人全部、经理本人部门、员工所属部门单条     | FR-ORG-01 |
| UT-02-05 | EmployeeService.create               | 总负责人创建任意部门员工，经理创建本部门普通员工 | 后端覆盖部门和角色；员工编号唯一                 | FR-ORG-02 |
| UT-02-06 | EmployeeService.update               | 部门经理维护本部门普通员工允许字段               | 禁止跨部门、修改角色、任命经理或调整部门         | FR-ORG-02 |
| UT-02-07 | EmployeeService.queryPage            | 组合筛选叠加数据范围                             | 任何筛选条件都不能移除部门/本人限制              | FR-ORG-02 |
| UT-02-08 | EmployeeService.deactivate           | 离职停用但保留考勤、请假和审计历史               | 账号失效；历史查询仍可关联员工档案               | FR-ORG-02 |
| UT-02-09 | EmployeeAccountService.open          | 员工与账号一对一开通                             | 重复开通失败；初始密码不写日志；默认角色EMPLOYEE | FR-ORG-03 |
| UT-02-10 | EmployeeAccountService.resetPassword | 授权管理员重置普通员工密码                       | 旧密码与旧会话失效；跨部门重置被拒绝             | FR-ORG-03 |

## 4.3 UT-03 批量导入与部门经理任命模块

验证受控Excel/CSV导入的文件安全、逐行校验、异步确认和幂等；验证经理任命、撤销、负责人关系和权限缓存的原子更新。

> **建议测试对象**
>
> EmployeeImportService、ImportFileValidator、ImportRowValidator、ImportTaskExecutor、ManagerAppointmentService

| **编号** | **测试对象/方法**                 | **测试内容**                               | **关键断言**                                        | **需求**  |
|----------|-----------------------------------|--------------------------------------------|-----------------------------------------------------|-----------|
| UT-03-01 | ImportFileValidator               | 校验扩展名、MIME、文件头、大小、行数和表头 | 任一不一致立即拒绝；不创建可执行导入任务            | FR-ORG-04 |
| UT-03-02 | ImportRowValidator                | 必填、格式、部门存在性和唯一字段           | 返回包含行号、字段和原因的错误集合                  | FR-ORG-04 |
| UT-03-03 | ImportRowValidator                | 部门经理导入时强制本人部门和EMPLOYEE角色   | 忽略模板departmentId/role；跨部门和提权数据记为失败 | FR-ORG-04 |
| UT-03-04 | EmployeeImportService.preview     | 同一文件哈希生成可追踪预检任务             | 统计预计成功/失败数；不写正式员工表                 | FR-ORG-04 |
| UT-03-05 | EmployeeImportService.confirm     | 确认后分批写入并记录部分成功               | 成功行可查询；失败行不影响已成功行；任务统计一致    | FR-ORG-04 |
| UT-03-06 | EmployeeImportService.confirm     | 相同taskId重复确认幂等                     | 第二次不重复写入；返回已有结果                      | FR-ORG-04 |
| UT-03-07 | ImportTaskExecutor                | 处理中断或数据库异常                       | 任务转FAILED/部分成功；保留错误明细和traceId        | FR-ORG-04 |
| UT-03-08 | ManagerAppointmentService.appoint | 任命在职且属于目标部门的员工为经理         | 原子更新leader_id与用户角色；旧权限版本失效         | FR-ORG-05 |
| UT-03-09 | ManagerAppointmentService.appoint | 同部门已有经理时按规则替换或拒绝           | 同一部门当前负责人唯一；不可产生双经理关系          | FR-ORG-05 |
| UT-03-10 | ManagerAppointmentService.revoke  | 撤销后恢复普通员工权限并校验审批路由       | 旧会话权限立即失效；保留完整审计记录                | FR-ORG-05 |

## 4.4 UT-04 考勤规则与任务发布模块

验证各部门独立维护多时段考勤规则，规则版本、生效期、CIDR和时间边界合法；验证调度器按部门发布时间幂等生成任务并固化快照。

> **建议测试对象**
>
> AttendanceRuleService、AttendanceSegmentValidator、CidrValidator、AttendanceTaskPublisher、AttendanceTaskLifecycleService

| **编号** | **测试对象/方法**                      | **测试内容**                                        | **关键断言**                                                       | **需求**  |
|----------|----------------------------------------|-----------------------------------------------------|--------------------------------------------------------------------|-----------|
| UT-04-01 | AttendanceSegmentValidator             | 校验时间段开始结束、签到/签退窗口和正常边界包含关系 | 非法顺序、交叉或边界越界时拒绝启用                                 | FR-ATT-04 |
| UT-04-02 | AttendanceSegmentValidator             | 同一规则多个时间段编码唯一且不冲突                  | 上午/下午等合法并存；重复编码或冲突失败                            | FR-ATT-04 |
| UT-04-03 | CidrValidator                          | IPv4/IPv6 CIDR合法性                                | 非法掩码、空白或危险通配配置被拒绝                                 | FR-ATT-04 |
| UT-04-04 | AttendanceRuleService.saveVersion      | 规则改版生成新版本而非覆盖历史                      | 旧版本仍可被历史任务引用；当前版本正确切换                         | FR-ATT-04 |
| UT-04-05 | AttendanceRuleService.permission       | 总负责人管理全部，部门经理仅本人部门，员工只读      | 跨部门写操作403；员工写接口403                                     | FR-ATT-04 |
| UT-04-06 | AttendanceTaskPublisher.scanAndPublish | 仅扫描到达各部门publish_time且当日生效的规则        | 未到时间、停用或不在生效期不生成任务                               | FR-ATT-05 |
| UT-04-07 | AttendanceTaskPublisher.publish        | 按每个segment创建任务并保存规则快照                 | 任务包含department、workDate、segment、ruleVersion、时间阈值和CIDR | FR-ATT-05 |
| UT-04-08 | AttendanceTaskPublisher.publish        | 分布式锁与数据库唯一键双重幂等                      | 并发或重复补偿只产生一条有效任务                                   | FR-ATT-05 |
| UT-04-09 | AttendanceTaskPublisher.publish        | 单部门失败隔离                                      | 失败部门记录原因和重试次数；其他部门继续发布                       | FR-ATT-05 |
| UT-04-10 | AttendanceTaskLifecycleService         | 任务按开始/结束时间自动流转并触发结算               | 状态顺序合法；历史任务不受新规则修改影响                           | FR-ATT-05 |

## 4.5 UT-05 考勤打卡与结算模块

验证员工按部门当日分段任务在允许时间和公司网络内签到签退；验证服务器时间、真实IP、幂等和迟到/早退/缺勤组合结果。

> **建议测试对象**
>
> ClientIpResolver、AttendanceClockService、AttendanceResultCalculator、AttendanceQueryService

| **编号** | **测试对象/方法**               | **测试内容**                               | **关键断言**                                                    | **需求**      |
|----------|---------------------------------|--------------------------------------------|-----------------------------------------------------------------|---------------|
| UT-05-01 | ClientIpResolver                | 直连请求解析remoteAddr                     | 忽略客户端提交IP；保存标准化IP                                  | FR-ATT-01     |
| UT-05-02 | ClientIpResolver                | 仅受信代理链解析X-Forwarded-For            | 可信代理取正确客户端IP；伪造XFF不生效                           | FR-ATT-01     |
| UT-05-03 | AttendanceClockService.checkIn  | 任务已发布、时间窗合法、CIDR允许时签到     | 服务器生成时间；创建/更新task+employee唯一记录                  | FR-ATT-01     |
| UT-05-04 | AttendanceClockService.checkIn  | 超正常截止但仍在可签到窗口                 | 签到成功并标记LATE；不标记缺勤                                  | FR-ATT-01     |
| UT-05-05 | AttendanceClockService.checkIn  | 非允许CIDR、未发布、已结束或窗口外         | 拒绝并记录安全审计；不写打卡事件                                | FR-ATT-01     |
| UT-05-06 | AttendanceClockService.checkOut | 合法签退和提前签退                         | 正常签退保存IP；提前签退标记EARLY_LEAVE                         | FR-ATT-01     |
| UT-05-07 | AttendanceClockService          | 签到和签退分别幂等                         | 同一员工同一任务各最多一次有效事件；重复请求返回首次结果        | FR-ATT-01     |
| UT-05-08 | AttendanceClockService          | 并发签到/签退                              | 唯一约束或条件更新保证不产生重复记录                            | FR-ATT-01     |
| UT-05-09 | AttendanceResultCalculator      | 任务结束后计算MISSING_CHECK_IN/OUT与ABSENT | 两项均缺失时同时保留missing_in、missing_out和absent；结果可组合 | FR-ATT-06     |
| UT-05-10 | AttendanceQueryService          | 本人、本部门和全局查询范围                 | 员工仅本人，经理本部门，总负责人全部；统计与分页条件一致        | FR-ATT-02～03 |

# 第五章 Flowable工作流单元测试模块

## 5.1 UT-06 考勤补交工作流模块

验证attendance-repair流程的启动、部门经理审批、呈报总负责人、经理直达总负责人、ServiceTask回写、撤回和历史追踪。业务代码不得直接修改最终审批状态。

> **建议测试对象**
>
> AttendanceCorrectionService、AttendanceRepairProcessService、TaskAssignmentListener、AttendanceRepairDelegate、WorkflowConsistencyService

| **编号** | **测试对象/方法**                    | **测试内容**                               | **关键断言**                                                         | **需求**  |
|----------|--------------------------------------|--------------------------------------------|----------------------------------------------------------------------|-----------|
| UT-06-01 | AttendanceCorrectionService.submit   | 仅本人对已发布任务的异常/缺失记录发起申请  | 保存PENDING业务记录后以申请ID为businessKey启动流程并写回proc_inst_id | FR-ATT-06 |
| UT-06-02 | attendance-repair BPMN               | 普通员工申请进入本部门经理UserTask         | assignee为正确经理；申请人不能成为审批人                             | FR-ATT-06 |
| UT-06-03 | attendance-repair BPMN               | 部门经理本人申请直接进入总负责人UserTask   | 跳过本人部门经理节点；总负责人获得待办                               | FR-ATT-06 |
| UT-06-04 | TaskService审批                      | 部门经理批准普通员工补交                   | 流程进入ServiceTask；最终APPROVED；实际审批人留痕                    | FR-ATT-06 |
| UT-06-05 | TaskService审批                      | 部门经理驳回                               | 流程结束为REJECTED；原考勤结果不变；意见按规则校验                   | FR-ATT-06 |
| UT-06-06 | TaskService呈报                      | 部门经理选择呈报总负责人                   | 创建总负责人待办；业务状态仍PENDING；历史记录呈报动作                | FR-ATT-06 |
| UT-06-07 | AttendanceRepairDelegate             | 审批通过后写入补交时间并重新计算结果       | 保留原始事件和before/after快照；调用考勤服务一次                     | FR-ATT-06 |
| UT-06-08 | AttendanceRepairDelegate             | 回写失败                                   | 流程与业务事务回滚或产生可重试补偿；不得出现流程已通过但业务未更新   | FR-ATT-06 |
| UT-06-09 | AttendanceCorrectionService.withdraw | 本人在允许状态撤回                         | RuntimeService终止实例并同步WITHDRAWN；重复撤回冲突                  | FR-ATT-06 |
| UT-06-10 | WorkflowTaskGuard                    | 完成他人任务、跨部门任务或最终状态重复处理 | 拒绝TaskService.complete；返回403/409；无状态变化                    | FR-ATT-06 |
| UT-06-11 | HistoryService                       | 批准、驳回、呈报、撤回后的历史完整         | ACT_HI流程/任务、业务审计和操作日志可对应businessKey                 | FR-OPS-04 |

## 5.2 UT-07 请假工作流与附件模块

验证leaveApproval流程的三条核心路径：普通员工不超过3天、普通员工超过3天、部门经理申请；同时验证撤回、任务归属、审批审计和附件授权。

> **建议测试对象**
>
> LeaveService、LeaveProcessService、LeaveTaskListener、LeaveAuditService、LeaveAttachmentService、LeaveFileValidator

| **编号** | **测试对象/方法**                | **测试内容**                                           | **关键断言**                                                         | **需求**      |
|----------|----------------------------------|--------------------------------------------------------|----------------------------------------------------------------------|---------------|
| UT-07-01 | LeaveService.submit              | 合法申请保存业务数据并启动流程                         | 变量包含leaveId、applicantId、applicantRole、days等；businessKey唯一 | FR-LEA-01     |
| UT-07-02 | LeaveService.submit              | 时间非法、附件非法或重复提交                           | 不启动流程；不留下可审批PENDING孤立记录                              | FR-LEA-01     |
| UT-07-03 | leaveApproval BPMN               | 普通员工≤3天且经理通过                                 | 部门经理任务完成后直接APPROVED，不创建总负责人任务                   | FR-LEA-03     |
| UT-07-04 | leaveApproval BPMN               | 普通员工\>3天且经理通过                                | 排他网关进入总负责人二审；业务状态保持PENDING                        | FR-LEA-03     |
| UT-07-05 | leaveApproval BPMN               | 普通员工经理驳回                                       | 流程结束REJECTED；不进入二审                                         | FR-LEA-03     |
| UT-07-06 | leaveApproval BPMN               | 部门经理发起请假                                       | 跳过本人审批，直接分配总负责人任务                                   | FR-LEA-03     |
| UT-07-07 | LeaveProcessService.completeTask | 总负责人通过/驳回二审或经理申请                        | 流程结束后更新APPROVED/REJECTED并保存实际审批人和意见                | FR-LEA-03     |
| UT-07-08 | WorkflowTaskGuard                | 申请人审批本人、经理审批其他部门或完成他人taskId       | 拒绝；不调用TaskService.complete；返回403                            | FR-LEA-03     |
| UT-07-09 | LeaveService.withdraw            | 仅本人且PENDING可撤回                                  | 终止流程并同步WITHDRAWN；终态或流程已结束不能撤回                    | FR-LEA-02     |
| UT-07-10 | LeaveAuditService                | 每个用户任务记录taskId、taskKey、action、comment和时间 | 流程变量、历史任务与oa_leave_audit一致                               | FR-LEA-03～04 |
| UT-07-11 | LeaveFileValidator               | 扩展名、MIME、文件头、大小和数量联合校验               | 脚本/可执行文件、伪装文件和超限文件被拒绝                            | FR-LEA-05     |
| UT-07-12 | LeaveAttachmentService           | 本人PENDING增删、合法审批人查看、终态只读              | 猜测attachmentId、跨部门下载或公开绝对路径均被拒绝                   | FR-LEA-05     |
| UT-07-13 | LeaveAttachmentService           | 文件写入或元数据保存异常补偿                           | 不留下孤儿文件或无文件元数据；错误信息脱敏                           | FR-LEA-05     |

# 第六章 公告检索与AI智慧办公单元测试模块

## 6.1 UT-08 公告制度与缓存模块

验证公告草稿、发布、下架、范围化可见性、版本更新、缓存失效和ES同步事件。公告属于MySQL权威数据，ES仅为可重建副本。

> **建议测试对象**
>
> ContentService、ContentPermissionService、ContentCacheService、ContentIndexEventPublisher

| **编号** | **测试对象/方法**                | **测试内容**                             | **关键断言**                                          | **需求**      |
|----------|----------------------------------|------------------------------------------|-------------------------------------------------------|---------------|
| UT-08-01 | ContentService.createDraft       | 总负责人创建ALL或指定部门草稿            | 范围与部门字段组合合法；草稿不进入员工查询            | FR-CNT-01     |
| UT-08-02 | ContentService.createDraft       | 部门经理创建草稿                         | 后端强制DEPARTMENT和本人部门；忽略客户端ALL或其他部门 | FR-CNT-01     |
| UT-08-03 | ContentService.updateDraft       | 仅创建者或授权管理角色修改允许状态内容   | 已下架/已发布修改按版本规则处理；乐观锁冲突返回409    | FR-CNT-01～02 |
| UT-08-04 | ContentService.publish           | 发布后写状态、发布人、时间并发出索引事件 | 事务成功后才同步ES；清理详情/列表缓存                 | FR-CNT-02     |
| UT-08-05 | ContentService.offline           | 下架后员工列表、详情和搜索不可见         | 发送删除/停用索引事件；缓存立即失效                   | FR-CNT-02     |
| UT-08-06 | ContentPermissionService.canRead | ALL或本人部门的已发布公告可读            | 草稿、下架和其他部门公告拒绝；总负责人管理查询例外    | FR-CNT-03     |
| UT-08-07 | ContentCacheService              | 热点详情命中与回源                       | 缓存Key含id+version；权限校验先于返回缓存             | FR-CNT-03     |
| UT-08-08 | ContentService                   | 富文本脚本清洗                           | 危险标签/属性被移除；受控文本正常保留                 | FR-CNT-01     |
| UT-08-09 | ContentIndexEventPublisher       | ES同步失败                               | 不回滚MySQL公告；记录重试信息和traceId                | FR-CNT-04     |

## 6.2 UT-09 Elasticsearch全文检索模块

验证公告关键词检索、筛选、分页、高亮和可见范围过滤。前端不得直接访问ES，查询由后端基于当前用户构造。

> **建议测试对象**
>
> AnnouncementSearchService、SearchQueryBuilder、SearchScopeFilter、HighlightSanitizer、IndexRebuildService

| **编号** | **测试对象/方法**         | **测试内容**                              | **关键断言**                                                  | **需求**  |
|----------|---------------------------|-------------------------------------------|---------------------------------------------------------------|-----------|
| UT-09-01 | SearchQueryBuilder        | 标题高权重、正文参与中文关键词查询        | 构造多字段查询和合理排序；空关键词按规则拒绝                  | FR-SCH-01 |
| UT-09-02 | SearchQueryBuilder        | 类型、分类、发布时间和分页组合            | 单页默认10、最大50；非法排序字段使用白名单拦截                | FR-SCH-02 |
| UT-09-03 | SearchScopeFilter         | 普通员工/经理过滤ALL或本人部门且PUBLISHED | 查询DSL中同时包含状态和范围过滤                               | FR-SCH-03 |
| UT-09-04 | SearchScopeFilter         | 总负责人搜索已发布公告                    | 可查询全部已发布公告，但草稿仍不进入员工搜索接口              | FR-SCH-03 |
| UT-09-05 | HighlightSanitizer        | 只保留受控高亮标签                        | 用户正文中的脚本或伪造标签被转义，避免XSS                     | FR-SCH-02 |
| UT-09-06 | AnnouncementSearchService | ES命中后回查MySQL状态与范围               | 状态变化或范围变化的旧索引结果被过滤                          | FR-SCH-03 |
| UT-09-07 | IndexRebuildService       | 按内容重建和全部重建                      | 读取MySQL已发布数据，统计成功/失败/耗时                       | FR-CNT-04 |
| UT-09-08 | IndexRebuildService       | 重复重建锁和失败保留旧索引                | 同一时刻只运行一个任务；失败不破坏现有可用索引                | FR-CNT-04 |
| UT-09-09 | AnnouncementSearchService | ES不可用                                  | 返回可识别友好错误；不改用AI知识库或MySQL正文模糊查询伪装成功 | FR-SCH-01 |

## 6.3 UT-10 AI知识来源与索引模块

验证Word/PDF知识文件从上传、校验、Tika解析、分片、TEI向量化到ChromaDB写入的完整本地逻辑，以及权限元数据、版本切换、停用和重建。

> **建议测试对象**
>
> AiSourceService、KnowledgeFileValidator、TikaDocumentParser、TextChunker、TeiEmbeddingClient、ChromaIndexService、SourceVersionService

| **编号** | **测试对象/方法**                  | **测试内容**                                    | **关键断言**                                                                   | **需求** |
|----------|------------------------------------|-------------------------------------------------|--------------------------------------------------------------------------------|----------|
| UT-10-01 | KnowledgeFileValidator             | doc/docx/pdf扩展名、MIME、文件头、大小和SHA-256 | 伪装文件、重复文件和危险文件被拒绝；存储名随机化                               | FR-AI-02 |
| UT-10-02 | AiSourceService.create             | 总负责人和部门经理权限范围                      | 总负责人可ALL/任意部门；经理仅本人部门；员工403                                | FR-AI-02 |
| UT-10-03 | TikaDocumentParser                 | 解析标题、正文、章节和页码元数据                | 空文档或解析异常转FAILED并保存脱敏原因                                         | FR-AI-02 |
| UT-10-04 | TextChunker                        | 按400～600中文字符切分并重叠50～80字符          | 分片顺序、chunkNo、section/page和hash稳定；不丢失正文                          | FR-AI-02 |
| UT-10-05 | TextChunker                        | 控制Embedding输入最大约480 tokens               | 超长段落继续拆分；短段落合理合并且不越章节边界                                 | FR-AI-02 |
| UT-10-06 | TeiEmbeddingClient                 | 批量最多16条，向量维度512，normalize=true       | 维度不符、空向量或超时均标记索引失败，不写Chroma                               | FR-AI-02 |
| UT-10-07 | ChromaIndexService                 | 写入vector_key与权限元数据                      | metadata含source/chunk/status/scope/department/minRole/version；距离类型cosine | FR-AI-02 |
| UT-10-08 | AiSourceService.index              | 全部分片成功后才将index_status置SUCCESS         | 部分失败不得对外检索；可重试失败分片                                           | FR-AI-02 |
| UT-10-09 | SourceVersionService.switchVersion | 新版本全部索引成功后原子切换                    | 旧版本在切换前仍可用；切换后使旧向量失效并异步清理                             | FR-AI-02 |
| UT-10-10 | AiSourceService.disable            | MySQL状态立即阻断检索                           | 即使Chroma删除延迟，后续二次鉴权也不能使用该来源                               | FR-AI-02 |
| UT-10-11 | ChromaIndexService.reindex         | 同一source重建锁与幂等                          | 重复请求不产生重复vector_key；任务状态可追踪                                   | FR-AI-02 |

## 6.4 UT-11 AI问答与学习计划模块

验证AI问题向量、Chroma权限预过滤、MySQL二次鉴权、最小上下文、DeepSeek调用、引用保存、越权拒答、缓存限流和安全降级。AI不得检索公告ES，也不得执行业务写操作。

> **建议测试对象**
>
> AiChatService、QueryEmbeddingService、KnowledgeRetriever、KnowledgeAuthorizationService、PromptBuilder、DeepSeekClient、CitationService、AiCacheService、AiRateLimiter、LearningPlanService

| **编号** | **测试对象/方法**             | **测试内容**                                                 | **关键断言**                                                                 | **需求**    |
|----------|-------------------------------|--------------------------------------------------------------|------------------------------------------------------------------------------|-------------|
| UT-11-01 | QueryEmbeddingService         | 问题使用与文档一致模型、前缀和normalize配置                  | 生成512维向量；模型标识来自配置，不硬编码                                    | FR-AI-01    |
| UT-11-02 | KnowledgeRetriever            | 按状态、scope、department和minRole查询Chroma，最多取15条     | 普通员工、经理和总负责人过滤条件正确；不调用公告ES                           | FR-AI-01/04 |
| UT-11-03 | KnowledgeAuthorizationService | 对候选source/chunk回查MySQL状态、版本和权限                  | 未通过二次鉴权的片段全部剔除且不得发送DeepSeek                               | FR-AI-01/04 |
| UT-11-04 | KnowledgeRetriever            | 去重并选择5～8条最小必要片段                                 | 同来源重复片段合并；上下文长度受控；相关排序稳定                             | FR-AI-01    |
| UT-11-05 | AiChatService                 | 有授权依据时调用DeepSeek并返回答案                           | Prompt仅含授权上下文；结果标注AI生成；保存会话权限快照                       | FR-AI-01    |
| UT-11-06 | CitationService               | 仅保存实际进入模型上下文的来源                               | oa_ai_citation含source_id/chunk_id/版本/章节/页码/分数；不保存公告content_id | FR-AI-03    |
| UT-11-07 | CitationService.preview       | 点击来源时再次鉴权                                           | 来源停用、版本变化或权限调整后拒绝原文访问并使缓存失效                       | FR-AI-03    |
| UT-11-08 | AiChatService                 | 无授权资料或证据不足                                         | 明确拒答，不根据常识补全企业制度；不泄露受限文档标题                         | FR-AI-04    |
| UT-11-09 | AiSafetyGuard                 | 提示注入、内嵌指令、敏感数据和业务写请求                     | 阻断审批、考勤、账号或公告写操作；记录安全摘要                               | FR-AI-04    |
| UT-11-10 | AiCacheService                | 缓存Key包含角色、部门、permissionDigest、epoch和questionHash | 不同部门/角色不共享越权答案；TTL正确；命中后仍复核来源状态                   | FR-AI-05    |
| UT-11-11 | AiRateLimiter                 | 每用户窗口限流                                               | 达到阈值返回明确提示；配置从Nacos读取；Redis Key有TTL                        | FR-AI-05    |
| UT-11-12 | AiChatService                 | DeepSeek超时或开关关闭                                       | 返回已通过鉴权的检索片段并标记降级；基础OA不受影响                           | FR-AI-05    |
| UT-11-13 | AiChatService                 | ChromaDB或TEI不可用                                          | 安全失败并提示知识检索不可用；不得改查公告ES或越权缓存                       | FR-AI-05    |
| UT-11-14 | LearningPlanService           | 根据目标、周期和可用时间生成分阶段计划                       | 仅使用授权资料，包含任务、时间、验收建议和引用；资料不足说明缺口             | FR-AI-06    |

# 第七章 公共平台与前端单元测试模块

## 7.1 UT-12 公共平台、看板与日志模块

验证统一响应、全局异常、幂等防重、配置治理、缓存故障、看板聚合和关键操作审计。平台支撑逻辑必须在依赖异常时保持安全失败，不得放宽权限或返回伪造数据。

> **建议测试对象**
>
> ApiResultFactory、GlobalExceptionHandler、IdempotencyService、InternalRequestVerifier、ConfigProperties、DashboardService、OperationLogAspect、SensitiveDataMasker

| **编号** | **测试对象/方法**       | **测试内容**                                           | **关键断言**                                                | **需求**      |
|----------|-------------------------|--------------------------------------------------------|-------------------------------------------------------------|---------------|
| UT-12-01 | ApiResultFactory        | 成功、参数错误、未登录、无权限、冲突和系统异常响应     | 统一包含code、message、data、traceId；不暴露SQL、堆栈和密钥 | 4.11.1        |
| UT-12-02 | GlobalExceptionHandler  | 业务异常与中间件异常映射                               | 400/401/403/404/409/500区分正确；日志含traceId              | 4.11.1        |
| UT-12-03 | IdempotencyService      | 签到、流程启动/完成/撤回、公告发布和导入确认防重       | 相同requestId返回首次结果；Key含用户和业务类型；TTL正确     | 4.11.3        |
| UT-12-04 | InternalRequestVerifier | 内部身份、时间戳、nonce和签名验证                      | 伪造签名、过期请求和重放请求被拒绝                          | FR-OPS-03     |
| UT-12-05 | ConfigProperties        | Flowable Key、AI模型、超时、限流和中间件地址从配置读取 | 缺少关键配置启动失败或安全关闭；不得散落硬编码              | FR-OPS-03～04 |
| UT-12-06 | DashboardService        | 员工数、今日考勤、待审批和公告数聚合                   | 总负责人可见；缓存命中/回源一致；部分指标失败按设计降级     | FR-OPS-01     |
| UT-12-07 | OperationLogAspect      | 记录经理任命、导入、规则、流程、公告、索引和AI降级     | 操作者、部门、动作、结果、traceId完整；失败操作也留痕       | FR-OPS-02     |
| UT-12-08 | SensitiveDataMasker     | 密码、令牌、API Key、完整正文和绝对路径脱敏            | 敏感值不进入日志；必要业务ID和错误摘要保留                  | FR-OPS-02     |
| UT-12-09 | Redis故障策略           | 非安全缓存回源，权限/会话缓存安全处理                  | 热点可回源MySQL；会话无法确认时拒绝访问；不默认放行         | NFR-STAB-05   |
| UT-12-10 | 健康与治理              | Nacos配置变化、AI开关和健康状态映射                    | 仅允许动态刷新的配置生效；关闭AI不影响基础OA                | FR-OPS-03     |

## 7.2 UT-13 前端组件与权限交互模块

验证Vue前端的路由守卫、菜单裁剪、表单状态、审批待办、考勤操作、知识文件状态和AI来源展示。前端测试用于保证交互正确，但不能替代后端权限测试。

> **建议测试对象**
>
> router guard、permission directive、Axios interceptor、Pinia stores、Attendance/Leave/Content/AI页面组件

| **编号** | **测试对象/方法** | **测试内容**                                | **关键断言**                                              | **需求**        |
|----------|-------------------|---------------------------------------------|-----------------------------------------------------------|-----------------|
| UT-13-01 | 路由守卫          | 未登录访问受保护页面                        | 跳转登录页并保留目标地址；不重复触发请求                  | FR-AUTH-02      |
| UT-13-02 | 路由守卫/菜单     | 三角色动态路由和菜单裁剪                    | 总负责人、经理、员工看到各自入口；无权限路由进入403       | FR-AUTH-03      |
| UT-13-03 | Axios拦截器       | 401统一退出、403展示无权限、409展示状态冲突 | 并发401只执行一次退出流程；403不自动重试                  | 4.11.1          |
| UT-13-04 | 员工/部门页面     | 根据数据范围隐藏或禁用操作                  | 经理不能切换其他部门；员工页面无管理按钮                  | FR-ORG-01～03   |
| UT-13-05 | 考勤任务组件      | 按时间段展示签到/签退窗口与状态             | 窗口外按钮禁用；迟到/早退/缺勤可组合展示                  | FR-ATT-01～02   |
| UT-13-06 | 考勤补交页面      | 提交、待办、呈报、撤回和状态轨迹            | 不同角色操作按钮与当前Flowable任务一致                    | FR-ATT-06       |
| UT-13-07 | 请假表单          | 起止时间、原因、附件和重复提交              | 非法时间即时提示；提交中禁止重复点击；终态附件只读        | FR-LEA-01/05    |
| UT-13-08 | 请假待办页面      | ≤3天、\>3天、经理申请节点展示               | 节点名称、审批按钮和历史轨迹与后端返回一致                | FR-LEA-03～04   |
| UT-13-09 | 公告与搜索页面    | 范围化列表、筛选、分页和安全高亮            | 草稿/下架不可见；高亮仅渲染受控标签                       | FR-CNT/FR-SCH   |
| UT-13-10 | 知识上传页面      | 解析/索引状态和失败重试                     | PENDING/PROCESSING/SUCCESS/FAILED展示准确；员工无上传入口 | FR-AI-02        |
| UT-13-11 | AI问答组件        | 加载、拒答、降级、引用与原文入口            | 来源列表完整；越权不显示文档标题；降级有明确标识          | FR-AI-01/03～05 |
| UT-13-12 | 学习计划组件      | 目标、周期、可用时间输入和结构化结果        | 必填校验；阶段任务与引用正确展示                          | FR-AI-06        |

# 第八章 测试数据、Mock与覆盖率要求

## 8.1 基础测试数据集

| **数据类型**  | **建议数据**                                                                   | **覆盖目的**                             |
|---------------|--------------------------------------------------------------------------------|------------------------------------------|
| 用户角色      | 1名SUPER_ADMIN、至少2名不同部门DEPT_MANAGER、每部门2～3名EMPLOYEE、1名停用员工 | 覆盖全局、本部门、本人、跨部门和停用状态 |
| 部门          | 技术部、人事部、财务部、行政部；含启用和停用部门                               | 覆盖经理归属、规则差异和跨部门权限       |
| 考勤规则      | 每部门2个版本、2～4个时间段、不同publish_time和CIDR                            | 覆盖版本快照、独立调度和时间边界         |
| 考勤任务/记录 | 正常、迟到、早退、缺勤、未签到、未签退及组合结果                               | 覆盖打卡、结算和补交重算                 |
| 考勤补交      | 普通员工批准/驳回/呈报、经理申请、撤回、回写失败                               | 覆盖attendance-repair全部路径            |
| 请假          | 员工≤3天、员工\>3天、经理申请、初审/二审驳回、撤回                             | 覆盖leaveApproval条件网关                |
| 公告          | ALL、各部门、草稿、已发布、已下架                                              | 覆盖列表、详情、缓存和ES范围             |
| 知识文件      | doc/docx/pdf、ALL/部门/经理级、版本、停用、解析失败                            | 覆盖Tika、TEI、Chroma和权限              |
| AI问题        | 有依据、无依据、跨部门、角色不足、提示注入、写操作、模型异常                   | 覆盖拒答、引用和降级                     |

## 8.2 Mock边界

| **依赖**             | **Mock方式**                                              | **注意事项**                                           |
|----------------------|-----------------------------------------------------------|--------------------------------------------------------|
| 数据库Mapper         | 预设返回数据、唯一冲突、乐观锁0行更新和数据库异常         | 验证Service编排与异常转换，不在所有测试中启动真实MySQL |
| Redis                | 模拟命中、未命中、TTL、连接失败和重复幂等Key              | 权限缓存失败时必须安全拒绝或回查                       |
| Flowable外部业务调用 | 流程引擎使用真实轻量ProcessEngine；跨微服务回写接口可Mock | 确保路由真实、外部边界可控                             |
| Elasticsearch        | Mock搜索命中、超时、索引失败和重建结果                    | 验证查询DSL、权限复核和故障提示                        |
| Tika/TEI/Chroma      | 分别模拟解析、维度异常、写入失败、召回越权候选            | 验证每阶段状态机和二次鉴权                             |
| DeepSeek             | 模拟正常答案、超时、错误和恶意输出                        | 验证降级、引用和输出安全边界                           |
| 文件存储             | 内存临时目录或Mock对象存储                                | 验证路径随机化、补偿和授权下载                         |
| 时间与ID             | 注入固定Clock和可预测IdGenerator                          | 确保边界测试稳定且可重复                               |

## 8.3 覆盖率建议

| **对象**                           | **目标**                     | **说明**                                            |
|------------------------------------|------------------------------|-----------------------------------------------------|
| 权限判断、数据范围、状态机         | 行覆盖率≥90%，分支覆盖率≥90% | 属于安全与业务边界核心代码                          |
| Flowable流程路由、监听器和Delegate | 关键路径与网关分支100%覆盖   | 三条请假路径、补交批准/驳回/呈报/经理直达均必须通过 |
| 核心Service层                      | 行覆盖率≥80%，分支覆盖率≥75% | 所有P0正常、异常、权限和幂等场景至少各1条           |
| 工具类与校验器                     | 行覆盖率≥85%                 | CIDR、时间窗口、文件校验、分片和缓存Key             |
| Controller与前端组件               | 核心页面/接口分支≥70%        | 重点覆盖参数错误、401/403/409和交互状态             |
| 总体项目                           | 建议行覆盖率≥75%             | 覆盖率不得通过忽略关键包或空断言虚增                |

## 8.4 必须验证的负向调用

- 越权失败时，必须verify目标Mapper写操作、TaskService.complete、DeepSeek调用或文件下载调用从未发生。
- 流程启动失败时，不得留下可审批PENDING业务记录；ServiceTask失败时不得将业务状态标记为APPROVED。
- Chroma候选未通过MySQL二次鉴权时，必须断言该片段没有进入PromptBuilder和DeepSeek请求。
- 公告ES同步失败时，应断言MySQL公告已保存但重试任务已记录；不得回滚权威正文。
- 缓存或Nacos异常时，不得使用默认“全部权限”继续执行。

# 第九章 测试执行、报告与缺陷处理

## 9.1 执行顺序

> **1.** 开发成员在本地先执行纯函数、Service和前端组件测试，确保单模块快速通过。
>
> **2.** 提交合并请求前执行当前微服务全部单元测试和覆盖率检查。
>
> **3.** 修改BPMN流程、权限规则、数据库字段或AI链路时，强制执行对应模块及需求追踪回归。
>
> **4.** 每日集成后由团队统一执行全部单元测试；失败用例不得直接跳过或改为无断言。
>
> **5.** 答辩前冻结代码后保存测试报告、覆盖率报告和关键Flowable路径截图作为交付证据。

## 9.2 测试结果记录

| **状态** | **定义**                                      | **处理**                           |
|----------|-----------------------------------------------|------------------------------------|
| PASS     | 所有断言通过，未发现异常副作用                | 允许合并                           |
| FAIL     | 实际结果与业务规则不一致，或出现越权/状态错乱 | 必须修复并回归                     |
| BLOCKED  | 测试环境或必要配置缺失，无法执行              | 记录阻塞原因和责任人，不得视为通过 |
| SKIPPED  | 仅允许明确的P2未实现范围                      | P0测试不得长期跳过                 |

## 9.3 缺陷优先级

| **等级** | **示例**                                                           | **要求**         |
|----------|--------------------------------------------------------------------|------------------|
| 阻断     | 服务无法启动、流程定义无法部署、核心测试无法运行                   | 立即修复         |
| 严重     | 权限绕过、跨部门泄露、Flowable业务状态不一致、越权片段进入DeepSeek | 提交前必须为0    |
| 一般     | 局部规则错误、异常提示不正确、缓存未及时失效                       | 原则上修复后提交 |
| 轻微     | 测试命名、日志文本、非关键页面状态展示问题                         | 登记并安排修复   |

## 9.4 完成定义（DoD）

- P0代码对应正常、异常、权限、边界和幂等测试已编写并通过。
- Flowable两套BPMN流程的所有条件分支、任务分配、撤回和服务任务均有自动化测试。
- 测试类关联需求编号，新增或修改业务规则时同步更新测试用例表。
- 覆盖率达到团队约定，且未通过排除核心包、删除断言或只验证Mock调用方式虚增。
- 不存在阻断或严重缺陷，测试报告可在全新环境重复生成。

# 附录A 需求追踪矩阵

| **需求范围**   | **测试模块**        | **主要验证内容**                               |
|----------------|---------------------|------------------------------------------------|
| FR-AUTH-01～04 | UT-01、UT-13        | 登录、令牌、三角色权限、密码与前端守卫         |
| FR-ORG-01～03  | UT-02、UT-13        | 部门员工分级管理、账号和历史保护               |
| FR-ORG-04～05  | UT-03               | 批量导入预检/确认、经理任命与权限失效          |
| FR-ATT-01～03  | UT-05、UT-13        | 真实IP、多时段打卡、结果与查询范围             |
| FR-ATT-04～05  | UT-04               | 规则版本、多时间段、部门独立调度和任务快照     |
| FR-ATT-06      | UT-06、UT-13        | 考勤补交流程、呈报、回写、撤回与历史           |
| FR-LEA-01～05  | UT-07、UT-13        | 请假条件流程、附件、详情、审批与撤回           |
| FR-CNT-01～04  | UT-08、UT-13        | 公告范围、状态、缓存和索引同步                 |
| FR-SCH-01～03  | UT-09、UT-13        | 公告ES检索、分页、高亮和范围过滤               |
| FR-AI-01～06   | UT-10、UT-11、UT-13 | 知识索引、二次鉴权、引用、拒答、降级和学习计划 |
| FR-OPS-01～04  | UT-06、UT-07、UT-12 | 看板、日志、Nacos和Flowable运行管理            |

# 附录B 推荐测试类与目录结构

## B.1 后端目录建议

| **模块**             | **推荐路径/测试类**                         | **职责**               |
|----------------------|---------------------------------------------|------------------------|
| identity-service     | auth/AuthServiceTest.java                   | 登录、退出、密码与会话 |
| identity-service     | permission/DataScopeResolverTest.java       | 三角色与数据范围       |
| identity-service     | workflow/LeaveApprovalProcessTest.java      | 请假BPMN条件路由       |
| identity-service     | workflow/AttendanceRepairProcessTest.java   | 考勤补交BPMN路由       |
| identity-service     | workflow/\*DelegateTest.java                | ServiceTask回写与补偿  |
| organization-service | department/DepartmentServiceTest.java       | 部门分级管理           |
| organization-service | employee/EmployeeServiceTest.java           | 员工档案与账号         |
| organization-service | importer/EmployeeImportServiceTest.java     | 批量导入               |
| attendance-service   | rule/AttendanceRuleServiceTest.java         | 规则版本与时间段       |
| attendance-service   | task/AttendanceTaskPublisherTest.java       | 部门独立调度           |
| attendance-service   | clock/AttendanceClockServiceTest.java       | IP打卡与幂等           |
| attendance-service   | result/AttendanceResultCalculatorTest.java  | 迟到早退缺勤组合       |
| content-service      | content/ContentServiceTest.java             | 公告状态与范围         |
| search-service       | search/AnnouncementSearchServiceTest.java   | ES查询与范围过滤       |
| ai-service           | source/AiSourceServiceTest.java             | 知识来源与版本         |
| ai-service           | index/TextChunkerTest.java                  | 文档分片               |
| ai-service           | index/ChromaIndexServiceTest.java           | 向量元数据和重建       |
| ai-service           | chat/AiChatServiceTest.java                 | RAG问答、拒答和降级    |
| ai-service           | chat/KnowledgeAuthorizationServiceTest.java | MySQL二次鉴权          |
| common               | web/GlobalExceptionHandlerTest.java         | 统一异常响应           |
| common               | security/InternalRequestVerifierTest.java   | 内部调用安全           |

## B.2 前端目录建议

| **推荐路径**                                          | **测试内容**               |
|-------------------------------------------------------|----------------------------|
| src/router/\_\_tests\_\_/guard.spec.ts                | 路由登录态、角色和403守卫  |
| src/stores/\_\_tests\_\_/user.spec.ts                 | 用户画像、菜单和退出状态   |
| src/views/attendance/\_\_tests\_\_/task.spec.ts       | 分段打卡状态和按钮         |
| src/views/attendance/\_\_tests\_\_/correction.spec.ts | 补交提交、审批、呈报和撤回 |
| src/views/leave/\_\_tests\_\_/form.spec.ts            | 请假表单与附件             |
| src/views/leave/\_\_tests\_\_/task.spec.ts            | Flowable待办和历史节点     |
| src/views/content/\_\_tests\_\_/search.spec.ts        | 公告筛选、分页和高亮       |
| src/views/ai/\_\_tests\_\_/source.spec.ts             | 知识上传和索引状态         |
| src/views/ai/\_\_tests\_\_/chat.spec.ts               | 问答、拒答、降级和引用     |

## B.3 推荐测试方法命名示例

- `shouldRejectCrossDepartmentEmployeeUpdate_whenManagerUsesForgedDepartmentId`
- `shouldCreateOneTaskOnly_whenPublisherTriggeredConcurrently`
- `shouldMarkAbsentAndMissingInOut_whenNoAttendanceEventsAtTaskEnd`
- `shouldEscalateToSuperAdmin_whenManagerChoosesEscalate`
- `shouldRouteToSuperAdmin_whenEmployeeLeaveExceedsThreeDays`
- `shouldSkipManagerTask_whenApplicantIsDepartmentManager`
- `shouldNotCallDeepSeek_whenChunkFailsMysqlAuthorization`
- `shouldReturnAuthorizedChunks_whenDeepSeekTimesOut`
- `shouldNotSearchElasticsearch_whenAiKnowledgeServiceUnavailable`

# 结语

本说明书以智办AI OA需求规格说明书V2.0为基线，将最新业务规则落实到可执行的单元测试模块。开发过程中应优先保障权限边界、Flowable流程一致性、考勤规则快照、公告与RAG检索分离、MySQL二次鉴权和异常安全降级。任何影响上述规则的代码变更，均应先更新测试并通过回归后再合并。
