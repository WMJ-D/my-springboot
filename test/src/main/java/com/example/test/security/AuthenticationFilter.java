package com.example.test.security;

import com.example.test.common.AppException;
import com.example.test.mapper.SysUserSessionMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 认证过滤器，与 Express 侧 middleware/auth.js 的 authenticate 行为一致：
 * 校验 Bearer JWT + 会话表状态（ACTIVE / KICKED / LOGOUT / EXPIRED）
 */
@Component
@Order(100)
public class AuthenticationFilter extends OncePerRequestFilter {

    /** 需要登录才能访问的前缀与精确路径（/api/v1/auth/login 除外） */
    private static final Set<String> PROTECTED_PREFIXES = Set.of(
            "/api/v1/system/", "/api/v1/logs/", "/api/v1/ai/", "/api/v1/dashboard/");
    private static final Set<String> PROTECTED_EXACT = Set.of(
            "/api/v1/auth/me", "/api/v1/auth/menus", "/api/v1/auth/heartbeat", "/api/v1/auth/logout");

    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserSessionMapper sessionMapper;

    public AuthenticationFilter(JwtTokenProvider jwtTokenProvider, SysUserSessionMapper sessionMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionMapper = sessionMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(PROTECTED_PREFIXES.stream().anyMatch(path::startsWith) || PROTECTED_EXACT.contains(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            CurrentUser user = authenticate(request);
            AuthContext.set(user);
            chain.doFilter(request, response);
        } catch (AppException error) {
            writeError(response, error.getStatus(), error.getCode(), error.getMessage());
        } finally {
            AuthContext.clear();
        }
    }

    private CurrentUser authenticate(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AppException(401, "请先登录", "UNAUTHORIZED");
        }
        CurrentUser user = jwtTokenProvider.parse(authorization.substring(7));

        Map<String, Object> session = sessionMapper.findStatusAndExpires(user.sessionId(), user.userId());
        if (session == null) {
            throw new AppException(401, "登录会话不存在，请重新登录", "INVALID_SESSION");
        }
        String status = String.valueOf(session.get("status"));
        if ("KICKED".equals(status)) {
            throw new AppException(401, "当前账号已被管理员强制下线", "SESSION_KICKED");
        }
        if (!"ACTIVE".equals(status)) {
            throw new AppException(401, "登录会话已失效，请重新登录", "SESSION_INACTIVE");
        }
        Object expiresAt = session.get("expires_at");
        if (expiresAt instanceof LocalDateTime time && !time.isAfter(LocalDateTime.now())) {
            sessionMapper.markExpired(user.sessionId());
            throw new AppException(401, "登录会话已过期，请重新登录", "SESSION_EXPIRED");
        }
        return user;
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", code);
        payload.put("message", message);
        response.getWriter().write(com.example.test.common.JacksonHolder.toJson(payload));
    }
}
