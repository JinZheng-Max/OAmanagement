-- ================================================================
-- 修复 oa_attendance 唯一约束：升级为多场次支持
-- 兼容 MySQL 5.7 / 8.0 所有版本
-- ================================================================

USE oa_db;

-- 通过存储过程安全删除旧约束（兼容全版本）
DROP PROCEDURE IF EXISTS `sp_fix_attendance_unique_key`;

DELIMITER //
CREATE PROCEDURE `sp_fix_attendance_unique_key`()
BEGIN
    -- 删除旧约束 uk_employee_date（若存在）
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'oa_attendance'
          AND INDEX_NAME = 'uk_employee_date'
    ) THEN
        ALTER TABLE `oa_attendance` DROP INDEX `uk_employee_date`;
    END IF;

    -- 删除新约束 uk_emp_date_session（若已存在则先删再建）
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'oa_attendance'
          AND INDEX_NAME = 'uk_emp_date_session'
    ) THEN
        ALTER TABLE `oa_attendance` DROP INDEX `uk_emp_date_session`;
    END IF;

    -- 添加正确的三列唯一约束（支持多场次）
    ALTER TABLE `oa_attendance`
        ADD UNIQUE KEY `uk_emp_date_session` (`employee_id`, `work_date`, `session_name`);

    SELECT 'oa_attendance 唯一约束修复完成！' AS result;
END //
DELIMITER ;

CALL `sp_fix_attendance_unique_key`();
DROP PROCEDURE IF EXISTS `sp_fix_attendance_unique_key`;
