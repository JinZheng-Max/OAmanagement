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
