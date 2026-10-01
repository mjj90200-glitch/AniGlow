package com.aniglow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String storageType;

    public WebConfig(@Value("${aniglow.upload.dir}") String uploadDir,
                     @Value("${aniglow.storage.type:local}") String storageType) {
        this.uploadDir = uploadDir;
        this.storageType = storageType;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // OSS 模式下 /api/images/** 由 ImageCompatController 接管并 302 到对象存储
        if ("oss".equalsIgnoreCase(storageType)) {
            return;
        }
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir + "/images/")
                .setCachePeriod(86400);
    }
}
