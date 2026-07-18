package com.wut.practicum.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "test-only-secret-with-at-least-32-bytes";

    @Test
    void issuesAndParsesRequiredClaims() {
        JwtService service = new JwtService(SECRET, Duration.ofMinutes(30));
        JwtService.IssuedToken issued = service.issue(7L, "alice", "EMPLOYEE", 12L);
        CurrentUser user = service.parse(issued.token());
        assertEquals(7L, user.userId());
        assertEquals("alice", user.username());
        assertEquals("EMPLOYEE", user.role());
        assertEquals(12L, user.employeeId());
        assertEquals(issued.jti(), user.jti());
    }

    @Test
    void rejectsTamperedTokenAndWeakSecret() {
        JwtService service = new JwtService(SECRET, Duration.ofMinutes(30));
        String token = service.issue(1L, "admin", "ADMIN", null).token();
        assertThrows(JwtException.class, () -> service.parse(token.substring(0, token.length() - 2) + "aa"));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", Duration.ofMinutes(30)));
    }
}
