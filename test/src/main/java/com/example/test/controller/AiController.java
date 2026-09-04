package com.example.test.controller;

import com.example.test.common.AppException;
import com.example.test.common.JacksonHolder;
import com.example.test.service.AiGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 对话接口，与 Express 侧 /api/v1/ai/chat/stream 保持一致：
 * multipart（message + history + files）入参，ndjson 流式输出 start/thinking/delta/done/error
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 20L * 1024 * 1024;
    private static final int MAX_HISTORY_MESSAGES = 30;
    private static final Set<String> TEXT_FILE_TYPES = Set.of(
            "application/json", "application/xml", "application/javascript", "application/sql",
            "text/plain", "text/markdown", "text/csv", "text/html", "text/css", "text/xml",
            "text/javascript", "text/typescript", "application/x-yaml", "text/yaml");

    private final AiGatewayService aiGatewayService;

    public AiController(AiGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    /**
     * POST /api/v1/ai/chat/stream 多模态流式对话
     */
    @PostMapping("/chat/stream")
    public void chatStream(@RequestParam(required = false) String message,
                           @RequestParam(required = false) String history,
                           @RequestParam(required = false) List<MultipartFile> files,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        checkContentLength(request);
        List<Map<String, Object>> historyMessages = parseHistory(history);
        List<MultipartFile> fileList = files == null ? List.of() : files;
        String text = message == null ? "" : message.trim();

        if (text.isEmpty() && fileList.isEmpty()) {
            throw new AppException(400, "请输入消息或选择文件", "EMPTY_MESSAGE");
        }
        if (fileList.size() > MAX_FILES) {
            throw new AppException(400, "最多上传 " + MAX_FILES + " 个文件", "TOO_MANY_FILES");
        }
        long totalSize = 0;
        for (MultipartFile file : fileList) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new AppException(400, "文件 " + file.getOriginalFilename() + " 超过 10MB 限制", "FILE_TOO_LARGE");
            }
            totalSize += file.getSize();
        }
        if (totalSize > MAX_TOTAL_SIZE) {
            throw new AppException(413, "文件总大小超过 20MB 限制", "PAYLOAD_TOO_LARGE");
        }

        List<Object> content = new ArrayList<>();
        if (!text.isEmpty()) {
            content.add(Map.of("type", "text", "text", text));
        }
        for (MultipartFile file : fileList) {
            content.add(fileToContent(file));
        }

        List<Object> messages = new ArrayList<>(historyMessages);
        messages.add(Map.of("role", "user", "content", content));

        response.setStatus(200);
        response.setContentType("application/x-ndjson; charset=utf-8");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            writeEvent(response, Map.of("type", "start"));
            aiGatewayService.streamCompletion(messages, new AiGatewayService.StreamListener() {
                @Override
                public void onThinking() {
                    writeEvent(response, Map.of("type", "thinking"));
                }

                @Override
                public void onDelta(String delta) {
                    writeEvent(response, Map.of("type", "delta", "content", delta));
                }
            });
            writeEvent(response, Map.of("type", "done"));
        } catch (ClientDisconnectedException ignored) {
            // 客户端已断开：终止流式输出（上游连接由服务内的 try-with-resources 关闭）
        } catch (AppException error) {
            writeEventQuietly(response, Map.of("type", "error", "message", error.getMessage()));
        } catch (Exception error) {
            log.error("AI 流式对话失败", error);
            writeEventQuietly(response, Map.of("type", "error", "message", "AI 服务暂时不可用"));
        }
    }

    private void checkContentLength(HttpServletRequest request) {
        String contentLength = request.getHeader("Content-Length");
        if (contentLength != null) {
            try {
                if (Long.parseLong(contentLength) > MAX_TOTAL_SIZE) {
                    throw new AppException(413, "本次上传内容超过 20MB 限制", "PAYLOAD_TOO_LARGE");
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /**
     * 历史消息校验：最多 30 条，role ∈ user/assistant/system，content ≤ 100000 字符
     */
    private List<Map<String, Object>> parseHistory(String history) {
        if (history == null || history.isBlank()) {
            return List.of();
        }
        List<Map<String, Object>> parsed;
        try {
            parsed = JacksonHolder.parseList(history);
        } catch (Exception error) {
            throw new AppException(400, "历史消息格式不正确", "INVALID_HISTORY");
        }
        if (parsed.size() > MAX_HISTORY_MESSAGES) {
            throw new AppException(400, "历史消息格式不正确", "INVALID_HISTORY");
        }
        List<Map<String, Object>> result = new ArrayList<>(parsed.size());
        for (Map<String, Object> item : parsed) {
            Object role = item.get("role");
            Object content = item.get("content");
            if (!"user".equals(role) && !"assistant".equals(role) && !"system".equals(role)) {
                throw new AppException(400, "历史消息格式不正确", "INVALID_HISTORY");
            }
            if (!(content instanceof String text) || text.length() > 100000) {
                throw new AppException(400, "历史消息格式不正确", "INVALID_HISTORY");
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("role", role);
            normalized.put("content", content);
            result.add(normalized);
        }
        return result;
    }

    /**
     * 文件转 OpenAI 多模态内容：图片 → image_url，文本 → text，其他 → file（base64）
     */
    private Map<String, Object> fileToContent(MultipartFile file) throws IOException {
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        byte[] bytes = file.getBytes();

        if (contentType.startsWith("image/")) {
            return Map.of("type", "image_url",
                    "image_url", Map.of("url", "data:" + contentType + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes)));
        }
        if (contentType.startsWith("text/") || TEXT_FILE_TYPES.contains(contentType)) {
            String text = new String(bytes, StandardCharsets.UTF_8);
            return Map.of("type", "text", "text", "\n\n[文件：" + filename + "]\n" + text);
        }
        return Map.of("type", "file",
                "file", Map.of("filename", filename,
                        "file_data", "data:" + contentType + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes)));
    }

    /**
     * 客户端已断开（写入响应失败）
     */
    private static final class ClientDisconnectedException extends RuntimeException {
        ClientDisconnectedException(IOException cause) {
            super(cause);
        }
    }

    private void writeEvent(HttpServletResponse response, Map<String, Object> event) {
        try {
            OutputStream output = response.getOutputStream();
            output.write((JacksonHolder.toJson(event) + "\n").getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (IOException error) {
            throw new ClientDisconnectedException(error);
        }
    }

    private void writeEventQuietly(HttpServletResponse response, Map<String, Object> event) {
        try {
            writeEvent(response, event);
        } catch (ClientDisconnectedException ignored) {
            // 客户端已断开，忽略
        }
    }
}
