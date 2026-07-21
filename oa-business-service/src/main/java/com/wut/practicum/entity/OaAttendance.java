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
    
    @Schema(description = "工作日期 (yyyy-MM-dd)", example = "2026-07-20")
    private String workDate;
    
    @Schema(description = "签到打卡时间", example = "2026-07-20T08:50:00")
    private LocalDateTime checkIn;
    
    @Schema(description = "签退打卡时间", example = "2026-07-20T18:05:00")
    private LocalDateTime checkOut;
    
    @Schema(description = "签到IP地址", example = "192.168.1.100")
    private String checkInIp;
    
    @Schema(description = "签退IP地址", example = "192.168.1.100")
    private String checkOutIp;
    
    @Schema(description = "考勤状态 (UNCHECKED / CHECKED_IN / CHECKED_OUT / LEAVE_EARLY)", example = "CHECKED_IN")
    private String status;
    
    @Schema(description = "记录创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "记录更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "员工姓名 (表关联查询填充)")
    private String employeeName;
}
