package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.security.AuthContext;
import com.example.test.security.RequirePermission;
import com.example.test.service.MenuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
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
 * 菜单管理接口，与 Express 侧 /api/v1/system/menus 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/system/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    public record MenuBody(
            Long parentId,
            @Size(max = 64) String appId,
            @NotBlank(message = "菜单名称不能为空") @Size(max = 100) String menuName,
            @NotEmpty(message = "菜单类型不能为空") @Pattern(regexp = "[MCF]", message = "菜单类型必须是 M/C/F") String menuType,
            @Size(max = 255) String path,
            @Size(max = 255) String component,
            @Size(max = 100) String routeName,
            @Size(max = 128) String permission,
            @Size(max = 100) String icon,
            @Min(0) Integer sort,
            @Min(0) @Max(1) Integer visible,
            @Min(0) @Max(1) Integer status,
            @Min(0) @Max(1) Integer keepAlive,
            @Min(0) @Max(1) Integer externalLink,
            @Size(max = 500) String remark) {

        public MenuBody {
            path = trimToNull(path);
            component = trimToNull(component);
            routeName = trimToNull(routeName);
            permission = trimToNull(permission);
            icon = trimToNull(icon);
            remark = trimToNull(remark);
        }
    }

    /**
     * GET /api/v1/system/menus 菜单树
     */
    @GetMapping
    @RequirePermission("system:menu:list")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String menuName,
                                                       @RequestParam(required = false) String status) {
        Integer statusValue = status == null || status.isBlank() ? null : ("1".equals(status) ? 1 : 0);
        return ApiResponse.ok(menuService.list(menuName, statusValue));
    }

    /**
     * POST /api/v1/system/menus 新增菜单
     */
    @PostMapping
    @RequirePermission("system:menu:add")
    public ApiResponse<Map<String, String>> create(@Valid @RequestBody MenuBody body) {
        String id = menuService.create(toInput(body), AuthContext.require().userId());
        return ApiResponse.ok(Map.of("id", id), "新增成功");
    }

    /**
     * PUT /api/v1/system/menus/{id} 修改菜单
     */
    @PutMapping("/{id}")
    @RequirePermission("system:menu:edit")
    public ApiResponse<Object> update(@PathVariable long id, @Valid @RequestBody MenuBody body) {
        menuService.update(id, toInput(body), AuthContext.require().userId());
        return ApiResponse.ok(null, "修改成功");
    }

    /**
     * DELETE /api/v1/system/menus/{id} 删除菜单
     */
    @DeleteMapping("/{id}")
    @RequirePermission("system:menu:delete")
    public ApiResponse<Object> delete(@PathVariable long id) {
        menuService.delete(id, AuthContext.require().userId());
        return ApiResponse.ok(null, "删除成功");
    }

    private static MenuService.MenuInput toInput(MenuBody body) {
        return new MenuService.MenuInput(body.parentId(), body.appId(), body.menuName().trim(), body.menuType(),
                body.path(), body.component(), body.routeName(), body.permission(), body.icon(), body.sort(),
                body.visible(), body.status(), body.keepAlive(), body.externalLink(), body.remark());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
