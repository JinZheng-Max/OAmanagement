package com.wut.practicum.service;

import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.LoginRequest;
import com.wut.practicum.dto.PasswordRequest;
import com.wut.practicum.entity.SysUser;
import com.wut.practicum.mapper.UserMapper;
import com.wut.practicum.security.JwtService;
import com.wut.practicum.security.SessionStore;
import com.wut.practicum.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {
    private UserMapper mapper;
    private SessionStore sessions;
    private PasswordEncoder encoder;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(UserMapper.class);
        sessions = mock(SessionStore.class);
        encoder = new BCryptPasswordEncoder();
        service = new UserServiceImpl(mapper, encoder, new JwtService("test-only-secret-with-at-least-32-bytes", Duration.ofMinutes(30)), sessions);
    }

    @Test
    void loginCreatesRevocableSession() {
        SysUser user = user(1, "admin", encoder.encode("StrongPass1"), "ADMIN");
        when(mapper.selectByUsername("admin")).thenReturn(user);
        var response = service.login(new LoginRequest("admin", "StrongPass1"));
        assertEquals("Bearer", response.tokenType());
        assertEquals("ADMIN", response.userInfo().role());
        verify(sessions).create(eq(1L), anyString(), eq(Duration.ofMinutes(30)));
    }

    @Test
    void rejectsIncorrectPasswordAndDisabledAccount() {
        SysUser user = user(1, "admin", encoder.encode("StrongPass1"), "ADMIN");
        when(mapper.selectByUsername("admin")).thenReturn(user);
        assertThrows(BusinessException.class, () -> service.login(new LoginRequest("admin", "WrongPass1")));
        user.setStatus(0);
        assertThrows(BusinessException.class, () -> service.login(new LoginRequest("admin", "StrongPass1")));
        verifyNoInteractions(sessions);
    }

    @Test
    void passwordChangeRevokesEverySession() {
        SysUser user = user(9, "alice", encoder.encode("OldPass12"), "EMPLOYEE");
        when(mapper.selectUserWithEmployeeById(9L)).thenReturn(user);
        service.updatePassword(9L, new PasswordRequest("OldPass12", "NewPass34"));
        verify(mapper).updatePassword(eq(9L), argThat(hash -> encoder.matches("NewPass34", hash)));
        verify(sessions).revokeAll(9L);
    }

    private SysUser user(long id, String username, String hash, String role) {
        SysUser user = new SysUser(); user.setId(id); user.setUsername(username); user.setPasswordHash(hash); user.setRole(role); user.setStatus(1); return user;
    }
}
