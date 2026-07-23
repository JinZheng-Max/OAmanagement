-- ================================================================
-- V6：部门独立考勤规则表 与 考勤表防重/快照/补签扩展
-- ================================================================

-- 1. 创建 部门考勤规则表
CREATE TABLE IF NOT EXISTS `sys_department_attendance_rule` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `department_id` BIGINT NOT NULL COMMENT '所属部门ID',
    `session_name` VARCHAR(50) NOT NULL COMMENT '考勤场次名称(如 上午场, 下午场)',
    `check_in_start_time` VARCHAR(10) NOT NULL COMMENT '签到允许开始时间(HH:mm)',
    `normal_check_in_end_time` VARCHAR(10) NOT NULL COMMENT '正常签到截止时间(HH:mm, 超过则计迟到)',
    `check_in_end_time` VARCHAR(10) NOT NULL COMMENT '签到最晚截止时间(HH:mm)',
    `normal_check_out_start_time` VARCHAR(10) NOT NULL COMMENT '正常签退最早时间(HH:mm, 早于则计早退)',
    `check_out_end_time` VARCHAR(10) NOT NULL COMMENT '签退最晚截止时间(HH:mm)',
    `enabled` INT DEFAULT 1 COMMENT '规则启用状态 (1-启用 0-禁用)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_dept_session` (`department_id`, `session_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门独立考勤规则配置表';

-- 2. 为 oa_attendance 扩展列与防重快照
DROP PROCEDURE IF EXISTS `sp_enhance_attendance_table_v6`;
DELIMITER //
CREATE PROCEDURE `sp_enhance_attendance_table_v6`()
BEGIN
    DECLARE `_tbl_exists` INT DEFAULT 0;
    DECLARE `_exists` INT DEFAULT 0;

    SELECT COUNT(1) INTO `_tbl_exists` FROM `information_schema`.`TABLES`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance';

    IF `_tbl_exists` > 0 THEN
        -- 部门ID
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'department_id';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `department_id` BIGINT NULL COMMENT '部门ID' AFTER `employee_id`;
        END IF;

        -- 场次名称
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'session_name';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `session_name` VARCHAR(50) DEFAULT '默认场次' COMMENT '考勤场次名称(如 上午场, 下午场)' AFTER `work_date`;
        END IF;

        -- 正常签到截止时间
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'normal_check_in_end_time';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `normal_check_in_end_time` VARCHAR(10) NULL COMMENT '正常签到截止时间(HH:mm)' AFTER `check_in_start_time`;
        END IF;

        -- 正常签退最早时间
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'normal_check_out_start_time';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `normal_check_out_start_time` VARCHAR(10) NULL COMMENT '正常签退最早时间(HH:mm)' AFTER `check_in_end_time`;
        END IF;

        -- 补签状态
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'replenish_status';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `replenish_status` VARCHAR(20) DEFAULT 'NONE' COMMENT '补签状态(NONE/PENDING/APPROVED/REJECTED)' AFTER `status`;
        END IF;

        -- 补签原因
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'replenish_reason';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `replenish_reason` VARCHAR(500) NULL COMMENT '补签申请原因' AFTER `replenish_status`;
        END IF;

        -- 审批人ID
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'approver_id';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `approver_id` BIGINT NULL COMMENT '补签审批人用户ID' AFTER `replenish_reason`;
        END IF;

        -- 审批时间
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'approve_time';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `approve_time` DATETIME NULL COMMENT '补签审批时间' AFTER `approver_id`;
        END IF;

        -- 审批意见
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'approve_comment';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `approve_comment` VARCHAR(500) NULL COMMENT '补签审批意见' AFTER `approve_time`;
        END IF;

        -- 升级唯一索引为 (employee_id, work_date, session_name)
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`STATISTICS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `INDEX_NAME` = 'uk_employee_date';
        IF `_exists` > 0 THEN
            ALTER TABLE `oa_attendance` DROP INDEX `uk_employee_date`;
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`STATISTICS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `INDEX_NAME` = 'uk_emp_date_session';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD UNIQUE KEY `uk_emp_date_session` (`employee_id`, `work_date`, `session_name`);
        END IF;
    END IF;
END //
DELIMITER ;
CALL `sp_enhance_attendance_table_v6`();
DROP PROCEDURE IF EXISTS `sp_enhance_attendance_table_v6`;

-- 3. 填充基础考勤规则（为部门1-4预设上午场与下午场）
INSERT IGNORE INTO `sys_department_attendance_rule`
    (`department_id`, `session_name`, `check_in_start_time`, `normal_check_in_end_time`, `check_in_end_time`, `normal_check_out_start_time`, `check_out_end_time`, `enabled`)
VALUES
    (1, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
    (1, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1),
    (2, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
    (2, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1),
    (3, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
    (3, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1),
    (4, '上午场', '08:50', '09:10', '12:10', '11:50', '12:10', 1),
    (4, '下午场', '13:50', '14:10', '17:10', '16:50', '17:10', 1);
