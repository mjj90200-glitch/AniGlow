package com.aniglow.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一模型接口：ChatClient 由 spring.ai.openai.* 配置驱动，
 * 切换供应商（DeepSeek / 火山方舟 / GLM 等 OpenAI 兼容端点）只需改配置，不改代码。
 */
@Configuration
public class AiClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
