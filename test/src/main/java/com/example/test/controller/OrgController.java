package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.security.AuthContext;
import com.example.test.security.RequirePermission;
import com.example.test.service.OrgService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
 * 组织管理接口，与 Express 侧 /api/v1/system/orgs 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/system/orgs")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    public record OrgBody(
            Long parentId,
            @NotBlank(message = "组织名称不能为空") @Size(max = 100) String orgName,
            @Size(max = 64) String orgCode,
            @Size(max = 64) String leader,
            @Size(max = 32) String phone,
            @Email(message = "邮箱格式不正确") @Size(max = 128) String email,
            @Min(0) Integer sort,
            @Min(0) @Max(1) Integer status) {

        public OrgBody {
            orgCode = trimToNull(orgCode);
            leader = trimToNull(leader);
            phone = trimToNull(phone);
            email = trimToNull(email);
        }
    }

    /**
     * GET /api/v1/system/orgs 组织树
     */
    @GetMapping
    @RequirePermission("system:org:list")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String orgName) {
        return ApiResponse.ok(orgService.list(orgName));
    }

    /**
     * POST /api/v1/system/orgs 新增组织
     */
    @PostMapping
    @RequirePermission("system:org:add")
    public ApiResponse<Map<String, String>> create(@Valid @RequestBody OrgBody body) {
        String id = orgService.create(toInput(body), AuthContext.require().userId());
        return ApiResponse.ok(Map.of("id", id), "新增成功");
    }

    /**
     * PUT /api/v1/system/orgs/{id} 修改组织
     */
    @PutMapping("/{id}")
    @RequirePermission("system:org:edit")
    public ApiResponse<Object> update(@PathVariable long id, @Valid @RequestBody OrgBody body) {
        orgService.update(id, toInput(body), AuthContext.require().userId());
        return ApiResponse.ok(null, "修改成功");
    }

    /**
     * DELETE /api/v1/system/orgs/{id} 删除组织
     */
    @DeleteMapping("/{id}")
    @RequirePermission("system:org:delete")
    public ApiResponse<Object> delete(@PathVariable long id) {
        orgService.delete(id, AuthContext.require().userId());
        return ApiResponse.ok(null, "删除成功");
    }

    private static OrgService.OrgInput toInput(OrgBody body) {
        return new OrgService.OrgInput(body.parentId(), body.orgName().trim(), body.orgCode(), body.leader(),
                body.phone(), body.email(), body.sort(), body.status());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
