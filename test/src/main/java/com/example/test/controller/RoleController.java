package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.security.AuthContext;
import com.example.test.security.RequirePermission;
import com.example.test.service.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * 角色管理接口，与 Express 侧 /api/v1/system/roles 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/system/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    public record RoleBody(
            @NotBlank(message = "角色名称不能为空") @Size(max = 64) String roleName,
            @NotBlank(message = "角色标识不能为空") @Size(max = 64) String roleKey,
            @Min(1) @Max(5) Integer dataScope,
            @Min(0) Integer sort,
            @Min(0) @Max(1) Integer status,
            @Size(max = 500) String remark) {

        public RoleBody {
            roleName = trim(roleName);
            roleKey = trim(roleKey);
            remark = trimToNull(remark);
        }
    }

    /**
     * GET /api/v1/system/roles 分页查询角色
     */
    @GetMapping
    @RequirePermission("system:role:list")
    public ApiResponse<PageResult<Map<String, Object>>> list(@RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize,
                                                             @RequestParam(required = false) String roleName,
                                                             @RequestParam(required = false) String status) {
        PageQuery query = PageQuery.of(pageNum, pageSize);
        Integer statusValue = status == null || status.isBlank() ? null : ("1".equals(status) ? 1 : 0);
        return ApiResponse.ok(roleService.list(query, roleName, statusValue));
    }

    /**
     * POST /api/v1/system/roles 新增角色
     */
    @PostMapping
    @RequirePermission("system:role:add")
    public ApiResponse<Map<String, String>> create(@Valid @RequestBody RoleBody body) {
        String id = roleService.create(new RoleService.RoleInput(body.roleName(), body.roleKey(), body.dataScope(),
                body.sort(), body.status(), body.remark()), AuthContext.require().userId());
        return ApiResponse.ok(Map.of("id", id), "新增成功");
    }

    /**
     * PUT /api/v1/system/roles/{id} 修改角色
     */
    @PutMapping("/{id}")
    @RequirePermission("system:role:edit")
    public ApiResponse<Object> update(@PathVariable long id, @Valid @RequestBody RoleBody body) {
        roleService.update(id, new RoleService.RoleInput(body.roleName(), body.roleKey(), body.dataScope(),
                body.sort(), body.status(), body.remark()), AuthContext.require().userId());
        return ApiResponse.ok(null, "修改成功");
    }

    /**
     * DELETE /api/v1/system/roles/{id} 删除角色（支持逗号分隔批量）
     */
    @DeleteMapping("/{id}")
    @RequirePermission("system:role:delete")
    public ApiResponse<Object> delete(@PathVariable String id) {
        roleService.delete(id, AuthContext.require().userId());
        return ApiResponse.ok(null, "删除成功");
    }

    /**
     * GET /api/v1/system/roles/{id}/menu-ids 角色已分配菜单
     */
    @GetMapping("/{id}/menu-ids")
    @RequirePermission("system:role:list")
    public ApiResponse<List<Long>> menuIds(@PathVariable long id) {
        return ApiResponse.ok(roleService.menuIds(id));
    }

    /**
     * PUT /api/v1/system/roles/{id}/menus 保存角色权限
     */
    @PutMapping("/{id}/menus")
    @RequirePermission("system:role:permission")
    public ApiResponse<Object> assignMenus(@PathVariable long id, @RequestBody MenuIdsBody body) {
        roleService.assignMenus(id, body.menuIds());
        return ApiResponse.ok(null, "权限保存成功");
    }

    public record MenuIdsBody(List<Long> menuIds) {
        public MenuIdsBody {
            menuIds = menuIds == null ? List.of() : menuIds;
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
