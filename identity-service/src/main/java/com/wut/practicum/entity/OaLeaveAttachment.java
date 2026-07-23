package com.wut.practicum.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OaLeaveAttachment {
    private Long id;
    private Long leaveId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createTime;
}
