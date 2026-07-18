package com.wut.practicum.service;

import com.wut.practicum.dto.LoginRequest;
import com.wut.practicum.dto.LoginResponse;
import com.wut.practicum.dto.PasswordRequest;
import com.wut.practicum.dto.UserProfileResponse;

public interface UserService {
    LoginResponse login(LoginRequest request);
    UserProfileResponse getCurrentUserInfo(Long id);
    void updatePassword(Long userId, PasswordRequest request);
}
