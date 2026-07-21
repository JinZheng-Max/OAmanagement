-- ===================================================================
-- 公告制度表 (oa_content) 增加访问次数 (view_count) 增量更新脚本
-- 适用场景：已部署环境直接在数据库手动执行
-- ===================================================================

USE oa_db;

-- 1. 添加访问次数 view_count 字段 (默认值为 0)
ALTER TABLE `oa_content` 
ADD COLUMN `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '访问次数/浏览量(热点公告Redis缓存依据)' AFTER `version`;

-- 2. 为 view_count 创建索引，加速根据热度/访问量降序查询
ALTER TABLE `oa_content` 
ADD INDEX `idx_view_count` (`view_count`);
