package com.wut.practicum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * RAG企业知识来源 - oa_ai_source
 */
@Data
public class OaAiSource {
    private Long id;
    private String title;
    private String category;
    private String description;

    private String originalFileName;
    private String storedFileName;
    private String fileUrl;
    private String fileExtension;
    private String mimeType;
    private Long fileSize;
    private String fileHash;
    private String storageProvider = "LOCAL";

    private String extractedText;
    private String parseStatus = "PENDING";
    private String indexStatus = "PENDING";
    private String errorMessage;

    private String accessScope = "ALL";
    private String accessDepartmentId;
    private Integer minRoleLevel = 1;

    private String status = "ENABLED";
    private Integer version = 1;
    private Long uploaderId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
