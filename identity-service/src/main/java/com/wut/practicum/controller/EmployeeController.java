package com.wut.practicum.controller;

import com.wut.practicum.common.ApiResult;
import com.wut.practicum.dto.*;
import com.wut.practicum.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ApiResult<PageResult<EmployeeResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword) {
        PageQuery query = new PageQuery(page, size);
        PageResult<EmployeeResponse> result = employeeService.page(query, departmentId, keyword);
        return ApiResult.success(result);
    }

    @GetMapping("/{id}")
    public ApiResult<EmployeeResponse> getById(@PathVariable Long id) {
        return ApiResult.success(employeeService.getById(id));
    }

    @PostMapping
    public ApiResult<EmployeeResponse> create(@Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse result = employeeService.create(request);
        return ApiResult.success(result, "新增成功");
    }

    @PutMapping("/{id}")
    public ApiResult<EmployeeResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody EmployeeUpdateRequest request) {
        EmployeeResponse result = employeeService.update(id, request);
        return ApiResult.success(result, "更新成功");
    }

    @PostMapping("/{id}/account")
    public ApiResult<String> createAccount(@PathVariable Long id,
                                           @Valid @RequestBody CreateAccountRequest request) {
        String tempPassword = employeeService.createAccount(id, request);
        return ApiResult.success(tempPassword, "账号开通成功，初始密码为: " + tempPassword);
    }

    @PutMapping("/account/{userId}/reset-password")
    public ApiResult<String> resetPassword(@PathVariable Long userId) {
        String newPassword = employeeService.resetPassword(userId);
        return ApiResult.success(newPassword, "密码已重置为: " + newPassword);
    }
}
