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
    role           VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色: ADMIN/EMPLOYEE',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用',
    employee_id    BIGINT       NULL COMMENT '关联员工ID',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_employee_id (employee_id)
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

-- ===== 6. 请假附件表（后续接入阿里云OSS）=====
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

-- ===== 7. 公告/制度内容表 =====
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
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_status_type (status, type),
    KEY idx_publish_time (publish_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告制度';

-- ===== 8. AI 问答会话表 =====
CREATE TABLE IF NOT EXISTS oa_ai_session (
                                             id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             user_id         BIGINT       NOT NULL COMMENT '用户ID',
                                             question        TEXT         NOT NULL COMMENT '用户问题',
                                             answer_summary  TEXT         NULL COMMENT '答案摘要',
                                             status          VARCHAR(20)  NOT NULL DEFAULT 'ANSWERED' COMMENT '状态: ANSWERED/REJECTED/DEGRADED/FAILED',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答会话';

-- ===== 9. AI 答案来源表 =====
CREATE TABLE IF NOT EXISTS oa_ai_source (
                                            id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            session_id      BIGINT       NOT NULL COMMENT '问答会话ID',
                                            content_id      BIGINT       NOT NULL COMMENT '内容ID',
                                            fragment_summary VARCHAR(500) NULL COMMENT '片段摘要',
    score           DOUBLE       NULL COMMENT '匹配分数',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_session_id (session_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答来源';

-- ===== 10. 操作日志表 =====
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

-- 管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO sys_user (username, password_hash, role, status) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'ADMIN', 1);

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
                                                                              ('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'EMPLOYEE', 1, 1),
                                                                              ('lisi',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'EMPLOYEE', 1, 2),
                                                                              ('wangwu',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'EMPLOYEE', 1, 3),
                                                                              ('zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'EMPLOYEE', 1, 4);