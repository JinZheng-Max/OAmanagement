package com.wut.practicum.dto;

import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String workDate,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        String checkInIp,
        String checkOutIp,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
