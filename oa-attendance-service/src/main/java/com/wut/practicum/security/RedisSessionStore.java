package com.wut.practicum.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
public class RedisSessionStore implements SessionStore {
    private static final String SESSION_PREFIX = "oa:auth:session:";
    private static final String USER_PREFIX = "oa:auth:user:";
    private final StringRedisTemplate redis;
    public RedisSessionStore(StringRedisTemplate redis) { this.redis = redis; }
    public void create(Long userId, String jti, Duration ttl) {
        redis.opsForValue().set(SESSION_PREFIX + jti, userId.toString(), ttl);
        String userKey = USER_PREFIX + userId;
        redis.opsForSet().add(userKey, jti);
        redis.expire(userKey, ttl);
    }
    public boolean isActive(String jti) { return Boolean.TRUE.equals(redis.hasKey(SESSION_PREFIX + jti)); }
    public void revoke(String jti) { redis.delete(SESSION_PREFIX + jti); }
    public void revokeAll(Long userId) {
        String userKey = USER_PREFIX + userId;
        Set<String> ids = redis.opsForSet().members(userKey);
        if (ids != null && !ids.isEmpty()) redis.delete(ids.stream().map(id -> SESSION_PREFIX + id).toList());
        redis.delete(userKey);
    }
}
