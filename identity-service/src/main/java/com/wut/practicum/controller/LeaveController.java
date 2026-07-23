package com.wut.practicum.controller;

import com.wut.practicum.common.ApiResult;
import com.wut.practicum.dto.*;
import com.wut.practicum.security.CurrentUser;
import com.wut.practicum.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    public ApiResult<PageResult<LeaveResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal CurrentUser user) {
        PageQuery query = new PageQuery(page, size);
        return ApiResult.success(leaveService.page(query, user.employeeId(), user.role(), status, type));
    }

    @GetMapping("/{id}")
    public ApiResult<LeaveResponse> getById(@PathVariable Long id) {
        return ApiResult.success(leaveService.getById(id));
    }

    @PostMapping
    public ApiResult<LeaveResponse> create(@AuthenticationPrincipal CurrentUser user,
                                           @Valid @RequestBody LeaveCreateRequest request) {
        if (user.employeeId() == null) return ApiResult.error(400, "当前账号未关联员工信息");
        return ApiResult.success(leaveService.create(user.employeeId(), request), "提交成功");
    }

    @PostMapping("/{id}/withdraw")
    public ApiResult<Void> withdraw(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user) {
        leaveService.withdraw(id, user.employeeId());
        return ApiResult.success(null, "撤回成功");
    }

    @PostMapping("/{id}/audit")
    public ApiResult<Void> audit(@PathVariable Long id,
                                 @AuthenticationPrincipal CurrentUser user,
                                 @Valid @RequestBody LeaveAuditRequest request) {
        leaveService.audit(id, user.employeeId(), request);
        return ApiResult.success(null, "审批完成");
    }
}
