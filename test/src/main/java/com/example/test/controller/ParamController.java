package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.security.AuthContext;
import com.example.test.security.RequirePermission;
import com.example.test.service.ParamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * 系统参数管理接口，与 Express 侧 /api/v1/system/params 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/system/params")
public class ParamController {

    private final ParamService paramService;

    public ParamController(ParamService paramService) {
        this.paramService = paramService;
    }

    public record ParamBody(
            @NotBlank(message = "参数名称不能为空") @Size(max = 200) String paramName,
            @NotBlank(message = "参数键名不能为空") @Size(max = 200) String paramKey,
            @NotNull(message = "参数键值不能为空") @Size(max = 65535) String paramValue,
            @Pattern(regexp = "[YN]", message = "参数类型必须是 Y/N") String paramType,
            @Pattern(regexp = "string|number|boolean|json", message = "值类型不合法") String valueType,
            @Size(max = 1000) String remark) {

        public ParamBody {
            paramName = paramName.trim();
            paramKey = paramKey.trim();
            remark = trimToNull(remark);
        }
    }

    /**
     * GET /api/v1/system/params 分页查询参数
     */
    @GetMapping
    @RequirePermission("system:param:list")
    public ApiResponse<PageResult<Map<String, Object>>> list(@RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize,
                                                             @RequestParam(required = false) String paramName,
                                                             @RequestParam(required = false) String paramKey,
                                                             @RequestParam(required = false) String paramType) {
        PageQuery query = PageQuery.of(pageNum, pageSize);
        return ApiResponse.ok(paramService.list(query, paramName, paramKey, paramType));
    }

    /**
     * POST /api/v1/system/params 新增参数
     */
    @PostMapping
    @RequirePermission("system:param:add")
    public ApiResponse<Map<String, String>> create(@Valid @RequestBody ParamBody body) {
        String id = paramService.create(toInput(body), AuthContext.require().userId());
        return ApiResponse.ok(Map.of("id", id), "新增成功");
    }

    /**
     * PUT /api/v1/system/params/{id} 修改参数
     */
    @PutMapping("/{id}")
    @RequirePermission("system:param:edit")
    public ApiResponse<Object> update(@PathVariable long id, @Valid @RequestBody ParamBody body) {
        paramService.update(id, toInput(body), AuthContext.require().userId());
        return ApiResponse.ok(null, "修改成功");
    }

    /**
     * DELETE /api/v1/system/params/{id} 删除参数（支持逗号分隔批量）
     */
    @DeleteMapping("/{id}")
    @RequirePermission("system:param:delete")
    public ApiResponse<Object> delete(@PathVariable String id) {
        paramService.delete(id, AuthContext.require().userId());
        return ApiResponse.ok(null, "删除成功");
    }

    private static ParamService.ParamInput toInput(ParamBody body) {
        return new ParamService.ParamInput(body.paramName(), body.paramKey(), body.paramValue(),
                body.paramType(), body.valueType(), body.remark());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
