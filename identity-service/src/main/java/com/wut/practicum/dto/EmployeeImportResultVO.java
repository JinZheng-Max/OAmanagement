package com.wut.practicum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "员工批量导入结果")
public record EmployeeImportResultVO(
        @Schema(description = "解析到的数据总条数")
        int totalCount,

        @Schema(description = "成功导入条数")
        int successCount,

        @Schema(description = "失败条数")
        int failCount,

        @Schema(description = "错误及排错明细")
        List<String> errorMessages,

        @Schema(description = "成功开通账号明细（包含用户名和初始密码）")
        List<String> successDetails
) {
    public EmployeeImportResultVO(int totalCount, int successCount, int failCount, List<String> errorMessages) {
        this(totalCount, successCount, failCount, errorMessages, List.of());
    }
}
