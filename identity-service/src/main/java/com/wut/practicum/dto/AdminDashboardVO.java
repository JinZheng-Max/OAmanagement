package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "超级管理员运维数据大屏统计响应包")
public class AdminDashboardVO {

    @Schema(description = "在册员工总数")
    private Long employeeCount;

    @Schema(description = "集团下设部门数")
    private Long departmentCount;

    @Schema(description = "全公司待审批请假总数")
    private Long pendingLeaveCount;

    @Schema(description = "全公司待补录签到总数")
    private Long pendingRecordCount;

    @Schema(description = "各部门待审批请假数明细")
    private List<DeptStatItem> departmentPendingLeaves;

    @Schema(description = "各部门待补录签到数明细")
    private List<DeptStatItem> departmentPendingRecords;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeptStatItem {
        private Long departmentId;
        private String departmentName;
        private Long count;
        private String updateTime;
    }
}
