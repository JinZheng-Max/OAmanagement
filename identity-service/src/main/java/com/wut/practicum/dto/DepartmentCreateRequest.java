package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "新增部门请求")
public record DepartmentCreateRequest(
        @NotBlank(message = "部门编码不能为空")
        @Size(max = 50) @Schema(description = "部门编码（唯一）", example = "DEPT_MARKET")
        String code,

        @NotBlank(message = "部门名称不能为空")
        @Size(max = 100) @Schema(description = "部门名称", example = "市场部")
        String name,

        @Schema(description = "负责人员工ID", example = "1")
        Long leaderId,

        @Schema(description = "排序号", example = "5")
        Integer sort) {}
