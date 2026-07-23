package com.wut.practicum.service;

import com.wut.practicum.entity.OaAiSource;
import com.wut.practicum.mapper.OaAiSourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 批量索引知识库文档
 * 从指定目录批量读取docx/pdf文件，上传并索引到RAG系统
 */
@Service
public class BatchIndexService {
    private static final Logger log = LoggerFactory.getLogger(BatchIndexService.class);

    private final RagService ragService;
    private final OaAiSourceMapper aiSourceMapper;

    public BatchIndexService(RagService ragService, OaAiSourceMapper aiSourceMapper) {
        this.ragService = ragService;
        this.aiSourceMapper = aiSourceMapper;
    }

    /**
     * 批量导入知识库文件
     * @param directoryPath 知识库目录路径
     * @param accessScope 访问范围
     * @param minRoleLevel 最低角色等级
     * @param uploaderId 上传人ID（默认admin）
     * @return 导入结果统计
     */
    public BatchResult batchImport(String directoryPath, String accessScope,
                                    Integer minRoleLevel, Long uploaderId) {
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return new BatchResult(0, 0, "目录不存在: " + directoryPath);
        }

        // 获取所有doc/docx/pdf文件
        List<File> files;
        try (Stream<Path> stream = Files.list(Paths.get(directoryPath))) {
            files = stream
                    .map(Path::toFile)
                    .filter(f -> f.isFile() && isSupportedFile(f.getName()))
                    .sorted(Comparator.comparing(File::getName))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new BatchResult(0, 0, "读取目录失败: " + e.getMessage());
        }

        if (files.isEmpty()) {
            return new BatchResult(0, 0, "目录下没有支持的文档文件");
        }

        log.info("批量导入开始: {} 个文件, 目录={}", files.size(), directoryPath);

        int success = 0;
        int fail = 0;
        List<String> failedFiles = new ArrayList<>();

        for (File file : files) {
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String title = file.getName().replaceAll("\\.[^.]*$", "");

                // 推断分类
                String category = inferCategory(file.getName(), title);

                OaAiSource source = ragService.saveUploadedFile(
                        fileBytes, file.getName(),
                        title, category, null,
                        accessScope, null, minRoleLevel, uploaderId);

                // 异步启动索引
                ragService.asyncIndexDocument(source.getId());
                success++;
                log.info("  ✅ {} -> id={}, category={}", file.getName(), source.getId(), category);
            } catch (Exception e) {
                fail++;
                failedFiles.add(file.getName());
                log.error("  ❌ {} - {}", file.getName(), e.getMessage());
            }
        }

        log.info("批量导入完成: 成功={}, 失败={}", success, fail);
        return new BatchResult(success, fail, failedFiles.isEmpty() ? null :
                "失败文件: " + String.join(", ", failedFiles));
    }

    /** 根据文件名推断文档分类 */
    private String inferCategory(String fileName, String title) {
        if (title.contains("企业文化") || title.contains("员工行为")) return "企业文化";
        if (title.contains("新人培训") || title.contains("入职培训")) return "新人培训";
        if (title.contains("技术部") || title.contains("软件开发") || title.contains("生产运维")) return "技术部资料";
        if (title.contains("人事") || title.contains("考勤") || title.contains("休假") || title.contains("员工发展")) return "人事制度";
        if (title.contains("财务") || title.contains("报销")) return "财务制度";
        if (title.contains("行政") || title.contains("资产管理")) return "行政管理";
        if (title.contains("法务") || title.contains("合同")) return "法务合规";
        if (title.contains("信息安全") || title.contains("数据保护")) return "信息安全";
        if (fileName.contains("人力行政") || fileName.contains("市场销售")) return "部门资料";
        if (title.contains("部门资料")) {
            if (title.startsWith("01")) return "人事制度";
            if (title.startsWith("02")) return "财务制度";
            if (title.startsWith("03")) return "行政管理";
            if (title.startsWith("04")) return "技术部资料";
            return "部门资料";
        }
        return "其他";
    }

    private boolean isSupportedFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".docx") || lower.endsWith(".doc") || lower.endsWith(".pdf");
    }

    public record BatchResult(int success, int fail, String message) {}
}
