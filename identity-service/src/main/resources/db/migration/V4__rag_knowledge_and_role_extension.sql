-- ==========================================================================
-- V4：用户角色扩展 (SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE) 与 RAG 企业知识库全套表结构
-- 使用存储过程确保脚本防重复执行（幂等性）
-- ==========================================================================

DROP PROCEDURE IF EXISTS `sp_v4_rag_knowledge_and_role_extension`;
DELIMITER //
CREATE PROCEDURE `sp_v4_rag_knowledge_and_role_extension`()
BEGIN
    DECLARE `_exists` INT DEFAULT 0;

    -- 1. 用户角色扩展 (sys_user)
    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'sys_user' AND `COLUMN_NAME` = 'role';
    IF `_exists` > 0 THEN
        ALTER TABLE `sys_user` MODIFY COLUMN `role` VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色：SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE';
    END IF;

    UPDATE `sys_user` SET `role` = 'SUPER_ADMIN' WHERE `id` > 0 AND `role` = 'ADMIN';

    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`STATISTICS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'sys_user' AND `INDEX_NAME` = 'idx_role_status';
    IF `_exists` = 0 THEN
        ALTER TABLE `sys_user` ADD INDEX `idx_role_status` (`role`, `status`);
    END IF;

    -- 2. 重命名与扩展引用表 oa_ai_source -> oa_ai_citation
    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`TABLES`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_source';
    IF `_exists` > 0 THEN
        RENAME TABLE `oa_ai_source` TO `oa_ai_citation`;
    END IF;

    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`TABLES`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_citation';
    IF `_exists` > 0 THEN
        ALTER TABLE `oa_ai_citation` COMMENT = 'AI回答引用记录';

        ALTER TABLE `oa_ai_citation` MODIFY COLUMN `content_id` BIGINT NULL COMMENT '引用公告/制度ID；source_type=CONTENT时使用';

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_citation' AND `COLUMN_NAME` = 'source_type';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_ai_citation` ADD COLUMN `source_type` VARCHAR(20) NOT NULL DEFAULT 'CONTENT' COMMENT '引用类型: CONTENT/KNOWLEDGE_FILE' AFTER `session_id`;
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_citation' AND `COLUMN_NAME` = 'source_id';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_ai_citation` ADD COLUMN `source_id` BIGINT NULL COMMENT '引用知识来源ID；source_type=KNOWLEDGE_FILE时使用' AFTER `content_id`;
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_citation' AND `COLUMN_NAME` = 'chunk_id';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_ai_citation` ADD COLUMN `chunk_id` BIGINT NULL COMMENT '引用的知识分片ID' AFTER `source_id`;
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_citation' AND `COLUMN_NAME` = 'source_title';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_ai_citation` ADD COLUMN `source_title` VARCHAR(200) NULL COMMENT '引用标题快照' AFTER `chunk_id`;
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_citation' AND `COLUMN_NAME` = 'source_version';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_ai_citation` ADD COLUMN `source_version` INT NULL COMMENT '引用时来源版本号' AFTER `score`;
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`STATISTICS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_citation' AND `INDEX_NAME` = 'idx_citation_source';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_ai_citation` ADD INDEX `idx_citation_source` (`source_id`);
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`STATISTICS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_citation' AND `INDEX_NAME` = 'idx_citation_chunk';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_ai_citation` ADD INDEX `idx_citation_chunk` (`chunk_id`);
        END IF;

        SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`STATISTICS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_citation' AND `INDEX_NAME` = 'idx_citation_session_type';
        IF `_exists` = 0 THEN
            ALTER TABLE `oa_ai_citation` ADD INDEX `idx_citation_session_type` (`session_id`, `source_type`);
        END IF;
    END IF;

    -- 3. AI问答会话增加权限快照 (oa_ai_session)
    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_session' AND `COLUMN_NAME` = 'request_role';
    IF `_exists` = 0 THEN
        ALTER TABLE `oa_ai_session` ADD COLUMN `request_role` VARCHAR(30) NULL COMMENT '提问时角色快照: SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE' AFTER `user_id`;
    END IF;

    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_session' AND `COLUMN_NAME` = 'request_department_id';
    IF `_exists` = 0 THEN
        ALTER TABLE `oa_ai_session` ADD COLUMN `request_department_id` BIGINT NULL COMMENT '提问时部门ID快照' AFTER `request_role`;
    END IF;

    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_session' AND `COLUMN_NAME` = 'permission_digest';
    IF `_exists` = 0 THEN
        ALTER TABLE `oa_ai_session` ADD COLUMN `permission_digest` VARCHAR(128) NULL COMMENT '角色、部门、权限版本生成的权限摘要，用于缓存隔离' AFTER `request_department_id`;
    END IF;

    SELECT COUNT(1) INTO `_exists` FROM `information_schema`.`STATISTICS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'oa_ai_session' AND `INDEX_NAME` = 'idx_ai_session_permission';
    IF `_exists` = 0 THEN
        ALTER TABLE `oa_ai_session` ADD INDEX `idx_ai_session_permission` (`request_department_id`, `request_role`, `create_time`);
    END IF;

END //
DELIMITER ;

CALL `sp_v4_rag_knowledge_and_role_extension`();
DROP PROCEDURE IF EXISTS `sp_v4_rag_knowledge_and_role_extension`;

-- 4. 新建 RAG 企业知识来源表 (oa_ai_source)
CREATE TABLE IF NOT EXISTS `oa_ai_source` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL COMMENT '知识文档标题',
    `category` VARCHAR(50) NULL COMMENT '分类: 企业文化/新人培训/技术部资料/行政管理等',
    `description` VARCHAR(500) NULL COMMENT '文档简介',

    `original_file_name` VARCHAR(255) NOT NULL COMMENT '用户上传时的原文件名',
    `stored_file_name` VARCHAR(255) NOT NULL COMMENT '系统随机化后的存储文件名',
    `file_url` VARCHAR(500) NOT NULL COMMENT '受控文件路径或OSS对象地址；不得直接公开访问',
    `file_extension` VARCHAR(10) NOT NULL COMMENT '文件扩展名: doc/docx/pdf',
    `mime_type` VARCHAR(100) NOT NULL COMMENT '文件MIME类型',
    `file_size` BIGINT UNSIGNED NOT NULL COMMENT '文件大小，单位字节',
    `file_hash` CHAR(64) NOT NULL COMMENT '文件SHA-256摘要，用于去重与完整性校验',
    `storage_provider` VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT '存储方式: LOCAL/OSS',

    `extracted_text` LONGTEXT NULL COMMENT 'Word/PDF解析后的全文；用于分片重建',
    `parse_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT '解析状态: PENDING/PROCESSING/SUCCESS/FAILED',
    `index_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT '向量索引状态: PENDING/PROCESSING/SUCCESS/FAILED',
    `error_message` VARCHAR(1000) NULL COMMENT '解析或索引失败的脱敏摘要',

    `access_scope` VARCHAR(20) NOT NULL DEFAULT 'ALL'
        COMMENT '访问范围: ALL-全公司/DEPARTMENT-指定部门',
    `access_department_id` BIGINT NULL
        COMMENT '可访问部门ID；access_scope=DEPARTMENT时必填',
    `min_role_level` TINYINT NOT NULL DEFAULT 1
        COMMENT '最低角色等级: 1-普通员工/2-部门经理/3-总管理员',

    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
        COMMENT '文档状态: ENABLED/DISABLED',
    `version` INT NOT NULL DEFAULT 1 COMMENT '知识文档版本号',
    `uploader_id` BIGINT NOT NULL COMMENT '上传人用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY `idx_source_file_hash` (`file_hash`),
    KEY `idx_source_category_status` (`category`, `status`),
    KEY `idx_source_access` (`status`, `access_scope`, `access_department_id`, `min_role_level`),
    KEY `idx_source_uploader_time` (`uploader_id`, `create_time`),
    KEY `idx_source_parse_index_status` (`parse_status`, `index_status`),

    CONSTRAINT `chk_ai_source_file_extension`
        CHECK (`file_extension` IN ('doc', 'docx', 'pdf')),
    CONSTRAINT `chk_ai_source_storage_provider`
        CHECK (`storage_provider` IN ('LOCAL', 'OSS')),
    CONSTRAINT `chk_ai_source_access_scope`
        CHECK (`access_scope` IN ('ALL', 'DEPARTMENT')),
    CONSTRAINT `chk_ai_source_access_department`
        CHECK (
            (`access_scope` = 'ALL' AND `access_department_id` IS NULL)
            OR
            (`access_scope` = 'DEPARTMENT' AND `access_department_id` IS NOT NULL)
        ),
    CONSTRAINT `chk_ai_source_min_role_level`
        CHECK (`min_role_level` BETWEEN 1 AND 3),
    CONSTRAINT `chk_ai_source_status`
        CHECK (`status` IN ('ENABLED', 'DISABLED')),
    CONSTRAINT `chk_ai_source_parse_status`
        CHECK (`parse_status` IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    CONSTRAINT `chk_ai_source_index_status`
        CHECK (`index_status` IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG企业知识来源';

-- 5. 新建 RAG 文本分片表 (oa_ai_source_chunk)
CREATE TABLE IF NOT EXISTS `oa_ai_source_chunk` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `source_id` BIGINT NOT NULL COMMENT '所属知识来源ID，逻辑关联oa_ai_source.id',
    `chunk_no` INT NOT NULL COMMENT '文档内分片序号，从0或1开始由程序统一',
    `section_title` VARCHAR(200) NULL COMMENT '章节标题或段落标题',
    `page_no` INT NULL COMMENT 'PDF页码；Word无明确页码时可为空',
    `content_text` LONGTEXT NOT NULL COMMENT '分片正文',
    `content_hash` CHAR(64) NULL COMMENT '分片SHA-256摘要',
    `token_count` INT NULL COMMENT '分片Token数量',
    `vector_key` VARCHAR(200) NULL COMMENT 'RedisStack/向量库中的向量记录Key',
    `metadata_json` JSON NULL COMMENT '扩展元数据，如标题层级、段落位置等',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY `uk_source_chunk` (`source_id`, `chunk_no`),
    KEY `idx_chunk_source_status` (`source_id`, `status`),
    KEY `idx_chunk_vector_key` (`vector_key`),
    KEY `idx_chunk_content_hash` (`content_hash`),

    CONSTRAINT `chk_ai_source_chunk_status`
        CHECK (`status` IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识文档分片';
