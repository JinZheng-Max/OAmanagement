-- 本次升级内容：
--   A. 用户角色扩展：SUPER_ADMIN / DEPT_MANAGER / EMPLOYEE；
--   B. 原 oa_ai_source 重命名为 oa_ai_citation，继续保存每次回答的引用；
--   C. 新建 oa_ai_source，作为 Word/PDF 企业知识来源表；
--   D. 新建 oa_ai_source_chunk，保存解析后的 RAG 文本分片；
--   E. oa_ai_session 增加提问时角色、部门与权限摘要快照。
--   F：公告添加访问次数字段

USE oa_db;

-- 1. 添加访问次数 view_count 字段 (默认值为 0)
ALTER TABLE `oa_content` 
ADD COLUMN `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '访问次数/浏览量(热点公告Redis缓存依据)' AFTER `version`;

-- 2. 为 view_count 创建索引，加速根据热度/访问量降序查询
ALTER TABLE `oa_content` 
ADD INDEX `idx_view_count` (`view_count`);


-- ==========================================================================
-- 1. 用户角色扩展
-- ==========================================================================

-- 角色说明：
-- SUPER_ADMIN：总管理员，可访问全部启用的知识来源；
-- DEPT_MANAGER：部门经理，可访问全公司公开资料及本人部门授权资料；
-- EMPLOYEE：普通员工，可访问全公司公开资料及本人部门普通资料。
-- 1. 扩展角色字段长度
ALTER TABLE `sys_user`
MODIFY COLUMN `role` VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE'
COMMENT '角色：SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE';

-- 2. 查看旧管理员
SELECT `id`, `username`, `role`
FROM `sys_user`
WHERE `role` = 'ADMIN';

-- 3. 将旧管理员迁移为总管理员
UPDATE `sys_user`
SET `role` = 'SUPER_ADMIN'
WHERE `id` > 0
  AND `role` = 'ADMIN';

-- 4. 创建角色与状态组合索引
ALTER TABLE `sys_user`
ADD INDEX `idx_role_status` (`role`, `status`);

-- 5. 检查迁移结果
SELECT `id`, `username`, `role`, `status`
FROM `sys_user`;


-- ==========================================================================
-- 2. 保留原 AI 回答来源记录：oa_ai_source -> oa_ai_citation
-- ==========================================================================

-- 原 oa_ai_source 保存 session_id、content_id、fragment_summary、score，
-- 实际语义是“某次回答引用了什么”，因此重命名为引用记录表。
RENAME TABLE `oa_ai_source` TO `oa_ai_citation`;

ALTER TABLE `oa_ai_citation`
    COMMENT = 'AI回答引用记录';

-- 允许引用两类来源：
-- CONTENT：公告/制度 oa_content；
-- KNOWLEDGE_FILE：企业上传知识文件 oa_ai_source。
ALTER TABLE `oa_ai_citation`
    MODIFY COLUMN `content_id` BIGINT NULL COMMENT '引用公告/制度ID；source_type=CONTENT时使用',
    ADD COLUMN `source_type` VARCHAR(20) NOT NULL DEFAULT 'CONTENT'
        COMMENT '引用类型: CONTENT/KNOWLEDGE_FILE' AFTER `session_id`,
    ADD COLUMN `source_id` BIGINT NULL
        COMMENT '引用知识来源ID；source_type=KNOWLEDGE_FILE时使用' AFTER `content_id`,
    ADD COLUMN `chunk_id` BIGINT NULL
        COMMENT '引用的知识分片ID' AFTER `source_id`,
    ADD COLUMN `source_title` VARCHAR(200) NULL
        COMMENT '引用标题快照' AFTER `chunk_id`,
    ADD COLUMN `source_version` INT NULL
        COMMENT '引用时来源版本号' AFTER `score`,
    ADD INDEX `idx_citation_source` (`source_id`),
    ADD INDEX `idx_citation_chunk` (`chunk_id`),
    ADD INDEX `idx_citation_session_type` (`session_id`, `source_type`),
    ADD CONSTRAINT `chk_ai_citation_source_type`
        CHECK (`source_type` IN ('CONTENT', 'KNOWLEDGE_FILE')),
    ADD CONSTRAINT `chk_ai_citation_source_ref`
        CHECK (
            (`source_type` = 'CONTENT' AND `content_id` IS NOT NULL)
            OR
            (`source_type` = 'KNOWLEDGE_FILE' AND `source_id` IS NOT NULL)
        );

-- ==========================================================================
-- 3. 新建 RAG 企业知识来源表 oa_ai_source
-- ==========================================================================

CREATE TABLE `oa_ai_source` (
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

-- ==========================================================================
-- 4. 新建 RAG 文本分片表 oa_ai_source_chunk
-- ==========================================================================

CREATE TABLE `oa_ai_source_chunk` (
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

-- ==========================================================================
-- 5. AI问答会话增加权限快照
-- ==========================================================================

-- 保存提问发生时的身份快照，便于审计、缓存隔离和问题复现。
-- 当前用户部门仍以 sys_user.employee_id -> oa_employee.department_id 为权威关系。
ALTER TABLE `oa_ai_session`
    ADD COLUMN `request_role` VARCHAR(30) NULL
        COMMENT '提问时角色快照: SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE' AFTER `user_id`,
    ADD COLUMN `request_department_id` BIGINT NULL
        COMMENT '提问时部门ID快照' AFTER `request_role`,
    ADD COLUMN `permission_digest` VARCHAR(128) NULL
        COMMENT '角色、部门、权限版本生成的权限摘要，用于缓存隔离' AFTER `request_department_id`,
    ADD INDEX `idx_ai_session_permission` (`request_department_id`, `request_role`, `create_time`);


-- ============================================================================
-- 升级完成
-- ============================================================================
