package com.example.test.controller;

import com.example.test.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查，与 Express 侧 GET /health 保持一致
 */
@RestController
public class HealthController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now().format(FORMATTER));
        return ApiResponse.ok(data);
    }
}
