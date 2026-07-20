package com.wut.practicum.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OaAttendance {
    private Long id;
    private Long employeeId;
    private String workDate; // yyyy-MM-dd
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String checkInIp;
    private String checkOutIp;
    private String status; // UNCHECKED, CHECKED_IN, CHECKED_OUT
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // Field from join query
    private String employeeName;
}
