package com.wut.practicum.config;

import com.wut.practicum.entity.SysUser;
import com.wut.practicum.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrap(UserMapper userMapper, PasswordEncoder passwordEncoder,
                          @Value("${oa.bootstrap.admin.username:}") String username,
                          @Value("${oa.bootstrap.admin.password:}") String password) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) return;
        if (username.isBlank() || password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalStateException("Bootstrap admin requires a username and a password of at least 8 characters containing letters and digits");
        }
        if (userMapper.selectByUsername(username) == null) {
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole("SUPER_ADMIN");
            user.setStatus(1);
            userMapper.insertUser(user);
        }
    }
}
