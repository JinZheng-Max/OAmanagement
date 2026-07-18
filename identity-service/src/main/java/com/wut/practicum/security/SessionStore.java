package com.wut.practicum.security;

import java.time.Duration;

public interface SessionStore {
    void create(Long userId, String jti, Duration ttl);
    boolean isActive(String jti);
    void revoke(String jti);
    void revokeAll(Long userId);
}
