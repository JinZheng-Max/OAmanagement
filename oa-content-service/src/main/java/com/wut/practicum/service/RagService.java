package com.wut.practicum.service;

import com.wut.practicum.client.ChromaClient;
import com.wut.practicum.client.DeepSeekClient;
import com.wut.practicum.client.EmbeddingClient;
import com.wut.practicum.config.RagConfig;
import com.wut.practicum.entity.OaAiCitation;
import com.wut.practicum.entity.OaAiSession;
import com.wut.practicum.entity.OaAiSource;
import com.wut.practicum.entity.OaAiSourceChunk;
import com.wut.practicum.mapper.OaAiCitationMapper;
import com.wut.practicum.mapper.OaAiSessionMapper;
import com.wut.practicum.mapper.OaAiSourceChunkMapper;
import com.wut.practicum.mapper.OaAiSourceMapper;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.io.*;

@Service
public class RagService {
    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingClient embeddingClient;
    private final ChromaClient chromaClient;
    private final DeepSeekClient deepSeekClient;
    private final OaAiSourceMapper aiSourceMapper;
    private final OaAiSourceChunkMapper aiSourceChunkMapper;
    private final OaAiSessionMapper aiSessionMapper;
    private final OaAiCitationMapper aiCitationMapper;
    private final RagConfig ragConfig;
    private final Tika tika = new Tika();

    public RagService(EmbeddingClient embeddingClient, ChromaClient chromaClient,
                      DeepSeekClient deepSeekClient, OaAiSourceMapper aiSourceMapper,
                      OaAiSourceChunkMapper aiSourceChunkMapper,
                      OaAiSessionMapper aiSessionMapper, OaAiCitationMapper aiCitationMapper,
                      RagConfig ragConfig) {
        this.embeddingClient = embeddingClient;
        this.chromaClient = chromaClient;
        this.deepSeekClient = deepSeekClient;
        this.aiSourceMapper = aiSourceMapper;
        this.aiSourceChunkMapper = aiSourceChunkMapper;
        this.aiSessionMapper = aiSessionMapper;
        this.aiCitationMapper = aiCitationMapper;
        this.ragConfig = ragConfig;
    }

    // ========================================================================
    // 文档上传与索引
    // ========================================================================

    /**
     * 保存文件到本地存储并创建数据库记录
     * 如果文件Hash已存在，则递增版本号而非创建新记录
     */
    @Transactional
    public OaAiSource saveUploadedFile(byte[] fileBytes, String originalFileName,
                                        String title, String category, String description,
                                        String accessScope, String accessDepartmentId,
                                        Integer minRoleLevel, Long uploaderId) throws Exception {
        String ext = getExtension(originalFileName).toLowerCase();
        String mimeType = getMimeType(ext);
        String fileHash = sha256(fileBytes);
        String storedName = UUID.randomUUID() + "." + ext;

        // 检查是否已存在相同Hash的文档
        OaAiSource existing = aiSourceMapper.selectByFileHash(fileHash);
        if (existing != null) {
            log.info("文件Hash重复，复用已有记录: id={}, file={}", existing.getId(), originalFileName);
            return existing; // 实际业务中可开启新版本，这里简单返回已有记录
        }

        // 文件存储目录
        String uploadDir = ragConfig.getStorage().getUploadDir();
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        String fileUrl = uploadDir + "/" + storedName;
        java.nio.file.Files.write(java.nio.file.Paths.get(fileUrl), fileBytes);

        OaAiSource source = new OaAiSource();
        source.setTitle(title != null && !title.isBlank() ? title : originalFileName);
        source.setCategory(category);
        source.setDescription(description);
        source.setOriginalFileName(originalFileName);
        source.setStoredFileName(storedName);
        source.setFileUrl(fileUrl);
        source.setFileExtension(ext);
        source.setMimeType(mimeType);
        source.setFileSize((long) fileBytes.length);
        source.setFileHash(fileHash);
        source.setStorageProvider("LOCAL");
        source.setAccessScope(accessScope != null ? accessScope : "ALL");
        source.setAccessDepartmentId(accessDepartmentId);
        source.setMinRoleLevel(minRoleLevel != null ? minRoleLevel : 1);
        source.setUploaderId(uploaderId);
        source.setParseStatus("PENDING");
        source.setIndexStatus("PENDING");

        aiSourceMapper.insert(source);
        log.info("文件保存成功: id={}, file={}", source.getId(), originalFileName);
        return source;
    }

    /**
     * 上传新版本：禁用旧版本、创建新记录、索引
     */
    @Transactional
    public OaAiSource uploadNewVersion(byte[] fileBytes, String originalFileName,
                                        Long oldSourceId, Long uploaderId) throws Exception {
        OaAiSource old = aiSourceMapper.selectById(oldSourceId);
        if (old == null) throw new IllegalArgumentException("原文档不存在: " + oldSourceId);

        // 禁用旧版
        OaAiSource disableUpdate = new OaAiSource();
        disableUpdate.setId(oldSourceId);
        disableUpdate.setStatus("DISABLED");
        aiSourceMapper.updateById(disableUpdate);

        // 删除旧分片（新版重新插入）
        aiSourceChunkMapper.deleteBySourceId(oldSourceId);

        // 异步清理旧Chroma向量
        disableChromaVectors(oldSourceId);

        // 创建新版本记录
        OaAiSource newSource = saveUploadedFile(fileBytes, originalFileName,
                old.getTitle(), old.getCategory(), old.getDescription(),
                old.getAccessScope(), old.getAccessDepartmentId(),
                old.getMinRoleLevel(), uploaderId);

        // 递增版本号
        newSource.setVersion(old.getVersion() + 1);
        aiSourceMapper.updateById(newSource);

        // 启动索引
        asyncIndexDocument(newSource.getId());
        return newSource;
    }

    /**
     * 异步解析、分片、向量化、写入Chroma、保存分片到DB
     */
    @Async
    public void asyncIndexDocument(Long sourceId) {
        OaAiSource source = aiSourceMapper.selectById(sourceId);
        if (source == null) {
            log.error("文档不存在: id={}", sourceId);
            return;
        }

        try {
            // 1. 解析状态 -> PROCESSING
            source.setParseStatus("PROCESSING");
            aiSourceMapper.updateById(source);

            // 2. 使用Tika解析文档
            File file = new File(source.getFileUrl());
            if (!file.exists()) {
                throw new IOException("文件不存在: " + source.getFileUrl());
            }
            String extractedText = tika.parseToString(file);
            if (extractedText == null || extractedText.isBlank()) {
                throw new IllegalStateException("文档内容为空");
            }

            // 保存解析后的文本
            source.setExtractedText(extractedText);
            source.setParseStatus("SUCCESS");
            source.setIndexStatus("PROCESSING");
            aiSourceMapper.updateById(source);

            // 3. 文本分片
            int chunkSize = ragConfig.getChunk().getSize();
            int overlap = ragConfig.getChunk().getOverlap();
            List<String> chunks = chunkText(extractedText, chunkSize, overlap);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("分片结果为空");
            }

            // 4. 构建分片记录 + 向量ID
            List<OaAiSourceChunk> chunkRecords = new ArrayList<>();
            List<String> vectorIds = new ArrayList<>();
            List<String> chunkContents = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                String text = chunks.get(i);
                String vectorId = "source:" + sourceId + ":version:" + source.getVersion() + ":chunk:" + (i + 1);

                OaAiSourceChunk chunk = new OaAiSourceChunk();
                chunk.setSourceId(sourceId);
                chunk.setChunkNo(i + 1);
                chunk.setContentText(text);
                chunk.setContentHash(sha256(text));
                chunk.setTokenCount(estimateTokenCount(text));
                chunk.setVectorKey(vectorId);
                chunk.setStatus("ENABLED");
                chunkRecords.add(chunk);

                vectorIds.add(vectorId);
                chunkContents.add(text);
            }

            // 5. 批量生成Embedding
            List<List<Double>> embeddings = embeddingClient.embedDocuments(chunkContents);

            // 6. 准备Chroma元数据
            List<Map<String, Object>> metadatas = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("source_id", sourceId);
                meta.put("chunk_no", i + 1);
                meta.put("version", source.getVersion());
                meta.put("source_status", "ENABLED");
                meta.put("chunk_status", "ENABLED");
                meta.put("access_scope", source.getAccessScope());
                meta.put("min_role_level", source.getMinRoleLevel());
                if (source.getAccessDepartmentId() != null) {
                    meta.put("access_department_id", source.getAccessDepartmentId());
                }
                metadatas.add(meta);
            }

            // 7. 批量写入Chroma
            chromaClient.addChunks(vectorIds, chunkContents, embeddings, metadatas);
            log.info("Chroma写入完成: sourceId={}, chunks={}", sourceId, chunks.size());

            // 8. 保存分片记录到数据库（设置从Chroma写入成功后的ID）
            for (int i = 0; i < chunkRecords.size(); i++) {
                chunkRecords.get(i).setVectorKey(vectorIds.get(i));
            }
            aiSourceChunkMapper.insertBatch(chunkRecords);
            log.info("分片记录已保存: sourceId={}, count={}", sourceId, chunkRecords.size());

            // 9. 更新source状态为SUCCESS
            source.setIndexStatus("SUCCESS");
            source.setErrorMessage(null);
            aiSourceMapper.updateById(source);

            log.info("文档索引完成: id={}, title={}, chunks={}", sourceId, source.getTitle(), chunks.size());
        } catch (Exception e) {
            log.error("文档索引失败: id={}, error={}", sourceId, e.getMessage());
            source.setIndexStatus("FAILED");
            source.setParseStatus("FAILED");
            source.setErrorMessage(sanitizeError(e.getMessage()));
            aiSourceMapper.updateById(source);
        }
    }

    /** 停用某文档的所有Chroma向量 */
    private void disableChromaVectors(Long sourceId) {
        try {
            List<OaAiSourceChunk> chunks = aiSourceChunkMapper.selectBySourceId(sourceId);
            if (chunks != null && !chunks.isEmpty()) {
                List<String> vectorKeys = chunks.stream()
                        .map(OaAiSourceChunk::getVectorKey)
                        .filter(Objects::nonNull)
                        .toList();
                if (!vectorKeys.isEmpty()) {
                    chromaClient.deleteChunks(vectorKeys);
                    log.info("已清理Chroma旧向量: sourceId={}, count={}", sourceId, vectorKeys.size());
                }
            }
        } catch (Exception e) {
            log.warn("Chroma向量清理失败(非致命): sourceId={}", sourceId, e);
        }
    }

    // ========================================================================
    // AI问答
    // ========================================================================

    /**
     * AI问答 - Chroma检索 + MySQL二次鉴权 + DeepSeek + 引用保存
     */
    @Transactional
    public AnswerResult ask(String question, Long userId, String role, Long departmentId) {
        long start = System.currentTimeMillis();

        log.info("================== [RAG 问答流程开始] ==================");
        log.info("【用户身份与提问】: userId={}, role={}, departmentId={}, 问题='{}'", userId, role, departmentId, question);

        // 1. 生成问题向量
        List<Double> queryEmbedding = embeddingClient.embedQuery(question);
        log.info("1. [Embedding] 问题向量化完成, 生成维度: {}", queryEmbedding != null ? queryEmbedding.size() : 0);

        // 2. Chroma带权限条件检索
        Map<String, Object> where = buildChromaWhere(role, departmentId);
        ChromaClient.ChromaQueryResult chromaResult = chromaClient.query(
                queryEmbedding, where, ragConfig.getChroma().getRetrieveCount());

        int recalledCount = (chromaResult != null && chromaResult.documents() != null) ? chromaResult.documents().size() : 0;
        log.info("2. [ChromaDB] 带权限初筛完成, 召回文档片段数: {}", recalledCount);

        if (chromaResult == null || chromaResult.documents().isEmpty()) {
            log.warn("ChromaDB 向量检索召回数为 0，返回知识库空提示");
            saveSession(userId, role, departmentId, question, "知识库中暂无相关内容。", "ANSWERED");
            return new AnswerResult(question, "知识库中暂无相关内容。", List.of());
        }

        // 3. 从Chroma结果提取source_id集合（MySQL二次鉴权用）
        Set<Long> chromaSourceIds = new HashSet<>();
        for (Map<String, Object> meta : chromaResult.metadatas()) {
            if (meta != null && meta.get("source_id") != null) {
                chromaSourceIds.add(((Number) meta.get("source_id")).longValue());
            }
        }

        // 4. MySQL二次鉴权
        int roleLevel = getRoleLevel(role);
        List<OaAiSource> authorizedSources = aiSourceMapper.selectAuthorizedSources(
                new ArrayList<>(chromaSourceIds), role, roleLevel, departmentId);

        Set<Long> authorizedSourceIds = new HashSet<>();
        Map<Long, OaAiSource> sourceMap = new HashMap<>();
        for (OaAiSource s : authorizedSources) {
            authorizedSourceIds.add(s.getId());
            sourceMap.put(s.getId(), s);
        }

        log.info("3. [MySQL 鉴权] 二次鉴权筛选完成: Chroma 候选文档数={}, MySQL 校验通过数={}",
                chromaSourceIds.size(), authorizedSourceIds.size());

        // 5. 过滤 + 构建上下文（仅MySQL鉴权通过的分片）
        StringBuilder contextBuilder = new StringBuilder();
        List<SourceRef> sources = new ArrayList<>();
        List<OaAiCitation> citations = new ArrayList<>();
        int sourceIdx = 0;

        for (int i = 0; i < chromaResult.documents().size() && sourceIdx < 8; i++) {
            Map<String, Object> meta = chromaResult.metadatas().get(i);
            if (meta == null) continue;

            Long sid = meta.get("source_id") != null ?
                    ((Number) meta.get("source_id")).longValue() : null;
            if (sid == null || !authorizedSourceIds.contains(sid)) continue;

            OaAiSource authorizedSource = sourceMap.get(sid);
            sourceIdx++;
            String sidLabel = "S" + sourceIdx;
            String docContent = chromaResult.documents().get(i);
            Double distance = chromaResult.distances() != null && i < chromaResult.distances().size()
                    ? chromaResult.distances().get(i) : null;

            // 构建上下文（带来源信息）
            contextBuilder.append("[").append(sidLabel).append("]\n");
            if (authorizedSource != null) {
                contextBuilder.append("文件：").append(authorizedSource.getTitle()).append("\n");
                contextBuilder.append("版本：V").append(authorizedSource.getVersion()).append("\n");
            }
            contextBuilder.append("内容：").append(docContent).append("\n\n");

            // 来源引用（用户可见）
            String summary = docContent.substring(0, Math.min(100, docContent.length()));
            sources.add(new SourceRef(sidLabel, sid, summary));

            // 构建引用记录（入库）
            OaAiCitation citation = new OaAiCitation();
            citation.setSourceType("KNOWLEDGE_FILE");
            citation.setSourceId(sid);
            citation.setSourceTitle(authorizedSource != null ? authorizedSource.getTitle() : "");
            citation.setFragmentSummary(summary);
            citation.setScore(distance != null ? (1.0 - distance) : null);
            citation.setSourceVersion(authorizedSource != null ? authorizedSource.getVersion() : null);
            citations.add(citation);
        }

        if (contextBuilder.length() == 0) {
            log.warn("鉴权过滤后可用上下文为空，返回默认空提示");
            saveSession(userId, role, departmentId, question, "知识库中暂无相关内容。", "ANSWERED");
            return new AnswerResult(question, "知识库中暂无相关内容。", List.of());
        }

        log.info("4. [上下文构建] 组装授权文本分片数: {}, 引用文档标签: {}", 
                sourceIdx, sources.stream().map(SourceRef::id).toList());

        // 6. 调用DeepSeek
        String answer = deepSeekClient.chat(question, contextBuilder.toString());
        long elapsed = System.currentTimeMillis() - start;

        // 7. 保存会话记录
        OaAiSession session = saveSession(userId, role, departmentId, question, answer, "ANSWERED");

        // 8. 保存引用记录
        for (OaAiCitation citation : citations) {
            citation.setSessionId(session.getId());
        }
        if (!citations.isEmpty()) {
            aiCitationMapper.insertBatch(citations);
        }

        log.info("5. [RAG 问答完成] userId={}, 对应 SessionId={}, 包含引用片段数={}, 总耗时: {} ms", 
                userId, session.getId(), citations.size(), elapsed);
        log.info("================== [RAG 问答流程结束] ==================");
        return new AnswerResult(question, answer, sources);
    }

    /** 构建Chroma查询的权限过滤条件 */
    private Map<String, Object> buildChromaWhere(String role, Long departmentId) {
        List<Object> conditions = new ArrayList<>();
        conditions.add(Map.of("source_status", "ENABLED"));
        conditions.add(Map.of("chunk_status", "ENABLED"));

        if ("SUPER_ADMIN".equals(role)) {
            return Map.of("$and", conditions);
        }

        int roleLevel = getRoleLevel(role);
        conditions.add(Map.of("min_role_level", Map.of("$lte", roleLevel)));
        // 多部门ID逗号分隔，Chroma仅做粗筛，MySQL二次鉴权做精确匹配
        conditions.add(Map.of("$or", List.of(
                Map.of("access_scope", "ALL"),
                Map.of("access_scope", "DEPARTMENT")
        )));
        return Map.of("$and", conditions);
    }

    /** 保存AI问答会话 */
    private OaAiSession saveSession(Long userId, String role, Long departmentId,
                                     String question, String answer, String status) {
        OaAiSession session = new OaAiSession();
        session.setUserId(userId);
        session.setRequestRole(role);
        session.setRequestDepartmentId(departmentId);
        session.setPermissionDigest(role + ":" + departmentId + ":v1");
        session.setQuestion(question);
        session.setAnswerSummary(answer != null ? answer.substring(0, Math.min(500, answer.length())) : null);
        session.setStatus(status);
        aiSessionMapper.insert(session);
        return session;
    }

    // ========================================================================
    // 文档管理
    // ========================================================================

    public List<OaAiSource> listSources(String title, String category, String status, Long uploaderId) {
        OaAiSource params = new OaAiSource();
        params.setTitle(title);
        params.setCategory(category);
        params.setStatus(status);
        params.setUploaderId(uploaderId);
        return aiSourceMapper.selectList(params);
    }

    /** 修改可见范围（同步更新Chroma元数据） */
    @Transactional
    public void updateAccess(Long sourceId, String accessScope, String accessDepartmentId, Integer minRoleLevel) {
        OaAiSource source = aiSourceMapper.selectById(sourceId);
        if (source == null) throw new IllegalArgumentException("文档不存在: " + sourceId);

        // 全公司可见时清除部门ID
        if ("ALL".equals(accessScope)) {
            accessDepartmentId = null;
        }

        aiSourceMapper.updateAccessFields(sourceId, accessScope, accessDepartmentId, minRoleLevel);

        // 在Chroma中删除旧向量（权限变更必须保证即时生效）
        disableChromaVectors(sourceId);
        log.info("权限已更新，旧向量已清理: sourceId={}, scope={}", sourceId, accessScope);
    }

    /** 启用或停用文档 */
    @Transactional
    public void updateStatus(Long sourceId, String newStatus) {
        OaAiSource source = aiSourceMapper.selectById(sourceId);
        if (source == null) throw new IllegalArgumentException("文档不存在: " + sourceId);

        OaAiSource update = new OaAiSource();
        update.setId(sourceId);
        update.setStatus(newStatus);
        aiSourceMapper.updateById(update);

        if ("DISABLED".equals(newStatus)) {
            // 停用：同步禁用分片 + 清理Chroma向量
            aiSourceChunkMapper.updateStatusBySourceId(sourceId, "DISABLED");
            disableChromaVectors(sourceId);
            log.info("文档已停用，向量已清理: id={}", sourceId);
        }
    }

    /** 查询会话引用记录 */
    public List<OaAiCitation> getCitations(Long sessionId) {
        return aiCitationMapper.selectBySessionId(sessionId);
    }

    /** 重新索引文档（先清理旧向量，重置状态但不异步执行） */
    @Transactional
    public void reindex(Long sourceId) {
        OaAiSource source = aiSourceMapper.selectById(sourceId);
        if (source == null) throw new IllegalArgumentException("文档不存在: " + sourceId);

        // 清理旧向量和旧分片（物理删除以便重新插入）
        disableChromaVectors(sourceId);
        aiSourceChunkMapper.deleteBySourceId(sourceId);

        // 重置状态
        source.setParseStatus("PENDING");
        source.setIndexStatus("PENDING");
        source.setErrorMessage(null);
        aiSourceMapper.updateById(source);

        log.info("文档已重置，等待异步索引: id={}", sourceId);
    }

    // ========================================================================
    // 学习计划生成
    // ========================================================================

    public AnswerResult generateLearningPlan(String topic, Long userId, String role, Long departmentId) {
        List<Double> queryEmbedding = embeddingClient.embedQuery("培训计划 " + topic);
        Map<String, Object> where = buildChromaWhere(role, departmentId);
        ChromaClient.ChromaQueryResult chromaResult = chromaClient.query(
                queryEmbedding, where, 20);

        if (chromaResult == null || chromaResult.documents().isEmpty()) {
            return new AnswerResult(topic, "未找到相关的培训资料。", List.of());
        }

        // MySQL二次鉴权
        Set<Long> sourceIdSet = new HashSet<>();
        for (Map<String, Object> meta : chromaResult.metadatas()) {
            if (meta != null && meta.get("source_id") != null) {
                sourceIdSet.add(((Number) meta.get("source_id")).longValue());
            }
        }
        int roleLevel = getRoleLevel(role);
        List<OaAiSource> authorizedSources = aiSourceMapper.selectAuthorizedSources(
                new ArrayList<>(sourceIdSet), role, roleLevel, departmentId);
        Set<Long> authorizedIds = new HashSet<>();
        Map<Long, OaAiSource> sourceMap = new HashMap<>();
        for (OaAiSource s : authorizedSources) {
            authorizedIds.add(s.getId());
            sourceMap.put(s.getId(), s);
        }

        StringBuilder contextBuilder = new StringBuilder();
        List<SourceRef> sources = new ArrayList<>();
        int sourceIdx = 0;

        for (int i = 0; i < chromaResult.documents().size() && sourceIdx < 10; i++) {
            Map<String, Object> meta = chromaResult.metadatas().get(i);
            if (meta == null) continue;
            Long sid = meta.get("source_id") != null ?
                    ((Number) meta.get("source_id")).longValue() : null;
            if (sid == null || !authorizedIds.contains(sid)) continue;

            OaAiSource s = sourceMap.get(sid);
            sourceIdx++;
            String sidLabel = "S" + sourceIdx;
            contextBuilder.append("[").append(sidLabel).append("]\n");
            if (s != null) contextBuilder.append("文件：").append(s.getTitle()).append("\n");
            contextBuilder.append("内容：").append(chromaResult.documents().get(i)).append("\n\n");

            String summary = chromaResult.documents().get(i).substring(0,
                    Math.min(100, chromaResult.documents().get(i).length()));
            sources.add(new SourceRef(sidLabel, sid, summary));
        }

        String planPrompt = "你是企业培训导师。请根据以下授权资料，生成一份详细的学习计划。"
                + "主题：" + topic + "\n\n授权资料：\n" + contextBuilder;
        String answer = deepSeekClient.chat(planPrompt, "");
        return new AnswerResult(topic, answer, sources);
    }

    // ========================================================================
    // 工具方法
    // ========================================================================

    public static List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            if (end < text.length()) {
                // 优先在句号处断开
                int lastPeriod = text.lastIndexOf('。', end);
                if (lastPeriod > start && lastPeriod > end - chunkSize / 2) end = lastPeriod + 1;
            }
            String chunk = text.substring(start, Math.min(end, text.length())).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
            // 已处理到文档末尾，终止循环（防止overlap回退导致死循环）
            if (end >= text.length()) break;
            start = end - overlap;
            if (start < 0) start = 0;
            if (start >= end) break;
        }
        return chunks;
    }

    private static int getRoleLevel(String role) {
        if (role == null) return 1;
        return switch (role) {
            case "SUPER_ADMIN" -> 3;
            case "DEPT_MANAGER" -> 2;
            default -> 1;
        };
    }

    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1) : "docx";
    }

    private static String getMimeType(String ext) {
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            default -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        };
    }

    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return bytesToHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return bytesToHex(md.digest(data));
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static int estimateTokenCount(String text) {
        if (text == null) return 0;
        return (int) (text.length() / 1.5);
    }

    private static String sanitizeError(String error) {
        if (error == null) return "未知错误";
        error = error.replaceAll("\\\\", "/");
        return error.length() > 500 ? error.substring(0, 500) : error;
    }

    // ========================================================================
    // 记录类型
    // ========================================================================

    public record AnswerResult(String question, String answer, List<SourceRef> sources) {}
    public record SourceRef(String id, Long sourceId, String summary) {}
}
