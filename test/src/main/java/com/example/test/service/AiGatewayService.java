package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.JacksonHolder;
import com.example.test.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 网关流式服务，对应 Express 侧 services/ai.js：
 * 调用 OpenAI 兼容 /chat/completions SSE 接口并逐段回调
 */
@Service
public class AiGatewayService {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayService.class);

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final AppProperties appProperties;
    private final HttpClient httpClient;

    public AiGatewayService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 流式回调接口
     */
    public interface StreamListener {

        void onThinking();

        void onDelta(String content);
    }

    /**
     * 流式对话：messages 为 OpenAI 消息结构，逐段回调 onThinking/onDelta
     */
    public void streamCompletion(List<Object> messages, StreamListener listener) throws IOException, InterruptedException {
        String apiKey = appProperties.getAi().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException(503, "AI 服务尚未配置 API Key", "AI_NOT_CONFIGURED");
        }
        String url = appProperties.getAi().getGatewayBaseUrl().replaceAll("/$", "") + CHAT_COMPLETIONS_PATH;

        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "请始终使用规范的 Markdown 格式回答。合理使用标题、段落、列表、引用、表格和代码块；代码块必须标注语言。不要输出 HTML 标签。");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", appProperties.getAi().getModel());
        payload.put("messages", mergeMessages(systemMessage, messages));
        payload.put("stream", true);
        payload.put("reasoning_effort", "xhigh");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(JacksonHolder.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AppException(response.statusCode(), readUpstreamError(response), "AI_GATEWAY_ERROR");
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (contentType.contains("application/json")) {
            // 非流式响应：整体输出
            String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = JacksonHolder.parseLenient(body);
            String content = extractTextContent(root == null ? null : root.path("choices").path(0).path("message").path("content"));
            if (content != null && !content.isEmpty()) {
                listener.onDelta(content);
            }
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            boolean thinkingNotified = false;
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith(":") || !trimmed.startsWith("data:")) {
                    continue;
                }
                String data = trimmed.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    return;
                }
                JsonNode payloadNode = JacksonHolder.parseLenient(data);
                if (payloadNode == null) {
                    continue;
                }
                JsonNode delta = payloadNode.path("choices").path(0).path("delta");
                JsonNode reasoning = delta.path("reasoning_content");
                if (reasoning.isTextual() && !reasoning.asText().isEmpty() && !thinkingNotified) {
                    thinkingNotified = true;
                    listener.onThinking();
                }
                String content = extractTextContent(delta.path("content"));
                if (content != null && !content.isEmpty()) {
                    listener.onDelta(content);
                }
            }
        }
    }

    /**
     * content 可能是字符串，也可能是 [{type:'text',text},...] 结构
     */
    private static String extractTextContent(JsonNode content) {
        if (content == null || content.isMissingNode() || content.isNull()) {
            return null;
        }
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : content) {
                if (item.isTextual()) {
                    builder.append(item.asText());
                } else if (item.has("text")) {
                    builder.append(item.path("text").asText(""));
                } else if (item.has("content")) {
                    builder.append(item.path("content").asText(""));
                }
            }
            return builder.toString();
        }
        return null;
    }

    private static List<Object> mergeMessages(Map<String, Object> systemMessage, List<Object> messages) {
        List<Object> all = new ArrayList<>(messages.size() + 1);
        all.add(systemMessage);
        all.addAll(messages);
        return all;
    }

    private String readUpstreamError(HttpResponse<java.io.InputStream> response) {
        try {
            String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = JacksonHolder.parseLenient(body);
            if (root != null) {
                String message = root.path("error").path("message").asText("");
                if (!message.isEmpty()) {
                    return message;
                }
                message = root.path("message").asText("");
                if (!message.isEmpty()) {
                    return message;
                }
            }
            return body.isBlank() ? "AI 网关请求失败（" + response.statusCode() + "）" : body;
        } catch (IOException error) {
            log.error("读取 AI 网关错误响应失败", error);
            return "AI 网关请求失败（" + response.statusCode() + "）";
        }
    }
}
