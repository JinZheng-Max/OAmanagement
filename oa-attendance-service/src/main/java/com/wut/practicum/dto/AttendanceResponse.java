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
        @Schema(description = "部门ID", example = "1")
        Long departmentId,
        @Schema(description = "部门名称", example = "技术部")
        String departmentName,
        @Schema(description = "工作日期 (yyyy-MM-dd)", example = "2026-07-20")
        String workDate,
        @Schema(description = "考勤场次名称", example = "上午场")
        String sessionName,
        @Schema(description = "签到打卡时间", example = "2026-07-20T08:50:00")
        LocalDateTime checkIn,
        @Schema(description = "签退打卡时间", example = "2026-07-20T18:05:00")
        LocalDateTime checkOut,
        @Schema(description = "签到IP地址", example = "192.168.1.50")
        String checkInIp,
        @Schema(description = "签退IP地址", example = "192.168.1.50")
        String checkOutIp,
        @Schema(description = "签到允许开始时间 (HH:mm)", example = "08:50")
        String checkInStartTime,
        @Schema(description = "正常签到截止时间 (HH:mm)", example = "09:10")
        String normalCheckInEndTime,
        @Schema(description = "签到最晚截止时间 (HH:mm)", example = "12:10")
        String checkInEndTime,
        @Schema(description = "正常签退最早时间 (HH:mm)", example = "11:50")
        String normalCheckOutStartTime,
        @Schema(description = "签退最晚截止时间 (HH:mm)", example = "12:10")
        String checkOutEndTime,
        @Schema(description = "考勤状态：CHECKED_IN / LATE / EARLY_LEAVE / CHECKED_OUT / UNCHECKED / ABSENT / REPLENISHED", example = "CHECKED_OUT")
        String status,
        @Schema(description = "补签状态：NONE / PENDING / APPROVED / REJECTED", example = "NONE")
        String replenishStatus,
        @Schema(description = "补签原因")
        String replenishReason,
        @Schema(description = "审批人用户ID")
        Long approverId,
        @Schema(description = "审批人姓名")
        String approverName,
        @Schema(description = "审批时间")
        LocalDateTime approveTime,
        @Schema(description = "审批意见")
        String approveComment,
        @Schema(description = "记录创建时间")
        LocalDateTime createTime,
        @Schema(description = "记录最后更新时间")
        LocalDateTime updateTime
) {}
