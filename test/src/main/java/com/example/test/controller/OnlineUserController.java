package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.security.AuthContext;
import com.example.test.security.RequirePermission;
import com.example.test.service.OnlineUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 在线用户接口，与 Express 侧 /api/v1/logs/online 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/logs/online")
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    public OnlineUserController(OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }

    /**
     * GET /api/v1/logs/online 分页查询在线会话
     */
    @GetMapping
    @RequirePermission("log:online:list")
    public ApiResponse<PageResult<Map<String, Object>>> list(@RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize,
                                                             @RequestParam(required = false) String username,
                                                             @RequestParam(required = false) String ip,
                                                             @RequestParam(required = false) String ipAddress) {
        return ApiResponse.ok(onlineUserService.list(PageQuery.of(pageNum, pageSize), username,
                ip != null && !ip.isBlank() ? ip : ipAddress, AuthContext.require()));
    }

    /**
     * DELETE /api/v1/logs/online/expired 清理过期会话
     */
    @DeleteMapping("/expired")
    @RequirePermission("log:online:clean")
    public ApiResponse<Map<String, Object>> clean() {
        return ApiResponse.ok(onlineUserService.clean(), "清理完成");
    }

    /**
     * DELETE /api/v1/logs/online/{sessionId} 强制下线
     */
    @DeleteMapping("/{sessionId}")
    @RequirePermission("log:online:kick")
    public ApiResponse<Object> kick(@PathVariable String sessionId, @RequestBody(required = false) KickBody body) {
        onlineUserService.kick(sessionId, body == null ? null : body.reason(), AuthContext.require());
        return ApiResponse.ok(null, "强制下线成功");
    }

    /**
     * DELETE /api/v1/logs/online 批量强制下线
     */
    @DeleteMapping
    @RequirePermission("log:online:kick")
    public ApiResponse<Map<String, Object>> batchKick(@Valid @RequestBody BatchKickBody body) {
        return ApiResponse.ok(onlineUserService.batchKick(body.sessionIds(), body.reason(), AuthContext.require()),
                "批量强制下线成功");
    }

    public record KickBody(@Size(max = 500) String reason) {
    }

    public record BatchKickBody(@NotEmpty(message = "请选择需要下线的会话") List<String> sessionIds,
                                @Size(max = 500) String reason) {
    }
}
