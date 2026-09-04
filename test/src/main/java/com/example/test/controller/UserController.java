package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.security.AuthContext;
import com.example.test.security.RequirePermission;
import com.example.test.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户管理接口，与 Express 侧 /api/v1/system/users 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/system/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 入参：新增/修改用户（空字符串统一转为 null，与 Express 侧 optionalText 行为一致）
     */
    public record UserBody(
            @NotBlank(message = "用户名不能为空") @Size(min = 2, max = 64) String username,
            @NotBlank(message = "昵称不能为空") @Size(max = 64) String nickname,
            @Size(max = 32) String phone,
            @Email(message = "邮箱格式不正确") @Size(max = 128) String email,
            Long orgId,
            List<Long> roleIds,
            @Size(min = 6, max = 128) String password,
            @NotNull(message = "状态不能为空") @Min(0) @Max(1) Integer status) {

        public UserBody {
            phone = trimToNull(phone);
            email = trimToNull(email);
            roleIds = roleIds == null ? List.of() : roleIds;
        }
    }

    /**
     * GET /api/v1/system/users 分页查询用户
     */
    @GetMapping
    @RequirePermission("system:user:list")
    public ApiResponse<PageResult<Map<String, Object>>> list(@RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize,
                                                             @RequestParam(required = false) String username,
                                                             @RequestParam(required = false) String phone,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) Long orgId) {
        PageQuery query = PageQuery.of(pageNum, pageSize);
        return ApiResponse.ok(userService.list(query, username, phone, parseStatus(status), orgId));
    }

    /**
     * GET /api/v1/system/users/{id} 用户详情
     */
    @GetMapping("/{id}")
    @RequirePermission("system:user:list")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long id) {
        return ApiResponse.ok(userService.detail(id));
    }

    /**
     * POST /api/v1/system/users 新增用户
     */
    @PostMapping
    @RequirePermission("system:user:add")
    public ApiResponse<Map<String, String>> create(@Valid @RequestBody UserBody body) {
        long actor = AuthContext.require().userId();
        String id = userService.create(new UserService.UserInput(body.username().trim(), body.nickname().trim(),
                body.phone(), body.email(), body.orgId(), body.roleIds(), body.password(), body.status()), actor);
        return ApiResponse.ok(Map.of("id", id), "新增成功");
    }

    /**
     * PUT /api/v1/system/users/{id} 修改用户
     */
    @PutMapping("/{id}")
    @RequirePermission("system:user:edit")
    public ApiResponse<Object> update(@PathVariable long id, @Valid @RequestBody UserBody body) {
        long actor = AuthContext.require().userId();
        userService.update(id, new UserService.UserInput(body.username().trim(), body.nickname().trim(),
                body.phone(), body.email(), body.orgId(), body.roleIds(), null, body.status()), actor);
        return ApiResponse.ok(null, "修改成功");
    }

    /**
     * PATCH /api/v1/system/users/{id}/status 修改用户状态
     */
    @PatchMapping("/{id}/status")
    @RequirePermission("system:user:change-status")
    public ApiResponse<Object> updateStatus(@PathVariable long id, @RequestBody StatusBody body) {
        userService.updateStatus(id, body.status(), AuthContext.require().userId());
        return ApiResponse.ok(null);
    }

    /**
     * POST /api/v1/system/users/{id}/reset-password 重置密码
     */
    @PostMapping("/{id}/reset-password")
    @RequirePermission("system:user:reset-password")
    public ApiResponse<Object> resetPassword(@PathVariable long id, @RequestBody(required = false) PasswordBody body) {
        userService.resetPassword(id, body == null ? null : body.password(), AuthContext.require().userId());
        return ApiResponse.ok(null, "密码重置成功");
    }

    /**
     * DELETE /api/v1/system/users/{id} 删除用户（支持逗号分隔批量）
     */
    @DeleteMapping("/{id}")
    @RequirePermission("system:user:delete")
    public ApiResponse<Object> delete(@PathVariable String id) {
        long actor = AuthContext.require().userId();
        userService.delete(id, actor, actor);
        return ApiResponse.ok(null, "删除成功");
    }

    public record StatusBody(@NotNull(message = "状态不能为空") @Min(0) @Max(1) Integer status) {
    }

    public record PasswordBody(@Size(min = 6, max = 128) String password) {
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Integer parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return "1".equals(status) ? 1 : 0;
    }
}
