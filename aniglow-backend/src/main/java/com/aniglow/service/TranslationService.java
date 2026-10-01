package com.aniglow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 翻译服务
 * 通过 Spring AI ChatClient 调用模型（OpenAI 兼容端点，见 spring.ai.deepseek.* 配置）
 */
@Slf4j
@Service
public class TranslationService {

    @Value("${spring.ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${spring.ai.deepseek.chat.options.model:deepseek-flash}")
    private String model;

    /** 批量翻译专用模型：非推理模式（deepseek-chat），标题/简介批量处理对延迟敏感 */
    @Value("${aniglow.translation.model:deepseek-chat}")
    private String fastModel;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public TranslationService(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
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
                    .system("""
                            你是资深的动漫领域译名专家，熟悉 B站、巴哈姆特、萌娘百科等中文动漫社区通行的译名惯例。
                            任务：给出该动漫在中文圈最通行的译名。
                            规则：
                            1. 若该作品有官方中文译名或社区公认译名（如「葬送的芙莉莲」「进击的巨人」这类），必须使用公认译名，绝对不要自行直译。
                            2. 只有确认该作品没有通行中文译名时，才给出贴合原意、符合中文动漫圈命名习惯的译名。
                            3. 直接输出译名本身：不要解释、不要引号、不要书名号、不要标注「译」字。
                            """)
                    .user(sourceTitle)
                    .options(ChatOptions.builder()
                            .model(fastModel)
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
     * 批量翻译动漫标题：单次调用处理整批（编号列表 → JSON 映射），
     * 把逐条调用的固定开销摊薄一个数量级。
     * @param titles 标题列表（日文/英文/罗马音）
     * @return 与输入等长的结果列表；识别失败/未返回的位置为 null
     */
    public List<String> translateTitleBatch(List<String> titles) {
        if (titles == null || titles.isEmpty()) return List.of();
        if (apiKey == null || apiKey.isEmpty()) return Collections.nCopies(titles.size(), null);

        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < titles.size(); i++) {
            numbered.append(i + 1).append(". ").append(titles.get(i) == null ? "" : titles.get(i)).append('\n');
        }

        try {
            String content = chatClient.prompt()
                    .system("""
                            你是资深的动漫领域译名专家，熟悉 B站、巴哈姆特、萌娘百科等中文动漫社区通行的译名惯例。
                            用户给出编号的动漫标题列表。对每个编号给出该作品在中文圈最通行的译名：
                            1. 有官方中文译名或社区公认译名的，必须使用公认译名，绝对不要自行直译。
                            2. 没有通行译名的，给贴合原意、符合中文动漫圈命名习惯的译名。
                            3. 严格输出 JSON 对象：{"1":"译名","2":"译名"}，键与输入编号一致；无法确定时对应值为 null。
                            4. 不要输出 JSON 以外的任何内容。
                            """)
                    .user(numbered.toString())
                    .options(ChatOptions.builder()
                            .model(fastModel)
                            .temperature(0.1)
                            .maxTokens(2000)
                            .build())
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                return Collections.nCopies(titles.size(), null);
            }
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) {
                log.warn("批量标题翻译返回非 JSON: {}", content.substring(0, Math.min(content.length(), 120)));
                return Collections.nCopies(titles.size(), null);
            }
            JsonNode node = objectMapper.readTree(content.substring(start, end + 1));
            List<String> results = new ArrayList<>(Collections.nCopies(titles.size(), (String) null));
            for (int i = 0; i < titles.size(); i++) {
                String v = node.path(String.valueOf(i + 1)).asText(null);
                if (v != null && !v.isBlank()) results.set(i, v.trim());
            }
            return results;
        } catch (Exception e) {
            log.warn("批量标题翻译失败: {}", e.getMessage());
            return Collections.nCopies(titles.size(), null);
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
                    .system("""
                            你是资深的动漫内容编辑，为中文动漫社区撰写简介。
                            任务：把英文动漫简介改写成流畅自然的中文简介。
                            规则：
                            1. 意译优先，杜绝翻译腔：不要「一个关于…的故事」这类直译句式，用人话重新讲。
                            2. 作品名、人名使用中文圈通行译名。
                            3. 保留原文的分段结构，篇幅与原文相当，不添加原文没有的信息。
                            4. 直接输出译文，不要任何解释或前缀。
                            """)
                    .user("翻译成中文，保持简洁：\n" + toTranslate)
                    .options(ChatOptions.builder()
                            .model(fastModel)
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
