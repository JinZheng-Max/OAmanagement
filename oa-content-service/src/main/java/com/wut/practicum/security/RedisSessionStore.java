package com.wut.practicum.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
public class RedisSessionStore implements SessionStore {

    private static final String SESSION_PREFIX = "oa:auth:session:";
    private static final String USER_SESSIONS_PREFIX = "oa:auth:user:";

    private final StringRedisTemplate redis;

    public RedisSessionStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void create(Long userId, String jti, Duration ttl) {
        redis.opsForValue().set(SESSION_PREFIX + jti, userId.toString(), ttl);
        String userKey = USER_SESSIONS_PREFIX + userId;
        redis.opsForSet().add(userKey, jti);
        redis.expire(userKey, ttl);
    }

    @Override
    public boolean isActive(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(SESSION_PREFIX + jti));
    }

    @Override
    public void revoke(String jti) {
        redis.delete(SESSION_PREFIX + jti);
    }

    @Override
    public void revokeAll(Long userId) {
        String userKey = USER_SESSIONS_PREFIX + userId;
        Set<String> jtis = redis.opsForSet().members(userKey);
        if (jtis != null) {
            for (String jti : jtis) {
                redis.delete(SESSION_PREFIX + jti);
            }
        }
        redis.delete(userKey);
    }
}
