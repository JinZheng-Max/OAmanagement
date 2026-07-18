package com.wut.practicum.dto;

public record LoginResponse(String token, String tokenType, long expiresIn, UserInfo userInfo) {
    public record UserInfo(Long id, String username, String role, Long employeeId) {}
}
