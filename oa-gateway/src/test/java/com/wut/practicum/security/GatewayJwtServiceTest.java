package com.wut.practicum.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class GatewayJwtServiceTest {
    private static final String SECRET = "test-only-secret-with-at-least-32-bytes";
    @Test
    void parsesIdentityServiceTokenShape() {
        Instant now = Instant.now();
        String token = Jwts.builder().id("session-1").subject("alice").claim("userId", 8L).claim("role", "EMPLOYEE")
                .claim("employeeId", 4L).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        var user = new GatewayJwtService(SECRET).parse(token);
        assertEquals(8L, user.userId()); assertEquals("session-1", user.jti()); assertEquals("EMPLOYEE", user.role());
    }
}
