package com.example.test.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理，与 Express 侧 middleware/error.js 的响应结构保持一致
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数校验失败（zod 的 VALIDATION_ERROR 对应实现）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleValidation(Exception error) {
        List<Map<String, Object>> details = null;
        if (error instanceof MethodArgumentNotValidException valid) {
            details = valid.getBindingResult().getFieldErrors().stream().map(field -> {
                Map<String, Object> item = new HashMap<>();
                item.put("field", field.getField());
                item.put("message", field.getDefaultMessage());
                return item;
            }).toList();
        }
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "请求参数不合法", details);
    }

    /**
     * 唯一键冲突（ER_DUP_ENTRY）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateKeyException error) {
        return body(HttpStatus.CONFLICT, "DUPLICATE_DATA", "数据已存在", null);
    }

    /**
     * 上传内容超过大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUpload(MaxUploadSizeExceededException error) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "本次上传内容超过 20MB 限制", null);
    }

    /**
     * multipart 格式不正确
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(MultipartException error) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_MULTIPART", "上传内容格式不正确", null);
    }

    /**
     * 路由不存在
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException error) {
        return body(HttpStatus.NOT_FOUND, "NOT_FOUND", "接口不存在", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException error) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "请求方法不支持", null);
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleApp(AppException error) {
        HttpStatus status = HttpStatus.resolve(error.getStatus());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        if (status.is5xxServerError()) {
            log.error("请求处理失败", error);
        }
        return body(status, error.getCode(), status.is5xxServerError() ? "服务器内部错误" : error.getMessage(), error.getDetails());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception error) {
        log.error("请求处理失败", error);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务器内部错误", null);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message, Object details) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("message", message);
        if (details != null) {
            payload.put("details", details);
        }
        return ResponseEntity.status(status).body(payload);
    }
}
