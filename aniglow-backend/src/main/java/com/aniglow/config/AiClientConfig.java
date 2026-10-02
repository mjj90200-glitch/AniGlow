package com.aniglow.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/**
 * 统一模型接口：ChatClient 由 spring.ai.deepseek.* 配置驱动，
 * 切换供应商（DeepSeek / 火山方舟 / GLM 等 OpenAI 兼容端点）只需改配置，不改代码。
 */
@Configuration
public class AiClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * RestClient 读超时：DeepSeek 偶发静默连接（响应体永不返回），
     * 无超时会导致调用线程无限挂起、批量翻译整体卡死。
     * 用 SimpleClientHttpRequestFactory：socket 级 SO_TIMEOUT 连响应体读取也覆盖。
     */
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(90).toMillis());
        return builder -> builder.requestFactory(factory);
    }
}
