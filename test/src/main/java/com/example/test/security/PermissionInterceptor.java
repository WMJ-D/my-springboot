package com.example.test.security;

import com.example.test.common.AppException;
import com.example.test.mapper.AuthMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限拦截器，与 Express 侧 middleware/auth.js 的 authorize 行为一致：
 * admin 角色直通，否则按用户实际拥有的权限标识校验
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final AuthMapper authMapper;

    public PermissionInterceptor(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequirePermission annotation = method.getMethodAnnotation(RequirePermission.class);
        if (annotation == null || annotation.value().length == 0) {
            return true;
        }
        CurrentUser user = AuthContext.require();
        if (user.isAdmin()) {
            return true;
        }
        Set<String> granted = new HashSet<>(authMapper.findPermissions(user.userId(),
                SecurityUtils.getAppId(request)));
        if (Arrays.stream(annotation.value()).noneMatch(granted::contains)) {
            throw new AppException(403, "无权执行此操作", "FORBIDDEN");
        }
        return true;
    }
}
