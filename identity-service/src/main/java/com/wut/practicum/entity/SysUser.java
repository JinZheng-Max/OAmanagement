package com.wut.practicum.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "系统用户")
public class SysUser {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "密码哈希值(BCrypt)")
    private String passwordHash;

    @Schema(description = "角色: ADMIN/EMPLOYEE")
    private String role;

    @Schema(description = "状态: 1-启用 0-停用")
    private Integer status;

    @Schema(description = "关联的员工ID")
    private Long employeeId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // 关联的员工详细信息（可为null）
    @Schema(description = "关联的员工详细档案")
    private OaEmployee employee;
}
