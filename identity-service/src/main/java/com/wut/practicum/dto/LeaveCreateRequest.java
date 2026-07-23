package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "提交请假申请请求")
public record LeaveCreateRequest(
        @NotBlank @Schema(description = "请假类型: ANNUAL/SICK/PERSONAL/OTHER", example = "SICK")
        String type,
        @NotBlank @Schema(description = "开始时间", example = "2026-07-20 09:00:00")
        String startTime,
        @NotBlank @Schema(description = "结束时间", example = "2026-07-21 18:00:00")
        String endTime,
        @NotBlank @Schema(description = "请假原因", example = "感冒发烧需要休息")
        String reason) {}
