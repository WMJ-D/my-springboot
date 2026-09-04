package com.example.test.common;

/**
 * 业务异常，与 Express 侧 AppError 对应：携带 HTTP 状态码与业务错误码
 */
public class AppException extends RuntimeException {

    private final int status;
    private final String code;
    private final transient Object details;

    public AppException(int status, String message) {
        this(status, message, "BAD_REQUEST", null);
    }

    public AppException(int status, String message, String code) {
        this(status, message, code, null);
    }

    public AppException(int status, String message, String code, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }
}
