package com.studyroom.config;

import com.studyroom.common.ApiResponse;
import com.studyroom.common.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthTokenService tokenService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        boolean userRoute = path.startsWith("/api/preferences") || path.startsWith("/api/profile")
                || path.startsWith("/api/recommendations");
        boolean managementWrite = isWrite(request.getMethod())
                && (path.startsWith("/api/canteens") || path.startsWith("/api/shops")
                || path.startsWith("/api/dishes") || path.startsWith("/api/tags"));
        if (!userRoute && !managementWrite) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        AuthUser user = authorization != null && authorization.startsWith("Bearer ")
                ? tokenService.resolve(authorization.substring(7).trim()) : null;
        if (user == null) {
            writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "请先登录或登录已失效");
            return false;
        }
        if (managementWrite && !user.isAdmin()) {
            writeFailure(response, HttpServletResponse.SC_FORBIDDEN, 403, "仅管理员可维护数据");
            return false;
        }
        request.setAttribute(AuthUser.REQUEST_ATTRIBUTE, user);
        return true;
    }

    private void writeFailure(HttpServletResponse response, int status, int code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(code, message)));
    }

    private static boolean isWrite(String method) {
        return HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method) || HttpMethod.DELETE.matches(method);
    }
}
