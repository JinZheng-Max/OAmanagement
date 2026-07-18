package com.wut.practicum.dto;

import com.wut.practicum.entity.OaEmployee;

import java.time.LocalDate;

public record UserProfileResponse(Long id, String username, String role, Integer status, Long employeeId, EmployeeInfo employee) {
    public static UserProfileResponse from(Long id, String username, String role, Integer status, Long employeeId, OaEmployee employee) {
        EmployeeInfo info = employee == null ? null : new EmployeeInfo(employee.getId(), employee.getEmployeeNo(), employee.getName(),
                employee.getDepartmentId(), employee.getDepartmentName(), employee.getPosition(), employee.getPhone(), employee.getStatus(), employee.getHireDate());
        return new UserProfileResponse(id, username, role, status, employeeId, info);
    }
    public record EmployeeInfo(Long id, String employeeNo, String name, Long departmentId, String departmentName,
                               String position, String phone, Integer status, LocalDate hireDate) {}
}
