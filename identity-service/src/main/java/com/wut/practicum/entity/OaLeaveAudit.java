package com.wut.practicum.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OaLeaveAudit {
    private Long id;
    private Long leaveId;
    private Long auditorId;

    @Schema(description = "审批人姓名（JOIN查询）")
    private String auditorName;
    private String action;       // APPROVED/REJECTED
    private String comment;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
}
