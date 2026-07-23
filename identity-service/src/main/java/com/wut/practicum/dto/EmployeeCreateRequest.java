package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增员工请求 DTO
 */
@Schema(description = "新增员工请求")
public record EmployeeCreateRequest(
        @NotBlank(message = "工号不能为空")
        @Size(min = 2, max = 50, message = "工号长度2~50个字符")
        @Schema(description = "员工工号（唯一，必填）", example = "EMP0001")
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

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确（需11位手机号）")
        @Schema(description = "联系手机", example = "13800000001")
        String phone,

        @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$", message = "身份证号格式不正确（需18位）")
        @Schema(description = "身份证号（18位）", example = "110101199001011234")
        String idNumber,

        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确（需包含@）")
        @Schema(description = "电子邮箱", example = "zhangsan@company.com")
        String email,

        @Schema(description = "入职日期(yyyy-MM-dd)", example = "2026-07-01")
        String hireDate,

        @Schema(description = "工作年限（年）", example = "3.5")
        java.math.BigDecimal workYears) {
}
