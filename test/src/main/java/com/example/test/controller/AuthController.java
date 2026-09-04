package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.service.AuthService;
import com.example.test.security.AuthContext;
import com.example.test.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 认证接口，与 Express 侧 /api/v1/auth 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") @Size(max = 64) String username,
            @NotBlank(message = "密码不能为空") @Size(max = 128) String password) {
    }

    /**
     * POST /api/v1/auth/login 登录
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@jakarta.validation.Valid @RequestBody LoginRequest body,
                                                  HttpServletRequest request) {
        return ApiResponse.ok(authService.login(body.username().trim(), body.password(), request), "登录成功");
    }

    /**
     * GET /api/v1/auth/me 当前登录用户信息（permissions 按请求头 X-App-Id 过滤子系统）
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        return ApiResponse.ok(authService.loadIdentity(AuthContext.require().userId(),
                SecurityUtils.getAppId(request)));
    }

    /**
     * GET /api/v1/auth/menus 当前用户菜单树（按请求头 X-App-Id 过滤子系统）
     */
    @GetMapping("/menus")
    public ApiResponse<List<Map<String, Object>>> menus(HttpServletRequest request) {
        return ApiResponse.ok(authService.menus(AuthContext.require(), SecurityUtils.getAppId(request)));
    }

    /**
     * POST /api/v1/auth/heartbeat 会话心跳
     */
    @PostMapping("/heartbeat")
    public ApiResponse<Object> heartbeat() {
        authService.heartbeat(AuthContext.require());
        return ApiResponse.ok(null);
    }

    /**
     * POST /api/v1/auth/logout 退出登录
     */
    @PostMapping("/logout")
    public ApiResponse<Object> logout() {
        authService.logout(AuthContext.require());
        return ApiResponse.ok(null, "退出成功");
    }
}
