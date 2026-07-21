package com.wut.practicum.service;

import com.wut.practicum.dto.AttendancePageResult;
import com.wut.practicum.dto.AttendanceResponse;
import com.wut.practicum.dto.PageQuery;

public interface AttendanceService {
    AttendanceResponse checkIn(Long employeeId, String clientIp);
    
    AttendanceResponse checkOut(Long employeeId, String clientIp);
    
    AttendancePageResult queryPersonalRecords(Long employeeId, String startDate, String endDate, PageQuery query, Long requestEmployeeId);
    
    AttendancePageResult queryAdminRecords(Long employeeId, Long departmentId, String startDate, String endDate, String status, PageQuery query);

    int publishDailyAttendance();

    AttendanceResponse saveOrUpdateAdminRecord(com.wut.practicum.entity.OaAttendance attendance);

    AttendanceResponse updateAdminRecord(Long id, com.wut.practicum.entity.OaAttendance attendance);
}
