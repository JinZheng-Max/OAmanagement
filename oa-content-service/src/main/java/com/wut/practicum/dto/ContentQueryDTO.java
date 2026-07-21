package com.wut.practicum.dto;

import lombok.Data;

@Data
public class ContentQueryDTO {
    private String type;     // ANNOUNCEMENT / POLICY
    private String category;
    private String status;   // 管理员可用: DRAFT / PUBLISHED / UNPUBLISHED
    private Integer page = 1;
    private Integer size = 10;
}
