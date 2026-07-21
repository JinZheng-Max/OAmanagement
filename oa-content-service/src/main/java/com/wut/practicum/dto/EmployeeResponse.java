package com.wut.practicum.dto;

import lombok.Data;

@Data
public class EmployeeResponse {
    private Long id;
    private String employeeNo;
    private String name;
    private Long departmentId;
    private String position;
    private String phone;
    private Integer status;
}
