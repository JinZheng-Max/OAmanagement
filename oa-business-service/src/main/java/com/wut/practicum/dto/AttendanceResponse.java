package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "考勤打卡记录响应数据")
public record AttendanceResponse(
        @Schema(description = "考勤记录ID", example = "101")
        Long id,
        @Schema(description = "员工ID", example = "1001")
        Long employeeId,
        @Schema(description = "员工姓名", example = "张三")
        String employeeName,
        @Schema(description = "工作日期 (yyyy-MM-dd)", example = "2026-07-20")
        String workDate,
        @Schema(description = "签到打卡时间", example = "2026-07-20T08:50:00")
        LocalDateTime checkIn,
        @Schema(description = "签退打卡时间", example = "2026-07-20T18:05:00")
        LocalDateTime checkOut,
        @Schema(description = "签到IP地址", example = "192.168.1.50")
        String checkInIp,
        @Schema(description = "签退IP地址", example = "192.168.1.50")
        String checkOutIp,
        @Schema(description = "考勤状态：CHECKED_IN / CHECKED_OUT / UNCHECKED / LEAVE_EARLY", example = "CHECKED_OUT")
        String status,
        @Schema(description = "记录创建时间")
        LocalDateTime createTime,
        @Schema(description = "记录最后更新时间")
        LocalDateTime updateTime) {}
