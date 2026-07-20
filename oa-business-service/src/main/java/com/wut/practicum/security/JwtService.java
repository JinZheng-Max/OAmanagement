package com.wut.practicum.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {
    private final SecretKey key;
    private final Duration ttl;
    public JwtService(@Value("${oa.security.jwt-secret}") String secret, @Value("${oa.security.access-token-ttl:30m}") Duration ttl) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
    }
    public IssuedToken issue(Long userId, String username, String role, Long employeeId) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder().id(jti).subject(username).claim("userId", userId).claim("role", role).claim("employeeId", employeeId)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(ttl))).signWith(key).compact();
        return new IssuedToken(token, jti, ttl);
    }
    public CurrentUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number userId = claims.get("userId", Number.class);
        Number employeeId = claims.get("employeeId", Number.class);
        return new CurrentUser(userId.longValue(), claims.getSubject(), claims.get("role", String.class), employeeId == null ? null : employeeId.longValue(), claims.getId());
    }
    public record IssuedToken(String token, String jti, Duration ttl) {}
}
