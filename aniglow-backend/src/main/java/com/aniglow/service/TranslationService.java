package com.aniglow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 翻译服务
 * 用火山方舟 Ark API 将英文简介翻译为中文
 */
@Slf4j
@Service
public class TranslationService {

    @Value("${aniglow.ai.openai.api-key:}")
    private String apiKey;

    @Value("${aniglow.ai.openai.base-url:https://ark.cn-beijing.volces.com/api/coding/v3}")
    private String baseUrl;

    @Value("${aniglow.ai.openai.chat.options.model:doubao-seed-2.0-pro}")
    private String model;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * 将动漫标题翻译为简体中文
     * @param titleJapanese 日文标题（优先使用）
     * @param title 罗马音/英文标题（日文为空时使用）
     * @return 中文翻译，失败返回 null
     */
    @SuppressWarnings("unchecked")
    public String translateTitleToChinese(String titleJapanese, String title) {
        if (apiKey == null || apiKey.isEmpty()) return null;

        String sourceTitle = (titleJapanese != null && !titleJapanese.isBlank())
                ? titleJapanese
                : (title != null && !title.isBlank()) ? title : null;
        if (sourceTitle == null) return null;

        String endpoint = baseUrl.endsWith("/chat/completions")
                ? baseUrl
                : baseUrl + "/chat/completions";

        Map<String, Object> systemMsg = Map.of(
                "role", "system",
                "content", "你是一个专业动漫翻译。将动漫标题翻译成中文。直接输出中文译名，不要加任何解释、前缀或标点符号。如果标题已有公认的中文译名（如B站、巴哈姆特等平台使用的译名），请使用该译名。只输出译名本身。"
        );

        Map<String, Object> userMsg = Map.of(
                "role", "user",
                "content", sourceTitle
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(systemMsg, userMsg));
        payload.put("max_tokens", 30);
        payload.put("temperature", 0.1);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return ((String) message.get("content")).trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("标题翻译失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 将英文文本翻译为简体中文
     * @param text 英文原文
     * @return 中文翻译，失败返回 null
     */
    @SuppressWarnings("unchecked")
    public String translateToChinese(String text) {
        if (apiKey == null || apiKey.isEmpty() || text == null || text.isBlank()) {
            return null;
        }

        // 超长文本截断（API token 限制）
        String toTranslate = text.length() > 800 ? text.substring(0, 800) + "..." : text;

        String endpoint = baseUrl.endsWith("/chat/completions")
                ? baseUrl
                : baseUrl + "/chat/completions";

        Map<String, Object> systemMsg = Map.of(
                "role", "system",
                "content", "你是一个专业动漫翻译。将英文动漫简介翻译成流畅简洁的中文。" +
                           "直接输出中文译文，不要加任何解释或前缀。保持原文的分段和语气。"
        );

        Map<String, Object> userMsg = Map.of(
                "role", "user",
                "content", "翻译成中文，保持简洁：\n" + toTranslate
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(systemMsg, userMsg));
        payload.put("max_tokens", 300);
        payload.put("temperature", 0.3);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return ((String) message.get("content")).trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("翻译失败: {}", e.getMessage());
        }

        return null;
    }
}
