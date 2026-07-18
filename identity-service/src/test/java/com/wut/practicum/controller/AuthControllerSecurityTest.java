package com.wut.practicum.controller;

import com.wut.practicum.common.GlobalExceptionHandler;
import com.wut.practicum.config.SecurityConfig;
import com.wut.practicum.filter.TraceIdFilter;
import com.wut.practicum.mapper.UserMapper;
import com.wut.practicum.security.CurrentUser;
import com.wut.practicum.security.JwtAuthenticationFilter;
import com.wut.practicum.security.JwtService;
import com.wut.practicum.security.SessionStore;
import com.wut.practicum.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, properties = {
        "spring.config.import=", "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false"})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, TraceIdFilter.class, GlobalExceptionHandler.class})
class AuthControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserService userService;
    @MockitoBean SessionStore sessions;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserMapper userMapper;

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401)).andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void loginValidatesPayloadAndReturnsRequestTrace() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void publicRegistrationEndpointDoesNotExist() throws Exception {
        authenticateAsAdmin();
        mvc.perform(post("/api/auth/register").header("Authorization", "Bearer valid-token")
                        .contentType("application/json").content("{\"username\":\"mallory\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void logoutRevokesCurrentJti() throws Exception {
        authenticateAsAdmin();
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("退出成功"));
        verify(sessions).revoke("session-1");
    }

    private void authenticateAsAdmin() {
        when(jwtService.parse(anyString())).thenReturn(new CurrentUser(1L, "admin", "ADMIN", null, "session-1"));
        when(sessions.isActive("session-1")).thenReturn(true);
    }
}
