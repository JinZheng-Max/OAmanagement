-- ===================================================================
-- 智办AI OA 数据库增量升级与结构修复脚本 (全版本全功能安全幂等)
-- 适用数据库: oa_db
-- 编制日期: 2026-07-24
-- 【重要提示】：若在 Navicat / DBeaver 中运行本文件，请务必【全选(Ctrl+A)后运行】，切勿仅单独高亮选中某一行 IF 语句。
-- ===================================================================

USE oa_db;

DROP PROCEDURE IF EXISTS `sp_upgrade_oa_db`;

DELIMITER //

CREATE PROCEDURE `sp_upgrade_oa_db`()
BEGIN
    -- -------------------------------------------------------------------
    -- 1. 扩充 sys_user 角色字段与迁移旧角色 ADMIN -> SUPER_ADMIN
    -- -------------------------------------------------------------------
    ALTER TABLE `sys_user` MODIFY COLUMN `role` VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色: SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE';
    
    UPDATE `sys_user` SET `role` = 'SUPER_ADMIN' WHERE `role` = 'ADMIN' AND `id` > 0;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'idx_role_status'
    ) THEN
        ALTER TABLE `sys_user` ADD INDEX `idx_role_status` (`role`, `status`);
    END IF;

    -- -------------------------------------------------------------------
    -- 1.5 补齐 oa_employee 扩展字段 (身份证号、电子邮箱、工作年限)
    -- -------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_employee' AND COLUMN_NAME = 'id_number'
    ) THEN
        ALTER TABLE `oa_employee` ADD COLUMN `id_number` VARCHAR(18) NULL COMMENT '身份证号（18位）' AFTER `phone`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_employee' AND COLUMN_NAME = 'email'
    ) THEN
        ALTER TABLE `oa_employee` ADD COLUMN `email` VARCHAR(100) NULL COMMENT '电子邮箱' AFTER `id_number`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_employee' AND COLUMN_NAME = 'work_years'
    ) THEN
        ALTER TABLE `oa_employee` ADD COLUMN `work_years` DECIMAL(4,1) NULL DEFAULT 0 COMMENT '工作年限（年）' AFTER `hire_date`;
    END IF;

    -- -------------------------------------------------------------------
    -- 2. 补齐 oa_content 浏览量与可见部门扩展字段
    -- -------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_content' AND COLUMN_NAME = 'view_count'
    ) THEN
        ALTER TABLE `oa_content` ADD COLUMN `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '访问次数/浏览量(热点数据Redis依据)' AFTER `version`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_content' AND COLUMN_NAME = 'access_department_id'
    ) THEN
        ALTER TABLE `oa_content` ADD COLUMN `access_department_id` BIGINT NULL COMMENT '可见目标部门ID' AFTER `scope`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_content' AND INDEX_NAME = 'idx_view_count'
    ) THEN
        ALTER TABLE `oa_content` ADD INDEX `idx_view_count` (`view_count`);
    END IF;

    -- -------------------------------------------------------------------
    -- 3. 创建/补齐 部门独立考勤规则配置表 sys_department_attendance_rule
    -- -------------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS `sys_department_attendance_rule` (
        `id`                          BIGINT AUTO_INCREMENT PRIMARY KEY,
        `department_id`               BIGINT       NOT NULL COMMENT '所属部门ID',
        `session_name`                VARCHAR(50)  NOT NULL COMMENT '考勤场次名称(如 上午场, 下午场)',
        `check_in_start_time`         VARCHAR(10)  NOT NULL COMMENT '签到允许开始时间(HH:mm)',
        `normal_check_in_end_time`    VARCHAR(10)  NOT NULL COMMENT '正常签到截止时间(HH:mm, 超过则计迟到)',
        `check_in_end_time`           VARCHAR(10)  NOT NULL COMMENT '签到最晚截止时间(HH:mm)',
        `normal_check_out_start_time` VARCHAR(10)  NOT NULL COMMENT '正常签退最早时间(HH:mm, 早于则计早退)',
        `check_out_end_time`          VARCHAR(10)  NOT NULL COMMENT '签退最晚截止时间(HH:mm)',
        `enabled`                     INT          NOT NULL DEFAULT 1 COMMENT '规则启用状态 (1-启用 0-禁用)',
        `create_time`                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        `update_time`                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        UNIQUE KEY `uk_dept_session` (`department_id`, `session_name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门独立考勤规则配置表';

    -- -------------------------------------------------------------------
    -- 4. 动态升级 oa_attendance 考勤表字段与唯一索引 (支持多场次与 Flowable 补签)
    -- -------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_attendance' AND COLUMN_NAME = 'department_id'
    ) THEN
        ALTER TABLE `oa_attendance` ADD COLUMN `department_id` BIGINT NULL COMMENT '所属部门ID' AFTER `employee_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_attendance' AND COLUMN_NAME = 'session_name'
    ) THEN
        ALTER TABLE `oa_attendance` ADD COLUMN `session_name` VARCHAR(50) NOT NULL DEFAULT '默认场次' COMMENT '考勤场次名称' AFTER `work_date`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_attendance' AND COLUMN_NAME = 'check_in_start_time'
    ) THEN
        ALTER TABLE `oa_attendance` 
            ADD COLUMN `check_in_start_time` VARCHAR(10) NULL COMMENT '签到允许开始时间(HH:mm)' AFTER `check_out_ip`,
            ADD COLUMN `normal_check_in_end_time` VARCHAR(10) NULL COMMENT '正常签到截止时间(HH:mm)' AFTER `check_in_start_time`,
            ADD COLUMN `check_in_end_time` VARCHAR(10) NULL COMMENT '签到最晚截止时间(HH:mm)' AFTER `normal_check_in_end_time`,
            ADD COLUMN `normal_check_out_start_time` VARCHAR(10) NULL COMMENT '正常签退最早时间(HH:mm)' AFTER `check_in_end_time`,
            ADD COLUMN `check_out_end_time` VARCHAR(10) NULL COMMENT '签退最晚截止时间(HH:mm)' AFTER `normal_check_out_start_time`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_attendance' AND COLUMN_NAME = 'replenish_status'
    ) THEN
        ALTER TABLE `oa_attendance` 
            ADD COLUMN `replenish_status` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '补签状态: NONE/PENDING/APPROVED/REJECTED' AFTER `status`,
            ADD COLUMN `replenish_reason` VARCHAR(500) NULL COMMENT '补签原因' AFTER `replenish_status`,
            ADD COLUMN `approver_id` BIGINT NULL COMMENT '审批人用户ID' AFTER `replenish_reason`,
            ADD COLUMN `approve_time` DATETIME NULL COMMENT '审批时间' AFTER `approver_id`,
            ADD COLUMN `approve_comment` VARCHAR(500) NULL COMMENT '审批意见' AFTER `approve_time`;
    END IF;

    -- 安全替换约束：删除旧单场次 uk_employee_date，添加多场次 uk_emp_date_session
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_attendance' AND INDEX_NAME = 'uk_employee_date'
    ) THEN
        ALTER TABLE `oa_attendance` DROP INDEX `uk_employee_date`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_attendance' AND INDEX_NAME = 'uk_emp_date_session'
    ) THEN
        ALTER TABLE `oa_attendance` ADD UNIQUE KEY `uk_emp_date_session` (`employee_id`, `work_date`, `session_name`);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_attendance' AND INDEX_NAME = 'idx_replenish_status'
    ) THEN
        ALTER TABLE `oa_attendance` ADD INDEX `idx_replenish_status` (`replenish_status`);
    END IF;

    -- -------------------------------------------------------------------
    -- 5. RAG 向量与 AI 问答结构同步补齐
    -- -------------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS `oa_ai_source` (
        `id`                   BIGINT AUTO_INCREMENT PRIMARY KEY,
        `title`                VARCHAR(200) NOT NULL COMMENT '知识文档标题',
        `category`             VARCHAR(50)  NULL COMMENT '分类',
        `description`          VARCHAR(500) NULL COMMENT '文档简介',
        `original_file_name`   VARCHAR(255) NOT NULL COMMENT '原文件名',
        `stored_file_name`     VARCHAR(255) NOT NULL COMMENT '存储文件名',
        `file_url`             VARCHAR(500) NOT NULL COMMENT '文件路径',
        `file_extension`       VARCHAR(10)  NOT NULL COMMENT '扩展名',
        `mime_type`            VARCHAR(100) NOT NULL COMMENT 'MIME',
        `file_size`            BIGINT UNSIGNED NOT NULL COMMENT '字节大小',
        `file_hash`            CHAR(64)     NOT NULL COMMENT '文件哈希',
        `storage_provider`     VARCHAR(20)  NOT NULL DEFAULT 'LOCAL' COMMENT '存储方式',
        `extracted_text`       LONGTEXT     NULL COMMENT '解析全文',
        `parse_status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
        `index_status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
        `error_message`        VARCHAR(1000) NULL,
        `access_scope`         VARCHAR(20)  NOT NULL DEFAULT 'ALL',
        `access_department_id` VARCHAR(200) NULL,
        `min_role_level`       TINYINT      NOT NULL DEFAULT 1,
        `status`               VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
        `version`              INT          NOT NULL DEFAULT 1,
        `uploader_id`          BIGINT       NOT NULL COMMENT '上传人用户ID',
        `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        KEY `idx_source_file_hash` (`file_hash`),
        KEY `idx_source_category_status` (`category`, `status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG企业知识来源';

    CREATE TABLE IF NOT EXISTS `oa_ai_source_chunk` (
        `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
        `source_id`      BIGINT       NOT NULL COMMENT '所属知识来源ID',
        `chunk_no`       INT          NOT NULL COMMENT '分片序号',
        `section_title`  VARCHAR(200) NULL,
        `page_no`        INT          NULL,
        `content_text`   LONGTEXT     NOT NULL COMMENT '正文',
        `content_hash`   CHAR(64)     NULL,
        `token_count`    INT          NULL,
        `vector_key`     VARCHAR(200) NULL,
        `metadata_json`  JSON         NULL,
        `status`         VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
        `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY `uk_source_chunk` (`source_id`, `chunk_no`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识文档分片';

    -- -------------------------------------------------------------------
    -- 6. 默认初始化基础规则数据（如尚未导入）
    -- -------------------------------------------------------------------
    INSERT IGNORE INTO `sys_department_attendance_rule`
        (`id`, `department_id`, `session_name`, `check_in_start_time`, `normal_check_in_end_time`, `check_in_end_time`, `normal_check_out_start_time`, `check_out_end_time`, `enabled`)
    VALUES
        (1, 1, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
        (2, 1, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1),
        (3, 2, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
        (4, 2, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1),
        (5, 3, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
        (6, 3, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1),
        (7, 4, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
        (8, 4, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1);

    SELECT '智办AI OA 数据库增量结构与规则初始化成功！' AS result;
END //

DELIMITER ;

-- 执行存储过程
CALL `sp_upgrade_oa_db`();

-- 执行后清理临时存储过程
DROP PROCEDURE IF EXISTS `sp_upgrade_oa_db`;
