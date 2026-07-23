package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "审批请假请求")
public record LeaveAuditRequest(
        @NotBlank @Schema(description = "审批动作: APPROVED/REJECTED", example = "APPROVED")
        String action,
        @Schema(description = "审批意见", example = "同意请假，注意休息")
        String comment) {}
