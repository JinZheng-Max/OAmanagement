package com.wut.practicum.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class GatewayJwtService {
    private final SecretKey key;
    public GatewayJwtService(@Value("${oa.security.jwt-secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public GatewayPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number userId = claims.get("userId", Number.class);
        Number employeeId = claims.get("employeeId", Number.class);
        return new GatewayPrincipal(userId.longValue(), claims.getSubject(), claims.get("role", String.class),
                employeeId == null ? null : employeeId.longValue(), claims.getId());
    }
    public record GatewayPrincipal(Long userId, String username, String role, Long employeeId, String jti) {}
}
