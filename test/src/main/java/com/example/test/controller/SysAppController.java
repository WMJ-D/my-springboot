package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.security.AuthContext;
import com.example.test.security.RequirePermission;
import com.example.test.service.SysAppService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

import java.util.Map;

/**
 * 子系统管理接口（角色管理页「子系统管理」tab 使用）
 */
@RestController
@RequestMapping("/api/v1/system/apps")
public class SysAppController {

    private final SysAppService appService;

    public SysAppController(SysAppService appService) {
        this.appService = appService;
    }

    public record AppBody(
            @NotBlank(message = "子系统标识不能为空") @Size(max = 64)
            @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "子系统标识只能包含字母、数字、下划线和横线") String appId,
            @NotBlank(message = "子系统名称不能为空") @Size(max = 100) String appName,
            @Size(max = 255) String baseUrl,
            @Size(max = 100) String icon,
            @Min(0) Integer sort,
            @Min(0) @Max(1) Integer status,
            @Size(max = 500) String remark) {

        public AppBody {
            appId = appId == null ? null : appId.trim();
            appName = appName == null ? null : appName.trim();
            baseUrl = trimToNull(baseUrl);
            icon = trimToNull(icon);
            remark = trimToNull(remark);
        }
    }

    /**
     * GET /api/v1/system/apps 分页查询子系统
     */
    @GetMapping
    @RequirePermission("system:app:list")
    public ApiResponse<PageResult<Map<String, Object>>> list(@RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize,
                                                             @RequestParam(required = false) String appId,
                                                             @RequestParam(required = false) String appName,
                                                             @RequestParam(required = false) String status) {
        PageQuery query = PageQuery.of(pageNum, pageSize);
        Integer statusValue = status == null || status.isBlank() ? null : ("1".equals(status) ? 1 : 0);
        return ApiResponse.ok(appService.list(query, appId, appName, statusValue));
    }

    /**
     * POST /api/v1/system/apps 新增子系统
     */
    @PostMapping
    @RequirePermission("system:app:add")
    public ApiResponse<Map<String, String>> create(@Valid @RequestBody AppBody body) {
        String id = appService.create(toInput(body), AuthContext.require().userId());
        return ApiResponse.ok(Map.of("id", id), "新增成功");
    }

    /**
     * PUT /api/v1/system/apps/{id} 修改子系统
     */
    @PutMapping("/{id}")
    @RequirePermission("system:app:edit")
    public ApiResponse<Object> update(@PathVariable long id, @Valid @RequestBody AppBody body) {
        appService.update(id, toInput(body), AuthContext.require().userId());
        return ApiResponse.ok(null, "修改成功");
    }

    /**
     * DELETE /api/v1/system/apps/{id} 删除子系统（支持逗号分隔批量）
     */
    @DeleteMapping("/{id}")
    @RequirePermission("system:app:delete")
    public ApiResponse<Object> delete(@PathVariable String id) {
        appService.delete(id, AuthContext.require().userId());
        return ApiResponse.ok(null, "删除成功");
    }

    private static SysAppService.AppInput toInput(AppBody body) {
        return new SysAppService.AppInput(body.appId(), body.appName(), body.baseUrl(), body.icon(),
                body.sort(), body.status(), body.remark());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
