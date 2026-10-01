package com.aniglow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 翻译服务
 * 通过 Spring AI ChatClient 调用模型（OpenAI 兼容端点，见 spring.ai.openai.* 配置）
 */
@Slf4j
@Service
public class TranslationService {

    @Value("${spring.ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${spring.ai.deepseek.chat.options.model:deepseek-flash}")
    private String model;

    private final ChatClient chatClient;

    public TranslationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 将动漫标题翻译为简体中文
     * @param titleJapanese 日文标题（优先使用）
     * @param title 罗马音/英文标题（日文为空时使用）
     * @return 中文翻译，失败返回 null
     */
    public String translateTitleToChinese(String titleJapanese, String title) {
        if (apiKey == null || apiKey.isEmpty()) return null;

        String sourceTitle = (titleJapanese != null && !titleJapanese.isBlank())
                ? titleJapanese
                : (title != null && !title.isBlank()) ? title : null;
        if (sourceTitle == null) return null;

        try {
            String result = chatClient.prompt()
                    .system("你是一个专业动漫翻译。将动漫标题翻译成中文。直接输出中文译名，不要加任何解释、前缀或标点符号。如果标题已有公认的中文译名（如B站、巴哈姆特等平台使用的译名），请使用该译名。只输出译名本身。")
                    .user(sourceTitle)
                    .options(ChatOptions.builder()
                            .model(model)
                            .temperature(0.1)
                            .maxTokens(30)
                            .build())
                    .call()
                    .content();
            return (result == null || result.isBlank()) ? null : result.trim();
        } catch (Exception e) {
            log.warn("标题翻译失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将英文文本翻译为简体中文
     * @param text 英文原文
     * @return 中文翻译，失败返回 null
     */
    public String translateToChinese(String text) {
        if (apiKey == null || apiKey.isEmpty() || text == null || text.isBlank()) {
            return null;
        }

        // 超长文本截断（API token 限制）
        String toTranslate = text.length() > 800 ? text.substring(0, 800) + "..." : text;

        try {
            String result = chatClient.prompt()
                    .system("你是一个专业动漫翻译。将英文动漫简介翻译成流畅简洁的中文。直接输出中文译文，不要加任何解释或前缀。保持原文的分段和语气。")
                    .user("翻译成中文，保持简洁：\n" + toTranslate)
                    .options(ChatOptions.builder()
                            .model(model)
                            .temperature(0.3)
                            .maxTokens(300)
                            .build())
                    .call()
                    .content();
            return (result == null || result.isBlank()) ? null : result.trim();
        } catch (Exception e) {
            log.warn("翻译失败: {}", e.getMessage());
            return null;
        }
    }
}
