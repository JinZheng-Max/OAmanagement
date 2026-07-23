package com.wut.practicum.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "请假申请")
public class OaLeave {
    private Long id;
    private Long applicantId;
    private String type;        // ANNUAL/SICK/PERSONAL/OTHER
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String status;      // PENDING/APPROVED/REJECTED/WITHDRAWN

    @Schema(description = "申请人姓名（JOIN查询）")
    private String applicantName;

    @Schema(description = "申请人工号（JOIN查询）")
    private String applicantNo;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
