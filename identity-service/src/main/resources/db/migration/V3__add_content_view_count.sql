-- ================================================================
-- V3：公告/制度表 (oa_content) 添加访问次数 (view_count) 字段与索引
-- 目的：用于热点公告放入 Redis 缓存依据 (根据 view_count 降序)
-- 使用存储过程保证脚本执行的幂等性
-- ================================================================

DROP PROCEDURE IF EXISTS `sp_add_content_view_count`;
DELIMITER //
CREATE PROCEDURE `sp_add_content_view_count`()
BEGIN
    DECLARE `_column_exists` INT DEFAULT 0;
    DECLARE `_index_exists` INT DEFAULT 0;

    -- 1. 校验 view_count 字段是否存在，若不存在则添加
    SELECT COUNT(1) INTO `_column_exists` FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE()
          AND `TABLE_NAME` = 'oa_content'
          AND `COLUMN_NAME` = 'view_count';

    IF `_column_exists` = 0 THEN
        ALTER TABLE `oa_content`
        ADD COLUMN `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '访问次数/浏览量(热点公告Redis缓存依据)' AFTER `version`;
    END IF;

    -- 2. 校验 idx_view_count 索引是否存在，若不存在则添加
    SELECT COUNT(1) INTO `_index_exists` FROM `information_schema`.`STATISTICS`
        WHERE `TABLE_SCHEMA` = DATABASE()
          AND `TABLE_NAME` = 'oa_content'
          AND `INDEX_NAME` = 'idx_view_count';

    IF `_index_exists` = 0 THEN
        ALTER TABLE `oa_content`
        ADD INDEX `idx_view_count` (`view_count`);
    END IF;
END //
DELIMITER ;

CALL `sp_add_content_view_count`();
DROP PROCEDURE IF EXISTS `sp_add_content_view_count`;
