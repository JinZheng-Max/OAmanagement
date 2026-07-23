package com.wut.practicum.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "部门考勤规则实体")
@Data
public class OaAttendanceRule {

    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称 (视图关联可填)")
    private String departmentName;

    @Schema(description = "考勤场次名称 (如 上午场, 下午场)", example = "上午场")
    private String sessionName;

    @Schema(description = "允许签到开始时间(HH:mm)", example = "08:50")
    private String checkInStartTime;

    @Schema(description = "正常签到截止时间(HH:mm, 超过则为迟到)", example = "09:10")
    private String normalCheckInEndTime;

    @Schema(description = "签到最晚截止时间(HH:mm)", example = "12:10")
    private String checkInEndTime;

    @Schema(description = "正常签退最早时间(HH:mm, 早于则为早退)", example = "11:50")
    private String normalCheckOutStartTime;

    @Schema(description = "签退最晚截止时间(HH:mm)", example = "12:10")
    private String checkOutEndTime;

    @Schema(description = "启用状态 (1-启用 0-禁用)", example = "1")
    private Integer enabled;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
