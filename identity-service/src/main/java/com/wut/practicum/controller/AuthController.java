package com.wut.practicum.controller;

import com.wut.practicum.common.ApiResult;
import com.wut.practicum.dto.LoginRequest;
import com.wut.practicum.dto.LoginResponse;
import com.wut.practicum.dto.PasswordRequest;
import com.wut.practicum.dto.UserProfileResponse;
import com.wut.practicum.security.CurrentUser;
import com.wut.practicum.security.SessionStore;
import com.wut.practicum.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final SessionStore sessionStore;
    public AuthController(UserService userService, SessionStore sessionStore) { this.userService = userService; this.sessionStore = sessionStore; }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @SecurityRequirements
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success(userService.login(request), "登录成功");
    }

    @PostMapping("/logout")
    @Operation(summary = "退出并立即撤销当前会话")
    public ApiResult<Void> logout(@AuthenticationPrincipal CurrentUser user) {
        sessionStore.revoke(user.jti());
        return ApiResult.success(null, "退出成功");
    }

    @GetMapping("/me")
    public ApiResult<UserProfileResponse> me(@AuthenticationPrincipal CurrentUser user) {
        return ApiResult.success(userService.getCurrentUserInfo(user.userId()));
    }

    @PutMapping("/password")
    public ApiResult<Void> password(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody PasswordRequest request) {
        userService.updatePassword(user.userId(), request);
        return ApiResult.success(null, "密码修改成功，请重新登录");
    }
}
