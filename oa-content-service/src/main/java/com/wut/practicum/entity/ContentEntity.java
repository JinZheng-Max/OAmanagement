package com.wut.practicum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentEntity {
    private Long id;
    private String type;         // ANNOUNCEMENT / POLICY
    private String title;        // 标题
    private String category;     // 分类
    private String body;         // 正文内容
    private String status;       // DRAFT / PUBLISHED / UNPUBLISHED
    private String scope;        // ALL / DEPARTMENT
    private Long accessDepartmentId; // 可见部门ID
    private Long publisherId;    // 发布人ID
    private LocalDateTime publishTime; // 发布时间
    private Integer version;     // 版本号
    private Long viewCount;      // 访问量
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
