package com.wut.practicum.security;

public record CurrentUser(Long userId, String username, String role, Long employeeId, String jti) {}
