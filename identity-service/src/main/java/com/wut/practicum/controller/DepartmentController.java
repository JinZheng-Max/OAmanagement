package com.wut.practicum.controller;

import com.wut.practicum.common.ApiResult;
import com.wut.practicum.dto.*;
import com.wut.practicum.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ApiResult<PageResult<DepartmentResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        PageQuery query = new PageQuery(page, size);
        return ApiResult.success(departmentService.page(query, keyword));
    }

    @GetMapping("/{id}")
    public ApiResult<DepartmentResponse> getById(@PathVariable Long id) {
        return ApiResult.success(departmentService.getById(id));
    }

    @PostMapping
    public ApiResult<DepartmentResponse> create(@Valid @RequestBody DepartmentCreateRequest request) {
        return ApiResult.success(departmentService.create(request), "新增成功");
    }

    @PutMapping("/{id}")
    public ApiResult<DepartmentResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody DepartmentUpdateRequest request) {
        return ApiResult.success(departmentService.update(id, request), "更新成功");
    }

    @PutMapping("/{id}/status")
    public ApiResult<Void> updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody DepartmentStatusRequest request) {
        departmentService.updateStatus(id, request.status());
        return ApiResult.success(null, request.status() == 1 ? "已启用" : "已停用");
    }

    @GetMapping("/active")
    public ApiResult<List<DepartmentResponse>> listActive() {
        return ApiResult.success(departmentService.listAllActive());
    }
}
