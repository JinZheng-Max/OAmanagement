package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增员工请求 DTO
 *
 * 使用 Jakarta Validation 注解来做参数校验：
 * @NotBlank  — 不能为 null 且至少有一个非空格字符
 * @Size      — 限制字符串长度
 * @Pattern   — 正则校验格式
 */
@Schema(description = "新增员工请求")
public record EmployeeCreateRequest(
        @NotBlank(message = "工号不能为空")
        @Size(max = 50, message = "工号最长50个字符")
        @Schema(description = "员工工号（唯一）", example = "EMP005")
        String employeeNo,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名最长50个字符")
        @Schema(description = "员工姓名", example = "张三")
        String name,

        @Schema(description = "所属部门ID", example = "1")
        Long departmentId,

        @Size(max = 100, message = "职位最长100个字符")
        @Schema(description = "职位", example = "前端开发工程师")
        String position,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Schema(description = "联系手机", example = "13800000001")
        String phone,

        @Schema(description = "入职日期(yyyy-MM-dd)", example = "2026-07-01")
        String hireDate) {
}
