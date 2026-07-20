package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "修改部门状态请求")
public record DepartmentStatusRequest(
        @NotNull @Schema(description = "状态: 1-启用 0-停用", example = "0")
        Integer status) {}
