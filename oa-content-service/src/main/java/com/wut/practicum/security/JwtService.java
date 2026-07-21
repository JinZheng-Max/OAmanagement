package com.wut.practicum.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${oa.security.jwt-secret:daimeiyong-daozhang-cancandao-zzdq-zqmeisuzhi-huaienfuzhe}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public UserPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        Long employeeId = claims.get("employeeId", Long.class);
        String jti = claims.getId();
        return new UserPrincipal(userId, username, role, employeeId, jti);
    }

    public record UserPrincipal(Long userId, String username, String role, Long employeeId, String jti) {}
}
