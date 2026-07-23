-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: oa_db
-- ------------------------------------------------------
-- Server version	8.2.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `oa_ai_citation`
--

DROP TABLE IF EXISTS `oa_ai_citation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_ai_citation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '问答会话ID',
  `source_type` varchar(20) NOT NULL DEFAULT 'CONTENT' COMMENT '引用类型: CONTENT/KNOWLEDGE_FILE',
  `content_id` bigint DEFAULT NULL COMMENT '引用公告/制度ID；source_type=CONTENT时使用',
  `source_id` bigint DEFAULT NULL COMMENT '引用知识来源ID；source_type=KNOWLEDGE_FILE时使用',
  `chunk_id` bigint DEFAULT NULL COMMENT '引用的知识分片ID',
  `source_title` varchar(200) DEFAULT NULL COMMENT '引用标题快照',
  `fragment_summary` varchar(500) DEFAULT NULL COMMENT '片段摘要',
  `score` double DEFAULT NULL COMMENT '匹配分数',
  `source_version` int DEFAULT NULL COMMENT '引用时来源版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_citation_source` (`source_id`),
  KEY `idx_citation_chunk` (`chunk_id`),
  KEY `idx_citation_session_type` (`session_id`,`source_type`),
  CONSTRAINT `chk_ai_citation_source_ref` CHECK ((((`source_type` = _utf8mb4'CONTENT') and (`content_id` is not null)) or ((`source_type` = _utf8mb4'KNOWLEDGE_FILE') and (`source_id` is not null)))),
  CONSTRAINT `chk_ai_citation_source_type` CHECK ((`source_type` in (_utf8mb4'CONTENT',_utf8mb4'KNOWLEDGE_FILE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI回答引用记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_ai_citation`
--

LOCK TABLES `oa_ai_citation` WRITE;
/*!40000 ALTER TABLE `oa_ai_citation` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_ai_citation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_ai_session`
--

DROP TABLE IF EXISTS `oa_ai_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_ai_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `request_role` varchar(30) DEFAULT NULL COMMENT '提问时角色快照: SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE',
  `request_department_id` bigint DEFAULT NULL COMMENT '提问时部门ID快照',
  `permission_digest` varchar(128) DEFAULT NULL COMMENT '角色、部门、权限版本生成的权限摘要，用于缓存隔离',
  `question` text NOT NULL COMMENT '用户问题',
  `answer_summary` text COMMENT '答案摘要',
  `status` varchar(20) NOT NULL DEFAULT 'ANSWERED' COMMENT '状态: ANSWERED/REJECTED/DEGRADED/FAILED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_ai_session_permission` (`request_department_id`,`request_role`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI问答会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_ai_session`
--

LOCK TABLES `oa_ai_session` WRITE;
/*!40000 ALTER TABLE `oa_ai_session` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_ai_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_ai_source`
--

DROP TABLE IF EXISTS `oa_ai_source`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_ai_source` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '知识文档标题',
  `category` varchar(50) DEFAULT NULL COMMENT '分类: 企业文化/新人培训/技术部资料/行政管理等',
  `description` varchar(500) DEFAULT NULL COMMENT '文档简介',
  `original_file_name` varchar(255) NOT NULL COMMENT '用户上传时的原文件名',
  `stored_file_name` varchar(255) NOT NULL COMMENT '系统随机化后的存储文件名',
  `file_url` varchar(500) NOT NULL COMMENT '受控文件路径或OSS对象地址；不得直接公开访问',
  `file_extension` varchar(10) NOT NULL COMMENT '文件扩展名: doc/docx/pdf',
  `mime_type` varchar(100) NOT NULL COMMENT '文件MIME类型',
  `file_size` bigint unsigned NOT NULL COMMENT '文件大小，单位字节',
  `file_hash` char(64) NOT NULL COMMENT '文件SHA-256摘要，用于去重与完整性校验',
  `storage_provider` varchar(20) NOT NULL DEFAULT 'LOCAL' COMMENT '存储方式: LOCAL/OSS',
  `extracted_text` longtext COMMENT 'Word/PDF解析后的全文；用于分片重建',
  `parse_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态: PENDING/PROCESSING/SUCCESS/FAILED',
  `index_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '向量索引状态: PENDING/PROCESSING/SUCCESS/FAILED',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '解析或索引失败的脱敏摘要',
  `access_scope` varchar(20) NOT NULL DEFAULT 'ALL' COMMENT '访问范围: ALL-全公司/DEPARTMENT-指定部门',
  `access_department_id` bigint DEFAULT NULL COMMENT '可访问部门ID；access_scope=DEPARTMENT时必填',
  `min_role_level` tinyint NOT NULL DEFAULT '1' COMMENT '最低角色等级: 1-普通员工/2-部门经理/3-总管理员',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT '文档状态: ENABLED/DISABLED',
  `version` int NOT NULL DEFAULT '1' COMMENT '知识文档版本号',
  `uploader_id` bigint NOT NULL COMMENT '上传人用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_source_file_hash` (`file_hash`),
  KEY `idx_source_category_status` (`category`,`status`),
  KEY `idx_source_access` (`status`,`access_scope`,`access_department_id`,`min_role_level`),
  KEY `idx_source_uploader_time` (`uploader_id`,`create_time`),
  KEY `idx_source_parse_index_status` (`parse_status`,`index_status`),
  CONSTRAINT `chk_ai_source_access_department` CHECK ((((`access_scope` = _utf8mb4'ALL') and (`access_department_id` is null)) or ((`access_scope` = _utf8mb4'DEPARTMENT') and (`access_department_id` is not null)))),
  CONSTRAINT `chk_ai_source_access_scope` CHECK ((`access_scope` in (_utf8mb4'ALL',_utf8mb4'DEPARTMENT'))),
  CONSTRAINT `chk_ai_source_file_extension` CHECK ((`file_extension` in (_utf8mb4'doc',_utf8mb4'docx',_utf8mb4'pdf'))),
  CONSTRAINT `chk_ai_source_index_status` CHECK ((`index_status` in (_utf8mb4'PENDING',_utf8mb4'PROCESSING',_utf8mb4'SUCCESS',_utf8mb4'FAILED'))),
  CONSTRAINT `chk_ai_source_min_role_level` CHECK ((`min_role_level` between 1 and 3)),
  CONSTRAINT `chk_ai_source_parse_status` CHECK ((`parse_status` in (_utf8mb4'PENDING',_utf8mb4'PROCESSING',_utf8mb4'SUCCESS',_utf8mb4'FAILED'))),
  CONSTRAINT `chk_ai_source_status` CHECK ((`status` in (_utf8mb4'ENABLED',_utf8mb4'DISABLED'))),
  CONSTRAINT `chk_ai_source_storage_provider` CHECK ((`storage_provider` in (_utf8mb4'LOCAL',_utf8mb4'OSS')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG企业知识来源';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_ai_source`
--

LOCK TABLES `oa_ai_source` WRITE;
/*!40000 ALTER TABLE `oa_ai_source` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_ai_source` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_ai_source_chunk`
--

DROP TABLE IF EXISTS `oa_ai_source_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_ai_source_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_id` bigint NOT NULL COMMENT '所属知识来源ID，逻辑关联oa_ai_source.id',
  `chunk_no` int NOT NULL COMMENT '文档内分片序号，从0或1开始由程序统一',
  `section_title` varchar(200) DEFAULT NULL COMMENT '章节标题或段落标题',
  `page_no` int DEFAULT NULL COMMENT 'PDF页码；Word无明确页码时可为空',
  `content_text` longtext NOT NULL COMMENT '分片正文',
  `content_hash` char(64) DEFAULT NULL COMMENT '分片SHA-256摘要',
  `token_count` int DEFAULT NULL COMMENT '分片Token数量',
  `vector_key` varchar(200) DEFAULT NULL COMMENT 'RedisStack/向量库中的向量记录Key',
  `metadata_json` json DEFAULT NULL COMMENT '扩展元数据，如标题层级、段落位置等',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_chunk` (`source_id`,`chunk_no`),
  KEY `idx_chunk_source_status` (`source_id`,`status`),
  KEY `idx_chunk_vector_key` (`vector_key`),
  KEY `idx_chunk_content_hash` (`content_hash`),
  CONSTRAINT `chk_ai_source_chunk_status` CHECK ((`status` in (_utf8mb4'ENABLED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG知识文档分片';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_ai_source_chunk`
--

LOCK TABLES `oa_ai_source_chunk` WRITE;
/*!40000 ALTER TABLE `oa_ai_source_chunk` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_ai_source_chunk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_attendance`
--

DROP TABLE IF EXISTS `oa_attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `work_date` varchar(10) NOT NULL COMMENT '工作日期(yyyy-MM-dd)',
  `check_in` datetime DEFAULT NULL COMMENT '签到时间',
  `check_out` datetime DEFAULT NULL COMMENT '签退时间',
  `check_in_ip` varchar(45) DEFAULT NULL COMMENT '签到时客户端IP（用于内网校验）',
  `check_out_ip` varchar(45) DEFAULT NULL COMMENT '签退时客户端IP（用于内网校验）',
  `status` varchar(20) NOT NULL DEFAULT 'UNCHECKED' COMMENT '状态: UNCHECKED/CHECKED_IN/CHECKED_OUT',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_date` (`employee_id`,`work_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_attendance`
--

LOCK TABLES `oa_attendance` WRITE;
/*!40000 ALTER TABLE `oa_attendance` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_attendance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_content`
--

DROP TABLE IF EXISTS `oa_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_content` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(20) NOT NULL COMMENT '类型: ANNOUNCEMENT/POLICY',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `body` text NOT NULL COMMENT '正文',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/UNPUBLISHED',
  `scope` varchar(50) DEFAULT 'ALL' COMMENT '可见范围: ALL/DEPARTMENT',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人ID',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `view_count` bigint NOT NULL DEFAULT '0' COMMENT '访问次数/浏览量(热点公告Redis缓存依据)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_type` (`status`,`type`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_view_count` (`view_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告制度';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_content`
--

LOCK TABLES `oa_content` WRITE;
/*!40000 ALTER TABLE `oa_content` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_content` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_employee`
--

DROP TABLE IF EXISTS `oa_employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_no` varchar(50) NOT NULL COMMENT '员工编号',
  `name` varchar(50) NOT NULL COMMENT '员工姓名',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `position` varchar(100) DEFAULT NULL COMMENT '职位',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系方式',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1-在职 0-离职',
  `hire_date` date DEFAULT NULL COMMENT '入职日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_no` (`employee_no`),
  KEY `idx_department` (`department_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工档案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_employee`
--

LOCK TABLES `oa_employee` WRITE;
/*!40000 ALTER TABLE `oa_employee` DISABLE KEYS */;
INSERT INTO `oa_employee` VALUES (1,'EMP001','张三',1,'技术经理','13800000001',1,NULL,'2026-07-22 09:30:26','2026-07-22 09:30:26'),(2,'EMP002','李四',1,'Java开发工程师','13800000002',1,NULL,'2026-07-22 09:30:26','2026-07-22 09:30:26'),(3,'EMP003','王五',2,'HR主管','13800000003',1,NULL,'2026-07-22 09:30:26','2026-07-22 09:30:26'),(4,'EMP004','赵六',3,'财务专员','13800000004',1,NULL,'2026-07-22 09:30:26','2026-07-22 09:30:26');
/*!40000 ALTER TABLE `oa_employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_leave`
--

DROP TABLE IF EXISTS `oa_leave`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_leave` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `applicant_id` bigint NOT NULL COMMENT '申请人ID(员工ID)',
  `type` varchar(20) NOT NULL COMMENT '请假类型: ANNUAL/SICK/PERSONAL/OTHER',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `reason` varchar(500) NOT NULL COMMENT '请假原因',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/WITHDRAWN',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_applicant_status` (`applicant_id`,`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假申请';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_leave`
--

LOCK TABLES `oa_leave` WRITE;
/*!40000 ALTER TABLE `oa_leave` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_leave` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_leave_attachment`
--

DROP TABLE IF EXISTS `oa_leave_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_leave_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `leave_id` bigint DEFAULT NULL COMMENT '关联的请假申请ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名（如: 医院证明.jpg）',
  `file_url` varchar(500) NOT NULL COMMENT '文件存储路径/URL（后续改为阿里云OSS地址）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `mime_type` varchar(50) DEFAULT NULL COMMENT '文件类型（如: image/jpeg）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_leave_id` (`leave_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假附件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_leave_attachment`
--

LOCK TABLES `oa_leave_attachment` WRITE;
/*!40000 ALTER TABLE `oa_leave_attachment` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_leave_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_leave_audit`
--

DROP TABLE IF EXISTS `oa_leave_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_leave_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `leave_id` bigint NOT NULL COMMENT '请假申请ID',
  `auditor_id` bigint NOT NULL COMMENT '审批人ID',
  `action` varchar(20) NOT NULL COMMENT '审批动作: APPROVED/REJECTED',
  `comment` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `audit_time` datetime DEFAULT NULL COMMENT '审批时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_leave_id` (`leave_id`),
  KEY `idx_auditor_id` (`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_leave_audit`
--

LOCK TABLES `oa_leave_audit` WRITE;
/*!40000 ALTER TABLE `oa_leave_audit` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_leave_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_department`
--

DROP TABLE IF EXISTS `sys_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_department` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL COMMENT '部门编码',
  `name` varchar(100) NOT NULL COMMENT '部门名称',
  `leader_id` bigint DEFAULT NULL COMMENT '部门负责人ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1-启用 0-停用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_department`
--

LOCK TABLES `sys_department` WRITE;
/*!40000 ALTER TABLE `sys_department` DISABLE KEYS */;
INSERT INTO `sys_department` VALUES (1,'DEPT_IT','技术部',NULL,1,1,'2026-07-22 09:30:26','2026-07-22 09:30:26'),(2,'DEPT_HR','人事部',NULL,1,2,'2026-07-22 09:30:26','2026-07-22 09:30:26'),(3,'DEPT_FIN','财务部',NULL,1,3,'2026-07-22 09:30:26','2026-07-22 09:30:26'),(4,'DEPT_ADMIN','行政部',NULL,1,4,'2026-07-22 09:30:26','2026-07-22 09:30:26');
/*!40000 ALTER TABLE `sys_department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_operation_log`
--

DROP TABLE IF EXISTS `sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `module` varchar(50) NOT NULL COMMENT '操作模块',
  `action` varchar(100) NOT NULL COMMENT '操作动作',
  `target_id` varchar(50) DEFAULT NULL COMMENT '目标ID',
  `result` varchar(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '结果: SUCCESS/FAILED',
  `detail` varchar(500) DEFAULT NULL COMMENT '操作详情',
  `trace_id` varchar(50) DEFAULT NULL COMMENT '追踪标识',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operator_module` (`operator_id`,`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_operation_log`
--

LOCK TABLES `sys_operation_log` WRITE;
/*!40000 ALTER TABLE `sys_operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password_hash` varchar(200) NOT NULL COMMENT '密码摘要(BCrypt)',
  `role` varchar(30) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色：SUPER_ADMIN/DEPT_MANAGER/EMPLOYEE',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1-启用 0-停用',
  `employee_id` bigint DEFAULT NULL COMMENT '关联员工ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_employee_id` (`employee_id`),
  KEY `idx_role_status` (`role`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH','SUPER_ADMIN',1,NULL,'2026-07-22 09:30:26','2026-07-22 09:30:48'),(2,'zhangsan','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH','EMPLOYEE',1,1,'2026-07-22 09:30:26','2026-07-22 09:30:26'),(3,'lisi','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH','EMPLOYEE',1,2,'2026-07-22 09:30:26','2026-07-22 09:30:26'),(4,'wangwu','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH','EMPLOYEE',1,3,'2026-07-22 09:30:26','2026-07-22 09:30:26'),(5,'zhaoliu','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH','EMPLOYEE',1,4,'2026-07-22 09:30:26','2026-07-22 09:30:26');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-22  9:33:14
