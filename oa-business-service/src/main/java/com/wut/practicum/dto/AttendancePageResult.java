package com.wut.practicum.dto;

import java.util.List;
import java.util.Map;

public record AttendancePageResult(
        List<AttendanceResponse> records,
        long total,
        int size,
        int current,
        long pages,
        Map<String, Long> statistics) {
    public static AttendancePageResult of(List<AttendanceResponse> records, long total, PageQuery query, Map<String, Long> statistics) {
        long pages = query.size() > 0 ? (total + query.size() - 1) / query.size() : 0;
        return new AttendancePageResult(records, total, query.size(), query.page(), pages, statistics);
    }
}
