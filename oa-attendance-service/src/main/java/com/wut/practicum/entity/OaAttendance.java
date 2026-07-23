package com.wut.practicum.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "考勤实体 / 补录请求参数实体")
@Data
public class OaAttendance {
    @Schema(description = "考勤记录ID (编辑时填写)")
    private Long id;
    
    @Schema(description = "员工ID (补录时必须指定)")
    private Long employeeId;
    
    @Schema(description = "所属部门ID")
    private Long departmentId;

    @Schema(description = "工作日期 (yyyy-MM-dd)", example = "2026-07-20")
    private String workDate;
    
    @Schema(description = "考勤场次名称 (如 上午场, 下午场)", example = "上午场")
    private String sessionName;

    @Schema(description = "签到打卡时间", example = "2026-07-20T08:50:00")
    private LocalDateTime checkIn;
    
    @Schema(description = "签退打卡时间", example = "2026-07-20T18:05:00")
    private LocalDateTime checkOut;
    
    @Schema(description = "签到IP地址", example = "192.168.1.100")
    private String checkInIp;
    
    @Schema(description = "签退IP地址", example = "192.168.1.100")
    private String checkOutIp;

    @Schema(description = "签到开始时间(HH:mm)", example = "08:50")
    private String checkInStartTime;

    @Schema(description = "正常签到截止时间(HH:mm, 超过为迟到)", example = "09:10")
    private String normalCheckInEndTime;

    @Schema(description = "签到最晚截止时间(HH:mm)", example = "12:10")
    private String checkInEndTime;

    @Schema(description = "正常签退最早时间(HH:mm, 早于为早退)", example = "11:50")
    private String normalCheckOutStartTime;

    @Schema(description = "签退最晚截止时间(HH:mm)", example = "12:10")
    private String checkOutEndTime;
    
    @Schema(description = "考勤状态 (UNCHECKED / CHECKED_IN / LATE / EARLY_LEAVE / CHECKED_OUT / ABSENT / REPLENISHED)", example = "CHECKED_IN")
    private String status;
    
    @Schema(description = "补签状态 (NONE / PENDING / APPROVED / REJECTED)", example = "NONE")
    private String replenishStatus;

    @Schema(description = "补签申请原因", example = "因公差无法即时打卡，申请补签")
    private String replenishReason;

    @Schema(description = "补签审批人ID")
    private Long approverId;

    @Schema(description = "补签审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "补签审批意见")
    private String approveComment;

    @Schema(description = "记录创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "记录更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "员工姓名 (表关联查询填充)")
    private String employeeName;

    @Schema(description = "部门名称 (表关联查询填充)")
    private String departmentName;

    @Schema(description = "审批人姓名 (表关联查询填充)")
    private String approverName;
}
