package com.wut.practicum.dto;

import com.wut.practicum.entity.OaEmployee;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工信息响应 DTO
 *
 * 返回给前端的数据，从实体 OaEmployee 转换而来，
 * 避免直接把数据库实体暴露给前端。
 */
@Schema(description = "员工信息")
public record EmployeeResponse(
        @Schema(description = "员工ID") Long id,
        @Schema(description = "员工工号") String employeeNo,
        @Schema(description = "员工姓名") String name,
        @Schema(description = "所属部门ID") Long departmentId,
        @Schema(description = "所属部门名称") String departmentName,
        @Schema(description = "职位") String position,
        @Schema(description = "联系手机") String phone,
        @Schema(description = "在职状态: 1-在职 0-离职") Integer status,
        @Schema(description = "是否已开通系统账号") Boolean hasAccount,
        @Schema(description = "关联的系统用户ID（已开通账号时有效）") Long userId,
        @Schema(description = "系统账号角色: SUPER_ADMIN / DEPT_MANAGER / EMPLOYEE") String role,
        @Schema(description = "入职日期") LocalDate hireDate,
        @Schema(description = "创建时间") LocalDateTime createTime,
        @Schema(description = "更新时间") LocalDateTime updateTime) {

    /**
     * 静态工厂方法：将实体转换为响应 DTO
     *
     * static 方法可以通过类名直接调用：EmployeeResponse.from(employee)
     * 这是 DTO 转换的常见模式
     */
    public static EmployeeResponse from(OaEmployee emp) {
        if (emp == null) return null;
        return new EmployeeResponse(
                emp.getId(), emp.getEmployeeNo(), emp.getName(),
                emp.getDepartmentId(), emp.getDepartmentName(),
                emp.getPosition(), emp.getPhone(), emp.getStatus(),
                emp.getHasAccount() != null && emp.getHasAccount(),
                emp.getUserId(), emp.getRole(),
                emp.getHireDate(), emp.getCreateTime(), emp.getUpdateTime()
        );
    }
}
