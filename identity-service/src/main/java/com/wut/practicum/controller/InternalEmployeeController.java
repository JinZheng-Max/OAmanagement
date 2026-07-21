package com.wut.practicum.controller;

import com.wut.practicum.common.ApiResult;
import com.wut.practicum.dto.EmployeeResponse;
import com.wut.practicum.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "微服务内部接口", description = "仅供内部微服务(如考勤服务)调用的私有接口，网关已禁止外部直接访问")
@RestController
@RequestMapping("/internal/employees")
@RequiredArgsConstructor
public class InternalEmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "内部根据ID获取员工基本信息")
    @GetMapping("/{id}")
    public ApiResult<EmployeeResponse> getById(@PathVariable Long id) {
        return ApiResult.success(employeeService.getById(id));
    }
}
