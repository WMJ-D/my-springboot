package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.mapper.SysAppMapper;
import com.example.test.mapper.SysParamMapper;
import com.example.test.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公共系统配置接口（免鉴权）：
 * 多子系统架构下，系统名称优先取请求头 X-App-Id 对应的子系统名称，
 * 未匹配到时回退参数表 sys.index.name
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private static final String SYSTEM_NAME_KEY = "sys.index.name";
    private static final String DEFAULT_SYSTEM_NAME = "后台管理系统";

    private final SysParamMapper paramMapper;
    private final SysAppMapper appMapper;

    public ConfigController(SysParamMapper paramMapper, SysAppMapper appMapper) {
        this.paramMapper = paramMapper;
        this.appMapper = appMapper;
    }

    /**
     * GET /api/v1/config/system-name 获取系统名称（按请求头 X-App-Id 对应子系统）
     */
    @GetMapping("/system-name")
    public ApiResponse<Map<String, String>> systemName(HttpServletRequest request) {
        String name = null;
        String appId = SecurityUtils.getAppId(request);
        if (appId != null) {
            Map<String, Object> app = appMapper.findByAppId(appId);
            if (app != null) {
                name = String.valueOf(app.get("appName"));
            }
        }
        if (name == null || name.isBlank()) {
            String value = paramMapper.findValueByKey(SYSTEM_NAME_KEY);
            name = value == null || value.isBlank() ? DEFAULT_SYSTEM_NAME : value;
        }
        return ApiResponse.ok(Map.of("name", name));
    }
}
