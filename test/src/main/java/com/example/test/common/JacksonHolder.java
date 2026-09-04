package com.example.test.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * 共享 ObjectMapper：过滤器等非 Spring Bean 环境也能做 JSON 序列化/反序列化
 */
public final class JacksonHolder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JacksonHolder() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException("JSON 序列化失败", error);
        }
    }

    /**
     * 宽松解析 JSON：解析失败返回 null（用于日志等容错场景）
     */
    public static JsonNode parseLenient(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(text);
        } catch (Exception error) {
            return null;
        }
    }

    public static List<Map<String, Object>> parseList(String text) {
        try {
            return MAPPER.readValue(text, new TypeReference<>() {
            });
        } catch (Exception error) {
            throw new IllegalArgumentException("JSON 数组格式不正确", error);
        }
    }

    public static Map<String, Object> parseMap(String text) {
        try {
            return MAPPER.readValue(text, new TypeReference<>() {
            });
        } catch (Exception error) {
            throw new IllegalArgumentException("JSON 对象格式不正确", error);
        }
    }
}
