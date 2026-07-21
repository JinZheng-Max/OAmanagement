package com.wut.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsReindexResultVO {
    private long total;
    private long successCount;
    private long failureCount;
    private long costMillis;
    private String message;
}
