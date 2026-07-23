-- ================================================================
-- V5：考勤表增加指定打卡时间段字段 (check_in_start_time, check_in_end_time, check_out_start_time, check_out_end_time)
-- ================================================================

DROP PROCEDURE IF EXISTS `sp_add_attendance_window_columns`;
DELIMITER //
CREATE PROCEDURE `sp_add_attendance_window_columns`()
BEGIN
    DECLARE `_tbl_exists` INT DEFAULT 0;
    DECLARE `_exists` INT DEFAULT 0;

    SELECT COUNT(1) INTO `_tbl_exists` FROM `information_schema`.`TABLES`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance';

    IF `_tbl_exists` > 0 THEN
        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'check_in_start_time';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `check_in_start_time` VARCHAR(10) NULL COMMENT '签到开始时间(HH:mm)' AFTER `check_out_ip`;
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'check_in_end_time';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `check_in_end_time` VARCHAR(10) NULL COMMENT '签到截止时间(HH:mm)' AFTER `check_in_start_time`;
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'check_out_start_time';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `check_out_start_time` VARCHAR(10) NULL COMMENT '签退开始时间(HH:mm)' AFTER `check_in_end_time`;
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'check_out_end_time';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_attendance` ADD COLUMN `check_out_end_time` VARCHAR(10) NULL COMMENT '签退截止时间(HH:mm)' AFTER `check_out_start_time`;
        END IF;
    END IF;
END //
DELIMITER ;
CALL `sp_add_attendance_window_columns`();
DROP PROCEDURE IF EXISTS `sp_add_attendance_window_columns`;
