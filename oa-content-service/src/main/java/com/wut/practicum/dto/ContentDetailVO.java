package com.wut.practicum.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentDetailVO {
    private Long id;
    private String type;
    private String title;
    private String category;
    private String body;
    private String status;
    private String scope;
    private Long accessDepartmentId;
    private Long publisherId;
    private LocalDateTime publishTime;
    private Integer version;
    private Long viewCount;
    private String highlightTitle; // 高亮标题 (仅检索结果使用)
    private String highlightBody;  // 高亮片段 (仅检索结果使用)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
