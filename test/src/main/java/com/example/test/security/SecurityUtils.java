package com.example.test.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 客户端信息工具，与 Express 侧 utils/security.js 保持一致
 */
public final class SecurityUtils {

    private static final Pattern SENSITIVE_KEY =
            Pattern.compile("password|passwordHash|password_hash|token|authorization|secret",
                    Pattern.CASE_INSENSITIVE);

    private SecurityUtils() {
    }

    /**
     * 获取客户端 IP：优先 X-Forwarded-For 首个，其次 remoteAddr
     */
    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            return first.trim();
        }
        return request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
    }

    /**
     * 解析 User-Agent 中的浏览器与操作系统
     */
    public static String[] parseUserAgent(String userAgent) {
        String ua = userAgent == null ? "" : userAgent;
        String browser = ua.contains("Edg/") ? "Edge"
                : ua.contains("Chrome/") ? "Chrome"
                : ua.contains("Firefox/") ? "Firefox"
                : ua.contains("Safari/") ? "Safari"
                : "Unknown";
        String os = ua.contains("Windows NT") ? "Windows"
                : ua.contains("Mac OS X") ? "macOS"
                : ua.contains("Linux") ? "Linux"
                : ua.contains("Android") ? "Android"
                : (ua.contains("iPhone") || ua.contains("iPad")) ? "iOS"
                : "Unknown";
        return new String[]{browser, os};
    }

    /**
     * 敏感字段脱敏（password/token/secret 等），递归处理嵌套结构，值为 "******"
     */
    public static Object maskSensitive(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(SecurityUtils::maskSensitive).toList();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                result.put(key, SENSITIVE_KEY.matcher(key).find() ? "******" : maskSensitive(entry.getValue()));
            }
            return result;
        }
        return value;
    }
}
