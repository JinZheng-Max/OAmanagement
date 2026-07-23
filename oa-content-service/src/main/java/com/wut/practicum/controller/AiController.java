package com.wut.practicum.controller;

import com.wut.practicum.client.EmployeeClient;
import com.wut.practicum.common.ApiResult;
import com.wut.practicum.dto.EmployeeResponse;
import com.wut.practicum.entity.OaAiCitation;
import com.wut.practicum.entity.OaAiSource;
import com.wut.practicum.security.JwtService;
import com.wut.practicum.service.BatchIndexService;
import com.wut.practicum.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final RagService ragService;
    private final BatchIndexService batchIndexService;
    private final EmployeeClient employeeClient;

    public AiController(RagService ragService, BatchIndexService batchIndexService,
                        EmployeeClient employeeClient) {
        this.ragService = ragService;
        this.batchIndexService = batchIndexService;
        this.employeeClient = employeeClient;
    }

    // ========================================================================
    // 文档管理
    // ========================================================================

    /**
     * 上传知识文档
     */
    @PostMapping("/sources")
    public ApiResult<Map<String, Object>> uploadSource(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "accessScope", defaultValue = "ALL") String accessScope,
            @RequestParam(value = "accessDepartmentIds", required = false) String accessDepartmentId,
            @RequestParam(value = "minRoleLevel", defaultValue = "1") Integer minRoleLevel) {

        long start = System.currentTimeMillis();
        try {
            // 校验文件
            String ext = getExtension(file.getOriginalFilename());
            if (!List.of("doc", "docx", "pdf").contains(ext.toLowerCase())) {
                return ApiResult.fail(400, "仅支持doc/docx/pdf格式");
            }
            if (file.getSize() > 50 * 1024 * 1024) {
                return ApiResult.fail(400, "文件大小不能超过50MB");
            }

            Long uploaderId = getCurrentUserId();
            OaAiSource source = ragService.saveUploadedFile(
                    file.getBytes(), file.getOriginalFilename(),
                    title, category, description,
                    accessScope, accessDepartmentId, minRoleLevel, uploaderId);

            // 异步开始索引
            ragService.asyncIndexDocument(source.getId());

            long elapsed = System.currentTimeMillis() - start;
            return ApiResult.success(Map.of(
                    "id", source.getId(),
                    "title", source.getTitle(),
                    "status", "INDEXING",
                    "elapsedMs", elapsed));
        } catch (Exception e) {
            return ApiResult.fail(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 查询文档列表
     */
    @GetMapping("/sources")
    public ApiResult<List<OaAiSource>> listSources(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "uploaderId", required = false) Long uploaderId) {
        List<OaAiSource> list = ragService.listSources(title, category, status, uploaderId);
        return ApiResult.success(list);
    }

    /**
     * 修改文档可见范围
     */
    @PutMapping("/sources/{id}/access")
    public ApiResult<Void> updateAccess(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String accessScope = (String) body.get("accessScope");
        String accessDepartmentIds = null;
        if (body.get("accessDepartmentIds") != null) {
            Object deptIds = body.get("accessDepartmentIds");
            if (deptIds instanceof String) {
                accessDepartmentIds = (String) deptIds;
            } else if (deptIds instanceof Number) {
                accessDepartmentIds = String.valueOf(((Number) deptIds).longValue());
            }
        }
        Integer minRoleLevel = body.get("minRoleLevel") != null
                ? ((Number) body.get("minRoleLevel")).intValue() : null;
        ragService.updateAccess(id, accessScope, accessDepartmentIds, minRoleLevel);
        return ApiResult.success(null);
    }

    /**
     * 启用或停用文档
     */
    @PutMapping("/sources/{id}/status")
    public ApiResult<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ragService.updateStatus(id, body.get("status"));
        return ApiResult.success(null);
    }

    /**
     * 上传新版本文档（停用旧版+创建新版+异步索引）
     */
    @PostMapping("/sources/{id}/version")
    public ApiResult<Map<String, Object>> uploadNewVersion(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            Long uploaderId = getCurrentUserId();
            OaAiSource newSource = ragService.uploadNewVersion(
                    file.getBytes(), file.getOriginalFilename(), id, uploaderId);
            // 外部调用触发异步（通过AOP代理）
            ragService.asyncIndexDocument(newSource.getId());
            return ApiResult.success(Map.of(
                    "id", newSource.getId(),
                    "version", newSource.getVersion(),
                    "status", "INDEXING"));
        } catch (Exception e) {
            return ApiResult.fail(500, "上传新版本失败: " + e.getMessage());
        }
    }

    /**
     * 重新索引文档（先清理旧向量，再重新解析索引）
     */
    @PostMapping("/sources/{id}/reindex")
    public ApiResult<Void> reindex(@PathVariable Long id) {
        ragService.reindex(id);                // 重置状态 + 清理旧向量
        ragService.asyncIndexDocument(id);      // 通过AOP代理触发异步（外部调用）
        return ApiResult.success(null);
    }

    // ========================================================================
    // AI问答
    // ========================================================================

    /**
     * AI问答
     */
    @PostMapping("/chat")
    public ApiResult<RagService.AnswerResult> chat(@RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question", "");
        if (question.isBlank()) {
            return ApiResult.fail(400, "问题不能为空");
        }

        JwtService.UserPrincipal user = getCurrentUser();
        Long deptId = getDepartmentId(user);

        RagService.AnswerResult result = ragService.ask(
                question, user.userId(), user.role(), deptId);
        return ApiResult.success(result);
    }

    // ========================================================================
    // 学习计划
    // ========================================================================

    /**
     * 生成学习计划
     */
    @PostMapping("/learning-plans")
    public ApiResult<RagService.AnswerResult> generateLearningPlan(@RequestBody Map<String, String> body) {
        String topic = body.getOrDefault("topic", "");
        if (topic.isBlank()) {
            return ApiResult.fail(400, "主题不能为空");
        }

        JwtService.UserPrincipal user = getCurrentUser();
        RagService.AnswerResult result = ragService.generateLearningPlan(
                topic, user.userId(), user.role(), getDepartmentId(user));
        return ApiResult.success(result);
    }

    // ========================================================================
    // 会话与引用
    // ========================================================================

    /**
     * 查询回答引用来源
     */
    @GetMapping("/sessions/{id}/citations")
    public ApiResult<List<OaAiCitation>> getCitations(@PathVariable Long id) {
        return ApiResult.success(ragService.getCitations(id));
    }

    // ========================================================================
    // 批量导入
    // ========================================================================

    /**
     * 批量导入知识库文档（管理工具）
     * @param directoryPath 知识库目录路径，如 D:/QQ/download/OA-service/知识库word
     */
    @PostMapping("/batch-import")
    public ApiResult<BatchIndexService.BatchResult> batchImport(
            @RequestParam(defaultValue = "D:/QQ/download/OA-service/知识库word") String directoryPath,
            @RequestParam(defaultValue = "ALL") String accessScope,
            @RequestParam(defaultValue = "1") Integer minRoleLevel) {
        Long uploaderId = getCurrentUserId();
        BatchIndexService.BatchResult result = batchIndexService.batchImport(
                directoryPath, accessScope, minRoleLevel, uploaderId);
        return ApiResult.success(result);
    }

    // ========================================================================
    // 健康检查
    // ========================================================================

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        return ApiResult.success(Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis()));
    }

    // ========================================================================
    // 兼容旧接口
    // ========================================================================

    /**
     * 旧版上传接口（兼容）
     */
    @PostMapping("/index")
    public ApiResult<Map<String, Object>> indexDocument(@RequestParam("file") MultipartFile file) {
        return uploadSource(file, null, null, null, "ALL", null, 1);
    }

    // ========================================================================
    // 辅助方法
    // ========================================================================

    private JwtService.UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtService.UserPrincipal principal) {
            return principal;
        }
        return new JwtService.UserPrincipal(0L, "anonymous", "SUPER_ADMIN", null, "");
    }

    private Long getCurrentUserId() {
        return getCurrentUser().userId();
    }

    /** 通过EmployeeClient获取用户部门ID */
    private Long getDepartmentId(JwtService.UserPrincipal user) {
        if (user.employeeId() == null) return null;
        try {
            ApiResult<EmployeeResponse> res = employeeClient.getById(user.employeeId());
            if (res != null && res.getData() != null) {
                return res.getData().getDepartmentId();
            }
        } catch (Exception e) {
            log.warn("获取部门ID失败: userId={}, employeeId={}", user.userId(), user.employeeId());
        }
        return null;
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1) : "";
    }
}
