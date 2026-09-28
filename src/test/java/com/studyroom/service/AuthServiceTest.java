package com.studyroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.BizException;
import com.studyroom.config.AuthTokenService;
import com.studyroom.dto.LoginRequest;
import com.studyroom.dto.RegisterRequest;
import com.studyroom.mapper.AppUserMapper;
import com.studyroom.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private AppUserMapper userMapper;
    @Mock private AuthTokenService tokenService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userMapper, tokenService);
    }

    @Test
    void registersNormalUsersWithHashedPasswordAndDefaultNickname() {
        when(tokenService.issue(any())).thenReturn("session-token");
        when(userMapper.selectByUsername("student01")).thenReturn(null);
        when(userMapper.insert(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser created = invocation.getArgument(0);
            created.setId(7L);
            return 1;
        });

        var result = service.register(new RegisterRequest("student01", "password123", " "));

        assertThat(result.token()).isEqualTo("session-token");
        assertThat(result.nickname()).isEqualTo("student01");
        assertThat(result.role()).isEqualTo(1);
        verify(userMapper).insert(org.mockito.ArgumentMatchers.argThat(user ->
                !user.getPassword().equals("password123")
                        && new BCryptPasswordEncoder().matches("password123", user.getPassword())));
    }

    @Test
    void rejectsDuplicateAccountsAndPasswordsLongerThanBcryptLimit() {
        when(userMapper.selectByUsername("student01")).thenReturn(new AppUser());

        assertThatThrownBy(() -> service.register(new RegisterRequest("student01", "password123", null)))
                .isInstanceOf(BizException.class).hasMessageContaining("已被使用");
        assertThatThrownBy(() -> service.register(new RegisterRequest("student02", "a".repeat(73), null)))
                .isInstanceOf(BizException.class).hasMessageContaining("72 字节");
        verify(userMapper, never()).insert(any(AppUser.class));
    }

    @Test
    void logsInActiveUsersAndRejectsInvalidCredentialsOrDisabledUsers() {
        when(tokenService.issue(any())).thenReturn("session-token");
        AppUser user = user("student01", 1, "password123");
        when(userMapper.selectByUsername("student01")).thenReturn(user);

        assertThat(service.login(new LoginRequest("student01", "password123")).token())
                .isEqualTo("session-token");
        verify(userMapper).touchLastLogin(7L);
        assertThatThrownBy(() -> service.login(new LoginRequest("student01", "incorrect123")))
                .isInstanceOf(BizException.class);

        user.setStatus(0);
        assertThatThrownBy(() -> service.login(new LoginRequest("student01", "password123")))
                .isInstanceOf(BizException.class);
    }

    @Test
    void logoutRevokesOnlyNonBlankTokens() {
        service.logout("session-token");
        service.logout(" ");
        service.logout(null);

        verify(tokenService).revoke("session-token");
    }

    private static AppUser user(String username, int status, String password) {
        AppUser user = new AppUser();
        user.setId(7L);
        user.setUsername(username);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setNickname("学生");
        user.setRole(1);
        user.setStatus(status);
        return user;
    }
}
