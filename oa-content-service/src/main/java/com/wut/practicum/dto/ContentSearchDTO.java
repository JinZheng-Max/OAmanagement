package com.wut.practicum.dto;

import lombok.Data;

@Data
public class ContentSearchDTO {
    private String keyword;  // 检索关键词
    private String type;     // ANNOUNCEMENT / POLICY
    private String category; // 分类
    private String startDate; // 开始发布时间 yyyy-MM-dd
    private String endDate;   // 结束发布时间 yyyy-MM-dd
    private Integer page = 1;
    private Integer size = 10;
}
