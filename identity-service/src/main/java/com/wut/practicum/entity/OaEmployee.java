package com.wut.practicum.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "员工档案")
public class OaEmployee {
    @Schema(description = "员工ID")
    private Long id;

    @Schema(description = "员工工号")
    private String employeeNo;

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "所属部门ID")
    private Long departmentId;

    @Schema(description = "所属部门名称")
    private String departmentName; // 冗余字段或JOIN查询字段

    @Schema(description = "职位")
    private String position;

    @Schema(description = "联系手机")
    private String phone;

    @Schema(description = "身份证号（18位）")
    private String idNumber;

    @Schema(description = "电子邮箱")
    private String email;

    @Schema(description = "在职状态: 1-在职 0-离职")
    private Integer status;

    @Schema(description = "是否已开通系统账号（仅查询时填充，非数据库字段）")
    private Boolean hasAccount;

    @Schema(description = "关联的系统用户ID（仅查询时填充，用于重置密码等操作）")
    private Long userId;

    @Schema(description = "系统账号角色: SUPER_ADMIN / DEPT_MANAGER / EMPLOYEE")
    private String role;

    @Schema(description = "入职日期")
    private LocalDate hireDate;

    @Schema(description = "工作年限（年）")
    private java.math.BigDecimal workYears;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
