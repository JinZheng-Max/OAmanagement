package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 为员工开通账号的请求 DTO
 */
@Schema(description = "开通账号请求")
public record CreateAccountRequest(
        @Schema(description = "登录账号（不传则默认使用员工工号）", example = "chenqi")
        String username,

        @Schema(description = "分配系统角色: SUPER_ADMIN / DEPT_MANAGER / EMPLOYEE (默认 EMPLOYEE)", example = "DEPT_MANAGER")
        String role) {
}
