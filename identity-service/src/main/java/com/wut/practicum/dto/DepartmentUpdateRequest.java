package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "编辑部门请求")
public record DepartmentUpdateRequest(
        @Size(max = 50) @Schema(description = "部门编码", example = "DEPT_MARKET")
        String code,
        @Size(max = 100) @Schema(description = "部门名称", example = "市场部")
        String name,
        @Schema(description = "负责人员工ID", example = "2")
        Long leaderId,
        @Schema(description = "排序号", example = "5")
        Integer sort) {}
