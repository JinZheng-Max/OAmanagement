package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "考勤记录分页与统计聚合响应")
public record AttendancePageResult(
        @Schema(description = "当前页考勤记录列表")
        List<AttendanceResponse> records,
        @Schema(description = "总记录条数", example = "50")
        long total,
        @Schema(description = "每页大小", example = "10")
        int size,
        @Schema(description = "当前页码", example = "1")
        int current,
        @Schema(description = "总页数", example = "5")
        long pages,
        @Schema(description = "考勤状态统计汇总数据 (checkedIn / checkedOut / unchecked 等数量)")
        Map<String, Long> statistics) {
    public static AttendancePageResult of(List<AttendanceResponse> records, long total, PageQuery query, Map<String, Long> statistics) {
        long pages = query.size() > 0 ? (total + query.size() - 1) / query.size() : 0;
        return new AttendancePageResult(records, total, query.size(), query.page(), pages, statistics);
    }
}
