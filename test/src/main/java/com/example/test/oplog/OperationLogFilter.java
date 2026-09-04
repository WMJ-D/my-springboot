package com.example.test.oplog;

import com.example.test.common.JacksonHolder;
import com.example.test.mapper.SysOperationLogMapper;
import com.example.test.security.AuthContext;
import com.example.test.security.CurrentUser;
import com.example.test.security.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 操作日志过滤器，与 Express 侧 middleware/operation-log.js 行为一致：
 * 记录 /api/v1/** 下已登录用户的 POST/PUT/PATCH/DELETE 请求，请求参数与响应结果脱敏后落库。
 * <p>注意：AI 流式接口（ndjson）不包装响应（Express 侧仅拦截 res.json，流式响应不捕获），
 * 否则 ContentCachingResponseWrapper 会缓冲整个流，破坏流式输出。</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class OperationLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OperationLogFilter.class);

    private static final String AI_STREAM_PATH = "/api/v1/ai/chat/stream";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Map<String, String> MODULE_MAP = Map.of(
            "users", "用户管理", "roles", "角色管理", "menus", "菜单管理",
            "orgs", "组织管理", "params", "参数管理", "logs", "日志管理", "online", "在线用户");
    private static final Map<String, String[]> META_MAP = Map.of(
            "POST", new String[]{"CREATE", "新增"},
            "PUT", new String[]{"UPDATE", "修改"},
            "PATCH", new String[]{"UPDATE", "修改"},
            "DELETE", new String[]{"DELETE", "删除"});

    private final SysOperationLogMapper operationLogMapper;

    public OperationLogFilter(SysOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return !WRITE_METHODS.contains(method) || !path.startsWith("/api/v1/") || path.contains("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.currentTimeMillis();
        // 仅 JSON 请求需要捕获请求体（multipart 二进制不缓存，避免额外内存占用）
        String contentType = request.getContentType();
        boolean jsonBody = contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE);
        boolean wrapResponse = !AI_STREAM_PATH.equals(request.getRequestURI());

        HttpServletRequest wrappedRequest = jsonBody ? new ContentCachingRequestWrapper(request) : request;
        ContentCachingResponseWrapper wrappedResponse = wrapResponse ? new ContentCachingResponseWrapper(response) : null;
        Map<String, Object> queryParams = parseQueryString(request.getQueryString());
        String traceId = request.getHeader("x-trace-id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        if (wrappedResponse != null) {
            wrappedResponse.setHeader("x-trace-id", traceId);
        } else {
            response.setHeader("x-trace-id", traceId);
        }

        try {
            chain.doFilter(wrappedRequest, wrappedResponse == null ? response : wrappedResponse);
        } finally {
            try {
                writeLog(wrappedRequest, wrappedResponse == null ? response : wrappedResponse,
                        queryParams, traceId, System.currentTimeMillis() - started);
            } catch (Exception error) {
                log.error("操作日志写入失败", error);
            }
            if (wrappedResponse != null) {
                wrappedResponse.copyBodyToResponse();
            }
        }
    }

    private void writeLog(HttpServletRequest request, HttpServletResponse response,
                          Map<String, Object> queryParams, String traceId, long duration) {
        CurrentUser user = AuthContext.get();
        if (user == null) {
            return;
        }
        String method = request.getMethod();
        String[] meta = META_MAP.get(method);
        if (meta == null) {
            return;
        }
        String module = inferModule(request.getRequestURI());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("query", queryParams);
        Object body = readJsonBody(request);
        if (body != null) {
            params.put("body", body);
        }

        Map<String, Object> responseResult = new LinkedHashMap<>();
        responseResult.put("statusCode", response.getStatus());
        // 仅捕获 JSON 响应体（对应 Express 侧只拦截 res.json 的行为）
        String responseContentType = response.getContentType();
        if (responseContentType != null && responseContentType.contains("json")
                && response instanceof ContentCachingResponseWrapper wrapper) {
            Object responseBody = parseJson(new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8));
            if (responseBody != null) {
                responseResult.put("body", responseBody);
            }
        }
        int status = response.getStatus() < 400 ? 1 : 0;

        operationLogMapper.insertLog(
                traceId, module, meta[0], meta[1] + module, user.userId(), user.username(),
                method, requestUrl(request), JacksonHolder.toJson(SecurityUtils.maskSensitive(params)),
                JacksonHolder.toJson(SecurityUtils.maskSensitive(responseResult)),
                SecurityUtils.getClientIp(request),
                request.getHeader("User-Agent") == null ? "" : request.getHeader("User-Agent"),
                status, status == 1 ? null : "HTTP " + response.getStatus(), duration);
    }

    /**
     * 模块推断：online 优先，其次取路径中第一个命中的模块段（users/roles/menus/...）
     */
    private String inferModule(String path) {
        List<String> parts = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (!segment.isEmpty() && !"api".equals(segment) && !"v1".equals(segment)) {
                parts.add(segment);
            }
        }
        if (parts.contains("online")) {
            return MODULE_MAP.get("online");
        }
        for (String part : parts) {
            String mapped = MODULE_MAP.get(part);
            if (mapped != null) {
                return mapped;
            }
        }
        return parts.isEmpty() ? "系统" : parts.get(0);
    }

    private String requestUrl(HttpServletRequest request) {
        String query = request.getQueryString();
        return request.getRequestURI() + (query == null ? "" : "?" + query);
    }

    /**
     * 手动解析查询串（避免 multipart 场景下 getParameterMap 触发已消费流的解析）
     */
    private Map<String, Object> parseQueryString(String queryString) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (queryString == null || queryString.isBlank()) {
            return result;
        }
        for (String pair : queryString.split("&")) {
            int eq = pair.indexOf('=');
            String key = decode(eq > 0 ? pair.substring(0, eq) : pair);
            String value = eq > 0 ? decode(pair.substring(eq + 1)) : "";
            if (key.isEmpty()) {
                continue;
            }
            if (result.containsKey(key)) {
                Object existing = result.get(key);
                if (existing instanceof List<?> list) {
                    List<Object> mutable = new ArrayList<>(list);
                    mutable.add(value);
                    result.put(key, mutable);
                } else {
                    List<Object> values = new ArrayList<>();
                    values.add(existing);
                    values.add(value);
                    result.put(key, values);
                }
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private Object readJsonBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return null;
        }
        return parseJson(new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private Object parseJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return JacksonHolder.mapper().readValue(text, Object.class);
        } catch (Exception error) {
            return null;
        }
    }
}
