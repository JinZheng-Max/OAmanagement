package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * 当前员工所属部门详情响应
 */
@Schema(description = "我的部门详情")
public record MyDepartmentResponse(
        @Schema(description = "部门信息") DepartmentResponse department,
        @Schema(description = "部门成员列表") List<EmployeeResponse> employees,
        @Schema(description = "部门总人数") int totalCount,
        @Schema(description = "各岗位人数统计（岗位名 → 人数）") Map<String, Integer> positionCounts,
        @Schema(description = "部门负责人信息（含联系方式）") EmployeeResponse leader) {
}
