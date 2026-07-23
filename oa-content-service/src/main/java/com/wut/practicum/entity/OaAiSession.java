package com.wut.practicum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI问答会话 - oa_ai_session
 */
@Data
public class OaAiSession {
    private Long id;
    private Long userId;
    private String requestRole;
    private Long requestDepartmentId;
    private String permissionDigest;
    private String question;
    private String answerSummary;
    private String status = "ANSWERED";
    private LocalDateTime createTime;
}
