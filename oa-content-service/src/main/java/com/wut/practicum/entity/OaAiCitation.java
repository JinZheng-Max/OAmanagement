package com.wut.practicum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI回答引用记录 - oa_ai_citation
 */
@Data
public class OaAiCitation {
    private Long id;
    private Long sessionId;
    private String sourceType = "KNOWLEDGE_FILE";
    private Long contentId;
    private Long sourceId;
    private Long chunkId;
    private String sourceTitle;
    private String fragmentSummary;
    private Double score;
    private Integer sourceVersion;
    private LocalDateTime createTime;
}
