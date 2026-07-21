package com.wut.practicum.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String employeeNo,
        String name,
        Long departmentId,
        String departmentName,
        String position,
        String phone,
        Integer status,
        LocalDate hireDate,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
