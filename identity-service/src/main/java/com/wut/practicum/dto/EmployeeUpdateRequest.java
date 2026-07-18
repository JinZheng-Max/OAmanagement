package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 编辑员工请求 DTO
 *
 * 注意：这里所有字段都是可选的（没有 @NotBlank），
 * 前端可以只传需要修改的字段。
 */
@Schema(description = "编辑员工请求")
public record EmployeeUpdateRequest(
        @Size(max = 50, message = "姓名最长50个字符")
        @Schema(description = "员工姓名", example = "张三")
        String name,

        @Schema(description = "所属部门ID", example = "2")
        Long departmentId,

        @Size(max = 100, message = "职位最长100个字符")
        @Schema(description = "职位", example = "高级技术经理")
        String position,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Schema(description = "联系手机", example = "13800000099")
        String phone,

        @Schema(description = "在职状态: 1-在职 0-离职", example = "1")
        Integer status,

        @Schema(description = "入职日期(yyyy-MM-dd)", example = "2026-07-01")
        String hireDate) {
}
