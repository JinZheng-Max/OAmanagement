package com.wut.practicum.dto;

import com.wut.practicum.entity.SysDepartment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "部门信息")
public record DepartmentResponse(
        Long id, String code, String name, Long leaderId, String leaderName,
        Integer status, Integer sort,
        LocalDateTime createTime, LocalDateTime updateTime) {

    public static DepartmentResponse from(SysDepartment dept) {
        if (dept == null) return null;
        return new DepartmentResponse(
                dept.getId(), dept.getCode(), dept.getName(),
                dept.getLeaderId(), dept.getLeaderName(),
                dept.getStatus(), dept.getSort(),
                dept.getCreateTime(), dept.getUpdateTime());
    }
}
