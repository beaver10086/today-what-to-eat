package com.studyroom.service;

import com.studyroom.common.AuthUser;
import com.studyroom.common.BizException;
import com.studyroom.config.AuthTokenService;
import com.studyroom.dto.AuthView;
import com.studyroom.dto.LoginRequest;
import com.studyroom.dto.RegisterRequest;
import com.studyroom.mapper.AppUserMapper;
import com.studyroom.model.AppUser;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserMapper userMapper;
    private final AuthTokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AppUserMapper userMapper, AuthTokenService tokenService) {
        this.userMapper = userMapper;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthView register(RegisterRequest request) {
        validatePasswordBytes(request.password());
        if (userMapper.selectByUsername(request.username()) != null) {
            throw new BizException(409, "用户名已被使用");
        }
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() == null || request.nickname().isBlank()
                ? request.username() : request.nickname().trim());
        user.setRole(1);
        user.setStatus(1);
        userMapper.insert(user);
        return issue(user);
    }

    @Transactional
    public AuthView login(LoginRequest request) {
        validatePasswordBytes(request.password());
        AppUser user = userMapper.selectByUsername(request.username());
        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        userMapper.touchLastLogin(user.getId());
        return issue(user);
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenService.revoke(token);
        }
    }

    private AuthView issue(AppUser user) {
        AuthUser authUser = new AuthUser(user.getId(), user.getUsername(), user.getRole(), user.getNickname());
        return new AuthView(tokenService.issue(authUser), user.getId(), user.getUsername(),
                user.getNickname(), user.getRole());
    }

    private static void validatePasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BizException(400, "密码 UTF-8 长度不能超过 72 字节");
        }
    }
}
