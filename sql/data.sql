-- ===================================================================
-- 智办AI OA 数据库初始化脚本
-- 数据库: oa_db
-- ===================================================================

CREATE DATABASE IF NOT EXISTS oa_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oa_db;

-- ===== 1. 系统用户表 =====
CREATE TABLE IF NOT EXISTS sys_user (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password_hash  VARCHAR(200) NOT NULL COMMENT '密码摘要(BCrypt)',
    role           VARCHAR(30)  NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色: SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用',
    employee_id    BIGINT       NULL COMMENT '关联员工ID',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_employee_id (employee_id),
    KEY idx_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ===== 2. 部门表 =====
CREATE TABLE IF NOT EXISTS sys_department (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(50)  NOT NULL COMMENT '部门编码',
    name           VARCHAR(100) NOT NULL COMMENT '部门名称',
    leader_id      BIGINT       NULL COMMENT '部门负责人ID',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用',
    sort           INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

-- ===== 3. 员工档案表 =====
CREATE TABLE IF NOT EXISTS oa_employee (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_no     VARCHAR(50)  NOT NULL COMMENT '员工编号',
    name            VARCHAR(50)  NOT NULL COMMENT '员工姓名',
    department_id   BIGINT       NULL COMMENT '所属部门ID',
    position        VARCHAR(100) NULL COMMENT '职位',
    phone           VARCHAR(20)  NULL COMMENT '联系方式',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-在职 0-离职',
    hire_date       DATE         NULL COMMENT '入职日期',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_employee_no (employee_no),
    KEY idx_department (department_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工档案';

-- ===== 4. 考勤记录表 =====
CREATE TABLE IF NOT EXISTS oa_attendance (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT       NOT NULL COMMENT '员工ID',
    work_date       VARCHAR(10)  NOT NULL COMMENT '工作日期(yyyy-MM-dd)',
    check_in        DATETIME     NULL COMMENT '签到时间',
    check_out       DATETIME     NULL COMMENT '签退时间',
    check_in_ip     VARCHAR(45)  NULL COMMENT '签到时客户端IP（用于内网校验）',
    check_out_ip    VARCHAR(45)  NULL COMMENT '签退时客户端IP（用于内网校验）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'UNCHECKED' COMMENT '状态: UNCHECKED/CHECKED_IN/CHECKED_OUT',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_employee_date (employee_id, work_date),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录';

-- ===== 5. 请假申请表 =====
CREATE TABLE IF NOT EXISTS oa_leave (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id    BIGINT       NOT NULL COMMENT '申请人ID(员工ID)',
    type            VARCHAR(20)  NOT NULL COMMENT '请假类型: ANNUAL/SICK/PERSONAL/OTHER',
    start_time      DATETIME     NOT NULL COMMENT '开始时间',
    end_time        DATETIME     NOT NULL COMMENT '结束时间',
    reason          VARCHAR(500) NOT NULL COMMENT '请假原因',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/WITHDRAWN',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_applicant_status (applicant_id, status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请';

-- ===== 6. 请假附件表 =====
CREATE TABLE IF NOT EXISTS oa_leave_attachment (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    leave_id        BIGINT       NULL     COMMENT '关联的请假申请ID',
    file_name       VARCHAR(255) NOT NULL COMMENT '原始文件名（如: 医院证明.jpg）',
    file_url        VARCHAR(500) NOT NULL COMMENT '文件存储路径/URL（后续改为阿里云OSS地址）',
    file_size       BIGINT       NULL     COMMENT '文件大小（字节）',
    mime_type       VARCHAR(50)  NULL     COMMENT '文件类型（如: image/jpeg）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_leave_id (leave_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假附件';

-- ===== 7. 审批记录表 =====
CREATE TABLE IF NOT EXISTS oa_leave_audit (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_id        BIGINT       NOT NULL COMMENT '请假申请ID',
    auditor_id      BIGINT       NOT NULL COMMENT '审批人ID',
    action          VARCHAR(20)  NOT NULL COMMENT '审批动作: APPROVED/REJECTED',
    comment         VARCHAR(500) NULL COMMENT '审批意见',
    audit_time      DATETIME     NULL COMMENT '审批时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_leave_id (leave_id),
    KEY idx_auditor_id (auditor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录';

-- ===== 8. 公告/制度内容表 =====
CREATE TABLE IF NOT EXISTS oa_content (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL COMMENT '类型: ANNOUNCEMENT/POLICY',
    title           VARCHAR(200) NOT NULL COMMENT '标题',
    category        VARCHAR(50)  NULL COMMENT '分类',
    body            TEXT         NOT NULL COMMENT '正文',
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/UNPUBLISHED',
    scope           VARCHAR(50)  NULL DEFAULT 'ALL' COMMENT '可见范围: ALL/DEPARTMENT',
    publisher_id    BIGINT       NULL COMMENT '发布人ID',
    publish_time    DATETIME     NULL COMMENT '发布时间',
    version         INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    view_count      BIGINT       NOT NULL DEFAULT 0 COMMENT '访问次数/浏览量(热点公告Redis缓存依据)',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_status_type (status, type),
    KEY idx_publish_time (publish_time),
    KEY idx_view_count (view_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告制度';

-- ===== 9. RAG 企业知识来源表 =====
CREATE TABLE IF NOT EXISTS oa_ai_source (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                VARCHAR(200) NOT NULL COMMENT '知识文档标题',
    category             VARCHAR(50) NULL COMMENT '分类: 企业文化/新人培训/技术部资料/行政管理等',
    description          VARCHAR(500) NULL COMMENT '文档简介',
    original_file_name   VARCHAR(255) NOT NULL COMMENT '用户上传时的原文件名',
    stored_file_name     VARCHAR(255) NOT NULL COMMENT '系统随机化后的存储文件名',
    file_url             VARCHAR(500) NOT NULL COMMENT '受控文件路径或OSS对象地址；不得直接公开访问',
    file_extension       VARCHAR(10) NOT NULL COMMENT '文件扩展名: doc/docx/pdf',
    mime_type            VARCHAR(100) NOT NULL COMMENT '文件MIME类型',
    file_size            BIGINT UNSIGNED NOT NULL COMMENT '文件大小，单位字节',
    file_hash            CHAR(64) NOT NULL COMMENT '文件SHA-256摘要，用于去重与完整性校验',
    storage_provider     VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT '存储方式: LOCAL/OSS',
    extracted_text       LONGTEXT NULL COMMENT 'Word/PDF解析后的全文；用于分片重建',
    parse_status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态: PENDING/PROCESSING/SUCCESS/FAILED',
    index_status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '向量索引状态: PENDING/PROCESSING/SUCCESS/FAILED',
    error_message        VARCHAR(1000) NULL COMMENT '解析或索引失败的脱敏摘要',
    access_scope         VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '访问范围: ALL-全公司/DEPARTMENT-指定部门',
    access_department_id BIGINT NULL COMMENT '可访问部门ID；access_scope=DEPARTMENT时必填',
    min_role_level       TINYINT NOT NULL DEFAULT 1 COMMENT '最低角色等级: 1-普通员工/2-部门经理/3-总管理员',
    status               VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '文档状态: ENABLED/DISABLED',
    version              INT NOT NULL DEFAULT 1 COMMENT '知识文档版本号',
    uploader_id          BIGINT NOT NULL COMMENT '上传人用户ID',
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_source_file_hash (file_hash),
    KEY idx_source_category_status (category, status),
    KEY idx_source_access (status, access_scope, access_department_id, min_role_level),
    KEY idx_source_uploader_time (uploader_id, create_time),
    KEY idx_source_parse_index_status (parse_status, index_status),
    CONSTRAINT chk_ai_source_file_extension CHECK (file_extension IN ('doc', 'docx', 'pdf')),
    CONSTRAINT chk_ai_source_storage_provider CHECK (storage_provider IN ('LOCAL', 'OSS')),
    CONSTRAINT chk_ai_source_access_scope CHECK (access_scope IN ('ALL', 'DEPARTMENT')),
    CONSTRAINT chk_ai_source_access_department CHECK (
        (access_scope = 'ALL' AND access_department_id IS NULL) OR
        (access_scope = 'DEPARTMENT' AND access_department_id IS NOT NULL)
    ),
    CONSTRAINT chk_ai_source_min_role_level CHECK (min_role_level BETWEEN 1 AND 3),
    CONSTRAINT chk_ai_source_status CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT chk_ai_source_parse_status CHECK (parse_status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_ai_source_index_status CHECK (index_status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG企业知识来源';

-- ===== 10. RAG 文本分片表 =====
CREATE TABLE IF NOT EXISTS oa_ai_source_chunk (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id      BIGINT NOT NULL COMMENT '所属知识来源ID，逻辑关联oa_ai_source.id',
    chunk_no       INT NOT NULL COMMENT '文档内分片序号，从0或1开始由程序统一',
    section_title  VARCHAR(200) NULL COMMENT '章节标题或段落标题',
    page_no        INT NULL COMMENT 'PDF页码；Word无明确页码时可为空',
    content_text   LONGTEXT NOT NULL COMMENT '分片正文',
    content_hash   CHAR(64) NULL COMMENT '分片SHA-256摘要',
    token_count    INT NULL COMMENT '分片Token数量',
    vector_key     VARCHAR(200) NULL COMMENT 'RedisStack/向量库中的向量记录Key',
    metadata_json  JSON NULL COMMENT '扩展元数据，如标题层级、段落位置等',
    status         VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_source_chunk (source_id, chunk_no),
    KEY idx_chunk_source_status (source_id, status),
    KEY idx_chunk_vector_key (vector_key),
    KEY idx_chunk_content_hash (content_hash),
    CONSTRAINT chk_ai_source_chunk_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识文档分片';

-- ===== 11. AI 问答会话表 =====
CREATE TABLE IF NOT EXISTS oa_ai_session (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id               BIGINT       NOT NULL COMMENT '用户ID',
    request_role          VARCHAR(30)  NULL COMMENT '提问时角色快照: SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE',
    request_department_id BIGINT       NULL COMMENT '提问时部门ID快照',
    permission_digest     VARCHAR(128) NULL COMMENT '角色、部门、权限版本生成的权限摘要，用于缓存隔离',
    question              TEXT         NOT NULL COMMENT '用户问题',
    answer_summary        TEXT         NULL COMMENT '答案摘要',
    status                VARCHAR(20)  NOT NULL DEFAULT 'ANSWERED' COMMENT '状态: ANSWERED/REJECTED/DEGRADED/FAILED',
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, create_time),
    KEY idx_ai_session_permission (request_department_id, request_role, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答会话';

-- ===== 12. AI 回答引用记录表 =====
CREATE TABLE IF NOT EXISTS oa_ai_citation (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id       BIGINT       NOT NULL COMMENT '问答会话ID',
    source_type      VARCHAR(20)  NOT NULL DEFAULT 'CONTENT' COMMENT '引用类型: CONTENT/KNOWLEDGE_FILE',
    content_id       BIGINT       NULL COMMENT '引用公告/制度ID；source_type=CONTENT时使用',
    source_id        BIGINT       NULL COMMENT '引用知识来源ID；source_type=KNOWLEDGE_FILE时使用',
    chunk_id         BIGINT       NULL COMMENT '引用的知识分片ID',
    source_title     VARCHAR(200) NULL COMMENT '引用标题快照',
    fragment_summary VARCHAR(500) NULL COMMENT '片段摘要',
    score            DOUBLE       NULL COMMENT '匹配分数',
    source_version   INT          NULL COMMENT '引用时来源版本号',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_session_id (session_id),
    KEY idx_citation_source (source_id),
    KEY idx_citation_chunk (chunk_id),
    KEY idx_citation_session_type (session_id, source_type),
    CONSTRAINT chk_ai_citation_source_type CHECK (source_type IN ('CONTENT', 'KNOWLEDGE_FILE')),
    CONSTRAINT chk_ai_citation_source_ref CHECK (
        (source_type = 'CONTENT' AND content_id IS NOT NULL) OR
        (source_type = 'KNOWLEDGE_FILE' AND source_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI回答引用记录';

-- ===== 13. 操作日志表 =====
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

-- ===== 初始测试数据 =====

-- 总管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO sys_user (username, password_hash, role, status) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'SUPER_ADMIN', 1);

-- 部门
INSERT INTO sys_department (code, name, status, sort) VALUES
    ('DEPT_IT', '技术部', 1, 1),
    ('DEPT_HR', '人事部', 1, 2),
    ('DEPT_FIN', '财务部', 1, 3),
    ('DEPT_ADMIN', '行政部', 1, 4);

-- 员工
INSERT INTO oa_employee (employee_no, name, department_id, position, phone, status) VALUES
    ('EMP001', '张三', 1, '技术经理', '13800000001', 1),
    ('EMP002', '李四', 1, 'Java开发工程师', '13800000002', 1),
    ('EMP003', '王五', 2, 'HR主管', '13800000003', 1),
    ('EMP004', '赵六', 3, '财务专员', '13800000004', 1);

-- 员工账号 (密码: emp123)
INSERT INTO sys_user (username, password_hash, role, status, employee_id) VALUES
    ('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'DEPT_MANAGER', 1, 1),
    ('lisi',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'EMPLOYEE', 1, 2),
    ('wangwu',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'EMPLOYEE', 1, 3),
    ('zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'EMPLOYEE', 1, 4);