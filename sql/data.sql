-- ===================================================================
-- 智办AI OA 数据库完整初始化脚本（包含所有表结构定义与测试样例数据）
-- 适用数据库: oa_db
-- 编制日期: 2026-07-23
-- ===================================================================

CREATE DATABASE IF NOT EXISTS oa_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oa_db;

-- ===================================================================
-- 1. 系统用户表 -- 登录账号、角色权限、关联员工
-- ===================================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password_hash   VARCHAR(200) NOT NULL COMMENT '密码摘要(BCrypt)',
    role            VARCHAR(30)  NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色: SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用',
    employee_id     BIGINT       NULL COMMENT '关联员工ID',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_employee_id (employee_id),
    KEY idx_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ===================================================================
-- 2. 部门表 -- 组织架构，支持负责人设置
-- ===================================================================
CREATE TABLE IF NOT EXISTS sys_department (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL COMMENT '部门编码（唯一，如 DEPT_IT）',
    name            VARCHAR(100) NOT NULL COMMENT '部门名称',
    leader_id       BIGINT       NULL COMMENT '部门负责人员工ID',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用',
    sort            INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

-- ===================================================================
-- 3. 员工档案表 -- 员工基本信息、部门归属
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_employee (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_no     VARCHAR(50)  NOT NULL COMMENT '员工编号（唯一）',
    name            VARCHAR(50)  NOT NULL COMMENT '员工姓名',
    department_id   BIGINT       NULL COMMENT '所属部门ID',
    position        VARCHAR(100) NULL COMMENT '职位',
    phone           VARCHAR(20)  NULL COMMENT '联系方式',
    id_number       VARCHAR(18)  NULL COMMENT '身份证号（18位）',
    email           VARCHAR(100) NULL COMMENT '电子邮箱',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-在职 0-离职',
    hire_date       DATE         NULL COMMENT '入职日期',
    work_years      DECIMAL(4,1) NULL DEFAULT 0 COMMENT '工作年限（年）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_employee_no (employee_no),
    KEY idx_department (department_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工档案';

-- ===================================================================
-- 4. 部门独立考勤规则配置表 -- 多场次时段控制
-- ===================================================================
CREATE TABLE IF NOT EXISTS sys_department_attendance_rule (
    id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id                BIGINT       NOT NULL COMMENT '所属部门ID',
    session_name                 VARCHAR(50)  NOT NULL COMMENT '考勤场次名称(如 上午场, 下午场)',
    check_in_start_time          VARCHAR(10)  NOT NULL COMMENT '签到允许开始时间(HH:mm)',
    normal_check_in_end_time     VARCHAR(10)  NOT NULL COMMENT '正常签到截止时间(HH:mm, 超过则计迟到)',
    check_in_end_time            VARCHAR(10)  NOT NULL COMMENT '签到最晚截止时间(HH:mm)',
    normal_check_out_start_time  VARCHAR(10)  NOT NULL COMMENT '正常签退最早时间(HH:mm, 早于则计早退)',
    check_out_end_time           VARCHAR(10)  NOT NULL COMMENT '签退最晚截止时间(HH:mm)',
    enabled                      INT          NOT NULL DEFAULT 1 COMMENT '规则启用状态 (1-启用 0-禁用)',
    create_time                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_dept_session (department_id, session_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门独立考勤规则配置表';

-- ===================================================================
-- 5. 考勤记录表 -- 每日多场次签到/签退打卡与 Flowable 补签扩展
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_attendance (
    id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id                  BIGINT       NOT NULL COMMENT '员工ID',
    department_id                BIGINT       NULL     COMMENT '所属部门ID',
    work_date                    VARCHAR(10)  NOT NULL COMMENT '工作日期(yyyy-MM-dd)',
    session_name                 VARCHAR(50)  NOT NULL DEFAULT '默认场次' COMMENT '考勤场次名称(如 上午场, 下午场)',
    check_in                     DATETIME     NULL     COMMENT '签到时间',
    check_out                    DATETIME     NULL     COMMENT '签退时间',
    check_in_ip                  VARCHAR(45)  NULL     COMMENT '签到时客户端IP',
    check_out_ip                 VARCHAR(45)  NULL     COMMENT '签退时客户端IP',
    check_in_start_time          VARCHAR(10)  NULL     COMMENT '签到允许开始时间(HH:mm)',
    normal_check_in_end_time     VARCHAR(10)  NULL     COMMENT '正常签到截止时间(HH:mm)',
    check_in_end_time            VARCHAR(10)  NULL     COMMENT '签到最晚截止时间(HH:mm)',
    normal_check_out_start_time  VARCHAR(10)  NULL     COMMENT '正常签退最早时间(HH:mm)',
    check_out_end_time           VARCHAR(10)  NULL     COMMENT '签退最晚截止时间(HH:mm)',
    status                       VARCHAR(20)  NOT NULL DEFAULT 'UNCHECKED' COMMENT '状态: UNCHECKED未签到/CHECKED_IN已签到/LATE迟到/EARLY_LEAVE早退/CHECKED_OUT已签退/ABSENT缺勤/REPLENISHED已补签',
    replenish_status             VARCHAR(20)  NOT NULL DEFAULT 'NONE' COMMENT '补签状态: NONE/PENDING/APPROVED/REJECTED',
    replenish_reason             VARCHAR(500) NULL     COMMENT '补签申请原因',
    approver_id                  BIGINT       NULL     COMMENT '补签审批人用户ID',
    approve_time                 DATETIME     NULL     COMMENT '补签审批时间',
    approve_comment              VARCHAR(500) NULL     COMMENT '补签审批意见说明',
    create_time                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_emp_date_session (employee_id, work_date, session_name),
    KEY idx_status (status),
    KEY idx_replenish_status (replenish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录';

-- ===================================================================
-- 6. 请假申请表 -- 员工请假申请，含审批状态流转
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_leave (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id    BIGINT       NOT NULL COMMENT '申请人ID(员工ID)',
    type            VARCHAR(20)  NOT NULL COMMENT '请假类型: ANNUAL年假/SICK病假/PERSONAL事假/OTHER其他',
    start_time      DATETIME     NOT NULL COMMENT '开始时间',
    end_time        DATETIME     NOT NULL COMMENT '结束时间',
    reason          VARCHAR(500) NOT NULL COMMENT '请假原因',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING待审批/APPROVED通过/REJECTED驳回/WITHDRAWN撤回',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_applicant_status (applicant_id, status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请';

-- ===================================================================
-- 7. 请假附件表 -- 请假申请的图片/文件证明材料
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_leave_attachment (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    leave_id        BIGINT       NULL     COMMENT '关联的请假申请ID',
    file_name       VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_url        VARCHAR(500) NOT NULL COMMENT '文件存储路径/URL（OSS地址）',
    file_size       BIGINT       NULL     COMMENT '文件大小（字节）',
    mime_type       VARCHAR(50)  NULL     COMMENT '文件MIME类型',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_leave_id (leave_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假附件';

-- ===================================================================
-- 8. 审批记录表 -- 请假审批的每一步操作记录
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_leave_audit (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_id        BIGINT       NOT NULL COMMENT '请假申请ID',
    auditor_id      BIGINT       NOT NULL COMMENT '审批人ID(员工ID)',
    action          VARCHAR(20)  NOT NULL COMMENT '审批动作: APPROVED通过/REJECTED驳回',
    comment         VARCHAR(500) NULL COMMENT '审批意见',
    audit_time      DATETIME     NULL COMMENT '审批时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_leave_id (leave_id),
    KEY idx_auditor_id (auditor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录';

-- ===================================================================
-- 9. 公告/制度内容表 -- 企业公告与制度的发布与管理
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_content (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    type                 VARCHAR(20)  NOT NULL COMMENT '类型: ANNOUNCEMENT公告/POLICY制度',
    title                VARCHAR(200) NOT NULL COMMENT '标题',
    category             VARCHAR(50)  NULL COMMENT '分类',
    body                 TEXT         NOT NULL COMMENT '正文',
    status               VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT草稿/PUBLISHED已发布/UNPUBLISHED已下架',
    scope                VARCHAR(50)  NULL DEFAULT 'ALL' COMMENT '可见范围: ALL全公司/DEPARTMENT指定部门',
    access_department_id BIGINT       NULL COMMENT '可见目标部门ID',
    publisher_id         BIGINT       NULL COMMENT '发布人用户ID',
    publish_time         DATETIME     NULL COMMENT '发布时间',
    version              INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    view_count           BIGINT       NOT NULL DEFAULT 0 COMMENT '访问次数（热点公告Redis缓存依据）',
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_status_type (status, type),
    KEY idx_publish_time (publish_time),
    KEY idx_view_count (view_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告制度';

-- ===================================================================
-- 10. RAG 企业知识来源表 -- AI知识库的原始文档
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_ai_source (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                VARCHAR(200) NOT NULL COMMENT '知识文档标题',
    category             VARCHAR(50)  NULL COMMENT '分类: 企业文化/新人培训/技术部资料/行政管理等',
    description          VARCHAR(500) NULL COMMENT '文档简介',
    original_file_name   VARCHAR(255) NOT NULL COMMENT '用户上传时的原文件名',
    stored_file_name     VARCHAR(255) NOT NULL COMMENT '系统随机化后的存储文件名',
    file_url             VARCHAR(500) NOT NULL COMMENT '文件路径（本地路径或OSS地址）',
    file_extension       VARCHAR(10)  NOT NULL COMMENT '文件扩展名: doc/docx/pdf',
    mime_type            VARCHAR(100) NOT NULL COMMENT '文件MIME类型',
    file_size            BIGINT UNSIGNED NOT NULL COMMENT '文件大小，单位字节',
    file_hash            CHAR(64)     NOT NULL COMMENT '文件SHA-256摘要，去重与完整性校验',
    storage_provider     VARCHAR(20)  NOT NULL DEFAULT 'LOCAL' COMMENT '存储方式: LOCAL本地/OSS阿里云',
    extracted_text       LONGTEXT     NULL COMMENT 'Word/PDF解析后的全文；用于分片重建',
    parse_status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '解析状态: PENDING待处理/PROCESSING处理中/SUCCESS成功/FAILED失败',
    index_status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '向量索引状态: PENDING待处理/PROCESSING处理中/SUCCESS成功/FAILED失败',
    error_message        VARCHAR(1000) NULL COMMENT '解析或索引失败的脱敏摘要',
    access_scope         VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT '访问范围: ALL全公司/DEPARTMENT指定部门',
    access_department_id VARCHAR(200) NULL COMMENT '可访问部门ID列表（逗号分隔，如"1,2,3"）',
    min_role_level       TINYINT      NOT NULL DEFAULT 1 COMMENT '最低角色等级: 1全员可见/2经理级以上/3仅管理员',
    status               VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '文档状态: ENABLED启用/DISABLED停用',
    version              INT          NOT NULL DEFAULT 1 COMMENT '知识文档版本号',
    uploader_id          BIGINT       NOT NULL COMMENT '上传人用户ID',
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_source_file_hash (file_hash),
    KEY idx_source_category_status (category, status),
    KEY idx_source_access (status, access_scope, access_department_id, min_role_level),
    KEY idx_source_uploader_time (uploader_id, create_time),
    KEY idx_source_parse_index_status (parse_status, index_status),
    CONSTRAINT chk_ai_source_file_extension CHECK (file_extension IN ('doc', 'docx', 'pdf')),
    CONSTRAINT chk_ai_source_storage_provider CHECK (storage_provider IN ('LOCAL', 'OSS')),
    CONSTRAINT chk_ai_source_access_scope CHECK (access_scope IN ('ALL', 'DEPARTMENT')),
    CONSTRAINT chk_ai_source_min_role_level CHECK (min_role_level BETWEEN 1 AND 3),
    CONSTRAINT chk_ai_source_status CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT chk_ai_source_parse_status CHECK (parse_status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_ai_source_index_status CHECK (index_status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG企业知识来源';

-- ===================================================================
-- 11. RAG 文本分片表 -- 文档解析后的分片与向量关联
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_ai_source_chunk (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id      BIGINT       NOT NULL COMMENT '所属知识来源ID，关联oa_ai_source.id',
    chunk_no       INT          NOT NULL COMMENT '文档内分片序号',
    section_title  VARCHAR(200) NULL COMMENT '章节标题或段落标题',
    page_no        INT          NULL COMMENT 'PDF页码（Word无明确页码时可为空）',
    content_text   LONGTEXT     NOT NULL COMMENT '分片正文',
    content_hash   CHAR(64)     NULL COMMENT '分片SHA-256摘要',
    token_count    INT          NULL COMMENT '分片Token数量（约1.5字符=1Token）',
    vector_key     VARCHAR(200) NULL COMMENT '向量库中的向量记录Key',
    metadata_json  JSON         NULL COMMENT '扩展元数据（标题层级、段落位置等）',
    status         VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED启用/DISABLED停用',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_source_chunk (source_id, chunk_no),
    KEY idx_chunk_source_status (source_id, status),
    KEY idx_chunk_vector_key (vector_key),
    KEY idx_chunk_content_hash (content_hash),
    CONSTRAINT chk_ai_source_chunk_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识文档分片';

-- ===================================================================
-- 12. AI 问答会话表 -- 用户每次AI提问的记录
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_ai_session (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id               BIGINT       NOT NULL COMMENT '用户ID',
    request_role          VARCHAR(30)  NULL COMMENT '提问时角色快照: SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE',
    request_department_id BIGINT       NULL COMMENT '提问时部门ID快照',
    permission_digest     VARCHAR(128) NULL COMMENT '角色+部门+权限版本的权限摘要（缓存隔离用）',
    question              TEXT         NOT NULL COMMENT '用户问题',
    answer_summary        TEXT         NULL COMMENT '答案摘要（前500字）',
    status                VARCHAR(20)  NOT NULL DEFAULT 'ANSWERED' COMMENT '状态: ANSWERED/REJECTED/DEGRADED/FAILED',
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, create_time),
    KEY idx_ai_session_permission (request_department_id, request_role, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答会话';

-- ===================================================================
-- 13. AI 回答引用记录表 -- AI回答引用了哪些知识来源
-- ===================================================================
CREATE TABLE IF NOT EXISTS oa_ai_citation (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id       BIGINT       NOT NULL COMMENT '问答会话ID',
    source_type      VARCHAR(20)  NOT NULL DEFAULT 'CONTENT' COMMENT '引用类型: CONTENT/KNOWLEDGE_FILE',
    content_id       BIGINT       NULL COMMENT '引用公告ID',
    source_id        BIGINT       NULL COMMENT '引用知识来源ID',
    chunk_id         BIGINT       NULL COMMENT '引用分片ID',
    source_title     VARCHAR(200) NULL COMMENT '引用标题快照',
    fragment_summary VARCHAR(500) NULL COMMENT '片段摘要',
    score            DOUBLE       NULL COMMENT '匹配分数',
    source_version   INT          NULL COMMENT '版本号',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_session_id (session_id),
    KEY idx_citation_source (source_id),
    KEY idx_citation_chunk (chunk_id),
    KEY idx_citation_session_type (session_id, source_type),
    CONSTRAINT chk_ai_citation_source_type CHECK (source_type IN ('CONTENT', 'KNOWLEDGE_FILE')),
    CONSTRAINT chk_ai_citation_source_ref CHECK (
        (source_type = 'CONTENT' AND content_id IS NOT NULL)
        OR (source_type = 'KNOWLEDGE_FILE' AND source_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI回答引用记录';

-- ===================================================================
-- 14. 操作日志表 -- 记录用户关键操作（审计用）
-- ===================================================================
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id     BIGINT       NULL COMMENT '操作人ID',
    module          VARCHAR(50)  NOT NULL COMMENT '操作模块',
    action          VARCHAR(100) NOT NULL COMMENT '操作动作',
    target_id       VARCHAR(50)  NULL COMMENT '目标ID',
    result          VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '结果: SUCCESS/FAILED',
    detail          VARCHAR(500) NULL COMMENT '操作详情',
    trace_id        VARCHAR(50)  NULL COMMENT '追踪标识',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operator_module (operator_id, module),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ===================================================================
-- 初始测试数据 (带有默认部门考勤规则与测试账号)
-- ===================================================================

-- 1. 总管理员账号 (用户名: admin, 初始密码: 123456, BCrypt加密)
INSERT IGNORE INTO sys_user (id, username, password_hash, role, status) VALUES
    (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'SUPER_ADMIN', 1);

-- 2. 部门初始数据
INSERT IGNORE INTO sys_department (id, code, name, status, sort) VALUES
    (1, 'DEPT_IT', '技术部', 1, 1),
    (2, 'DEPT_HR', '人事部', 1, 2),
    (3, 'DEPT_FIN', '财务部', 1, 3),
    (4, 'DEPT_ADMIN', '行政部', 1, 4);

-- 3. 员工初始档案 (工号以 Smart 开头)
INSERT IGNORE INTO oa_employee (id, employee_no, name, department_id, position, phone, status, hire_date) VALUES
    (1, 'Smart0001', '张三', 1, '技术部经理', '13800000001', 1, '2025-01-01'),
    (2, 'Smart0002', '李四', 1, 'Java开发工程师', '13800000002', 1, '2025-02-15'),
    (3, 'Smart0003', '王五', 2, '人事主管', '13800000003', 1, '2025-03-01'),
    (4, 'Smart0004', '赵六', 3, '财务专员', '13800000004', 1, '2025-03-10');

-- 更新部门负责人
UPDATE sys_department SET leader_id = 1 WHERE id = 1;
UPDATE sys_department SET leader_id = 3 WHERE id = 2;

-- 4. 员工系统账号 (初始密码统一为 123456)
INSERT IGNORE INTO sys_user (id, username, password_hash, role, status, employee_id) VALUES
    (2, 'Smart0001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'DEPT_MANAGER', 1, 1),
    (3, 'Smart0002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'EMPLOYEE', 1, 2),
    (4, 'Smart0003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'DEPT_MANAGER', 1, 3),
    (5, 'Smart0004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'EMPLOYEE', 1, 4);

-- 5. 各部门独立考勤规则初始数据 (包含上午场与下午场)
INSERT IGNORE INTO sys_department_attendance_rule
    (id, department_id, session_name, check_in_start_time, normal_check_in_end_time, check_in_end_time, normal_check_out_start_time, check_out_end_time, enabled)
VALUES
    (1, 1, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
    (2, 1, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1),
    (3, 2, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
    (4, 2, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1),
    (5, 3, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
    (6, 3, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1),
    (7, 4, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
    (8, 4, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1);

-- 6. 初始公告数据
INSERT IGNORE INTO oa_content (id, type, title, category, body, status, scope, publisher_id, publish_time) VALUES
    (1, 'ANNOUNCEMENT', '智办 AI OA 系统上线运行通知', '系统通知', '各位同事：智办 AI OA 管理系统已正式上线试运行。具备智能问答、多场次考勤、规则配置及员工批量导入功能，请大家及时熟悉使用。', 'PUBLISHED', 'ALL', 1, CURRENT_TIMESTAMP),
    (2, 'POLICY', '公司考勤管理规定及打卡规范', '规章制度', '一、工作日分为上午场与下午场打卡。\n二、上午场签到开放为 08:50 - 12:10，其中 08:50-09:10 为正常签到，09:10 后标记为迟到。\n三、正常签退时间早于 11:50 标记为早退。\n四、因公无法打卡请提交补签申请，由主管审批。', 'PUBLISHED', 'ALL', 1, CURRENT_TIMESTAMP);