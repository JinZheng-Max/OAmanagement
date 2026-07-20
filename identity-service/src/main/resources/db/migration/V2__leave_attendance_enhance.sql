-- ================================================================
-- V2：请假附件表 + 考勤网络限制字段
-- 使用存储过程实现幂等性（重复执行不报错）
-- ================================================================

-- 1. 创建请假附件表（IF NOT EXISTS 确保幂等）
CREATE TABLE IF NOT EXISTS `oa_leave_attachment` (
    `id`          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `leave_id`    BIGINT       NULL     COMMENT '关联的请假申请ID（提交请假时关联）',
    `file_name`   VARCHAR(255) NOT NULL COMMENT '原始文件名（如: 医院证明.jpg）',
    `file_url`    VARCHAR(500) NOT NULL COMMENT '文件存储路径/URL（后续改为阿里云OSS地址）',
    `file_size`   BIGINT       NULL     COMMENT '文件大小（字节）',
    `mime_type`   VARCHAR(50)  NULL     COMMENT '文件类型（如: image/jpeg）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_leave_id` (`leave_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假附件（后续接入阿里云OSS）';

-- 2. 考勤表添加IP字段（使用存储过程判断字段是否存在，避免重复执行报错）
DROP PROCEDURE IF EXISTS `sp_add_attendance_ip_columns`;
DELIMITER //
CREATE PROCEDURE `sp_add_attendance_ip_columns`()
BEGIN
    DECLARE `_exists` INT DEFAULT 0;

    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'check_in_ip';
    IF `_exists` = 0 THEN
        ALTER TABLE `oa_attendance` ADD COLUMN `check_in_ip` VARCHAR(45) NULL COMMENT '签到时客户端IP' AFTER `check_out`;
    END IF;

    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_attendance' AND `COLUMN_NAME` = 'check_out_ip';
    IF `_exists` = 0 THEN
        ALTER TABLE `oa_attendance` ADD COLUMN `check_out_ip` VARCHAR(45) NULL COMMENT '签退时客户端IP' AFTER `check_in_ip`;
    END IF;
END //
DELIMITER ;
CALL `sp_add_attendance_ip_columns`();
DROP PROCEDURE IF EXISTS `sp_add_attendance_ip_columns`;
