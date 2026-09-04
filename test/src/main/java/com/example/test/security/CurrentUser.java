package com.example.test.security;

import java.util.List;

/**
 * 当前登录用户信息，对应 Express 侧 req.user（JWT payload）
 */
public record CurrentUser(long userId, String username, List<String> roles, String sessionId) {

    public boolean hasRole(String roleKey) {
        return roles.contains(roleKey);
    }

    public boolean isAdmin() {
        return hasRole("admin");
    }
}
