
package com.wut.practicum.service.impl;

import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.LoginRequest;
import com.wut.practicum.dto.LoginResponse;
import com.wut.practicum.dto.PasswordRequest;
import com.wut.practicum.dto.UserProfileResponse;
import com.wut.practicum.entity.SysUser;
import com.wut.practicum.mapper.UserMapper;
import com.wut.practicum.security.JwtService;
import com.wut.practicum.security.SessionStore;
import com.wut.practicum.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionStore sessionStore;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService, SessionStore sessionStore) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionStore = sessionStore;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt username={}", request.username());
        SysUser user = userMapper.selectByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login rejected username={} reason=bad_credentials", request.username());
            throw new BusinessException(1001, HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            log.warn("Login rejected username={} reason=disabled", request.username());
            throw new BusinessException(1002, HttpStatus.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        if (!"SUPER_ADMIN".equals(user.getRole()) && !"DEPT_MANAGER".equals(user.getRole()) && !"EMPLOYEE".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            log.warn("Login rejected username={} reason=invalid_role role={}", request.username(), user.getRole());
            throw new BusinessException(1003, HttpStatus.FORBIDDEN, "账号角色配置错误");
        }
        JwtService.IssuedToken issued = jwtService.issue(user.getId(), user.getUsername(), user.getRole(), user.getEmployeeId());
        sessionStore.create(user.getId(), issued.jti(), issued.ttl());
        log.info("Login succeeded username={} userId={} role={}", user.getUsername(), user.getId(), user.getRole());
        return new LoginResponse(issued.token(), "Bearer", issued.ttl().toSeconds(),
                new LoginResponse.UserInfo(user.getId(), user.getUsername(), user.getRole(), user.getEmployeeId()));
    }

    @Override
    public UserProfileResponse getCurrentUserInfo(Long id) {
        SysUser user = userMapper.selectUserWithEmployeeById(id);
        if (user == null) throw new BusinessException(1004, HttpStatus.NOT_FOUND, "用户不存在");
        return UserProfileResponse.from(user.getId(), user.getUsername(), user.getRole(), user.getStatus(), user.getEmployeeId(), user.getEmployee());
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, PasswordRequest request) {
        SysUser user = userMapper.selectUserWithEmployeeById(userId);
        if (user == null) throw new BusinessException(1004, HttpStatus.NOT_FOUND, "用户不存在");
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(1005, HttpStatus.BAD_REQUEST, "原密码错误");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(1006, HttpStatus.BAD_REQUEST, "新密码不能与原密码相同");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(request.newPassword()));
        sessionStore.revokeAll(userId);
    }
}