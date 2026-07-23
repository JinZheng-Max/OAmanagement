package com.wut.practicum.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class OaAiSourceChunk {
    private Long id; private Long sourceId; private Integer chunkNo;
    private String sectionTitle; private Integer pageNo;
    private String contentText; private String contentHash;
    private Integer tokenCount; private String vectorKey;
    private String metadataJson; private String status;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}
