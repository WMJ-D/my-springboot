package com.example.test.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一响应结构，与 Express 侧 utils/response.js 保持一致：
 * 成功 { code: 0, message, data }
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "操作成功", data);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(0, message, data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
