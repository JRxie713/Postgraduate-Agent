package com.demo.scaffoldbackend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 大模型对话客户端（DeepSeek，OpenAI 兼容协议）
 *
 * <p>密钥从环境变量 DEEPSEEK_API_KEY 读取（backend/.env）。未配置密钥、网络异常、
 * 返回非 200 或解析失败时统一返回 null，由调用方走规则化兜底，保证无 Key 也能完整演示。</p>
 */
@Slf4j
@Service
public class AiChatClient {

    @Value("${DEEPSEEK_API_KEY:}")
    private String apiKey;

    @Value("${DEEPSEEK_BASE_URL:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${DEEPSEEK_MODEL:deepseek-flash}")
    private String model;

    /** 单次调用最大输出 token（英文点评/报告返回较长，留足余量避免 JSON 被截断） */
    @Value("${DEEPSEEK_MAX_TOKENS:2000}")
    private int maxTokens;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 是否已配置密钥
     */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 发起一次对话
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @return 模型回答正文；任何异常返回 null
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (!isEnabled()) {
            return null;
        }
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("stream", false);
            root.put("max_tokens", maxTokens);
            root.put("temperature", 0.7);
            ArrayNode messages = root.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);

            String body = objectMapper.writeValueAsString(root);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(90))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.warn("[AI] 调用失败 status={} body={}", response.statusCode(), abbreviate(response.body()));
                return null;
            }
            JsonNode node = objectMapper.readTree(response.body());
            String content = node.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                log.warn("[AI] 返回内容为空");
                return null;
            }
            return content.trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[AI] 调用被中断: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("[AI] 调用异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从模型输出中提取 JSON（自动剥离 ```json 代码块与前后缀说明文字）
     */
    public String extractJson(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
            text = text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * 解析 JSON，失败返回 null
     */
    public JsonNode parseJson(String raw) {
        String json = extractJson(raw);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("[AI] JSON 解析失败: {}", abbreviate(json));
            return null;
        }
    }

    private String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }
}
