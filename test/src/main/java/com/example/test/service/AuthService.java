package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.mapper.AuthMapper;
import com.example.test.mapper.SysLoginLogMapper;
import com.example.test.mapper.SysUserSessionMapper;
import com.example.test.security.CurrentUser;
import com.example.test.security.JwtTokenProvider;
import com.example.test.security.LoginRateLimiter;
import com.example.test.security.PasswordUtil;
import com.example.test.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 认证服务，对应 Express 侧 routes/auth.js
 */
@Service
public class AuthService {

    private final AuthMapper authMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final SysUserSessionMapper sessionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(AuthMapper authMapper, SysLoginLogMapper loginLogMapper, SysUserSessionMapper sessionMapper,
                       JwtTokenProvider jwtTokenProvider, LoginRateLimiter loginRateLimiter) {
        this.authMapper = authMapper;
        this.loginLogMapper = loginLogMapper;
        this.sessionMapper = sessionMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginRateLimiter = loginRateLimiter;
    }

    /**
     * 登录：校验密码、签发 JWT、写会话与登录日志
     */
    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        if (loginRateLimiter.isLimited(SecurityUtils.getClientIp(request))) {
            throw new AppException(429, "登录尝试过于频繁，请稍后再试", "TOO_MANY_ATTEMPTS");
        }
        String ip = SecurityUtils.getClientIp(request);
        String appId = SecurityUtils.getAppId(request);
        String userAgent = request.getHeader("User-Agent") == null ? "" : request.getHeader("User-Agent");
        String[] browserOs = SecurityUtils.parseUserAgent(userAgent);

        Map<String, Object> user = authMapper.findLoginUser(username);
        boolean valid = user != null
                && toLong(user.get("deleted")) == 0
                && toLong(user.get("status")) == 1
                && PasswordUtil.verify(password, String.valueOf(user.get("password_hash")));
        if (!valid) {
            Long userId = user == null ? null : toLong(user.get("id"));
            writeLoginLog(userId, username, 0, "用户名、密码错误或账号已禁用", ip, browserOs, userAgent, appId);
            throw new AppException(401, "用户名、密码错误或账号已禁用", "LOGIN_FAILED");
        }

        long userId = toLong(user.get("id"));
        Map<String, Object> identity = loadIdentity(userId, appId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> roles = (List<Map<String, Object>>) identity.get("roles");
        List<String> roleKeys = roles.stream().map(role -> String.valueOf(role.get("roleKey"))).toList();

        String sessionId = UUID.randomUUID().toString();
        String token = jwtTokenProvider.createToken(userId, username, roleKeys, sessionId);
        sessionMapper.insertSession(sessionId, appId, userId, username, ip, browserOs[0], browserOs[1], userAgent,
                LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpiresInSeconds()));
        authMapper.touchLastLogin(userId, ip);
        writeLoginLog(userId, username, 1, "登录成功", ip, browserOs, userAgent, appId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", formatExpiresIn(jwtTokenProvider.getExpiresInSeconds()));
        result.put("user", identity);
        return result;
    }

    /**
     * 加载用户身份：基础信息 + 角色 + 权限标识（permissions 按子系统过滤，appId 为 null 时不过滤）
     */
    public Map<String, Object> loadIdentity(long userId, String appId) {
        Map<String, Object> user = authMapper.findIdentityUser(userId);
        if (user == null || toLong(user.get("status")) != 1) {
            throw new AppException(401, "用户不存在或已禁用", "USER_DISABLED");
        }
        Map<String, Object> identity = new LinkedHashMap<>(user);
        identity.put("roles", authMapper.findRoles(userId));
        identity.put("permissions", authMapper.findPermissions(userId, appId));
        return identity;
    }

    /**
     * 当前用户菜单树（按请求头 X-App-Id 过滤子系统，NULL 菜单对所有系统可见）
     */
    public List<Map<String, Object>> menus(CurrentUser user, String appId) {
        List<Map<String, Object>> rows = user.isAdmin()
                ? authMapper.findAdminMenus(appId)
                : authMapper.findUserMenus(user.userId(), appId);
        return com.example.test.common.TreeBuilder.buildTree(rows, "parentId");
    }

    public void heartbeat(CurrentUser user) {
        sessionMapper.heartbeat(user.sessionId());
    }

    public void logout(CurrentUser user) {
        sessionMapper.logout(user.sessionId());
    }

    private void writeLoginLog(Long userId, String username, int status, String message,
                               String ip, String[] browserOs, String userAgent, String appId) {
        loginLogMapper.insertLog(userId, appId, username, ip, browserOs[0], browserOs[1], userAgent, status, message);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 与 Express 侧 JWT_EXPIRES_IN（如 2h）的输出格式保持一致
     */
    private String formatExpiresIn(long seconds) {
        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        return (seconds / 60) + "m";
    }
}
