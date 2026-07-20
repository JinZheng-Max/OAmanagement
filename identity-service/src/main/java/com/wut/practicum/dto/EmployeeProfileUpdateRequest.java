package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 员工自助修改个人信息请求
 * 普通员工只能修改姓名和手机号，部门和职位不可改
 */
@Schema(description = "员工自助修改个人信息请求")
public record EmployeeProfileUpdateRequest(
        @Size(max = 50, message = "姓名最长50个字符")
        @Schema(description = "员工姓名", example = "张三")
        String name,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Schema(description = "联系手机", example = "13800000001")
        String phone) {
}
