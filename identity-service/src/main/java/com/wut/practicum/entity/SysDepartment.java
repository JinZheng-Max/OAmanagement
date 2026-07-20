package com.wut.practicum.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "部门")
public class SysDepartment {
    @Schema(description = "部门ID")
    private Long id;

    @Schema(description = "部门编码（唯一）")
    private String code;

    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "部门负责人ID（员工ID）")
    private Long leaderId;

    @Schema(description = "负责人姓名（JOIN查询）")
    private String leaderName;

    @Schema(description = "状态: 1-启用 0-停用")
    private Integer status;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
