package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.mapper.DashboardMapper;
import com.example.test.security.AuthContext;
import com.example.test.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 仪表盘接口，与 Express 侧 /api/v1/dashboard 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardMapper dashboardMapper;

    public DashboardController(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    /**
     * GET /api/v1/dashboard/statistics 统计信息（登录即可访问，无需额外权限）
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        AuthContext.require();
        Map<String, Object> row = dashboardMapper.statistics();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCount", count(row, "user_count"));
        result.put("roleCount", count(row, "role_count"));
        result.put("menuCount", count(row, "menu_count"));
        result.put("todayVisitCount", count(row, "today_visit_count"));
        return ApiResponse.ok(result);
    }

    private static long count(Map<String, Object> row, String key) {
        Object value = row == null ? null : row.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }
}
