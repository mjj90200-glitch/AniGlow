package com.aniglow.config;

import com.aniglow.storage.AliyunOssImageStore;
import com.aniglow.storage.ImageStore;
import com.aniglow.storage.LocalImageStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图片存储装配：aniglow.storage.type = local（默认）| oss
 * OSS 凭据仅从环境变量读取（OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET）。
 */
@Configuration
public class StorageConfig {

    @Bean
    public ImageStore imageStore(
            @Value("${aniglow.storage.type:local}") String storageType,
            @Value("${aniglow.upload.dir}") String uploadDir,
            @Value("${aniglow.oss.endpoint:}") String endpoint,
            @Value("${aniglow.oss.bucket:}") String bucket,
            @Value("${aniglow.oss.access-key-id:}") String accessKeyId,
            @Value("${aniglow.oss.access-key-secret:}") String accessKeySecret,
            @Value("${aniglow.oss.custom-domain:}") String customDomain
    ) {
        if ("oss".equalsIgnoreCase(storageType)) {
            if (endpoint.isBlank() || bucket.isBlank() || accessKeyId.isBlank() || accessKeySecret.isBlank()) {
                throw new IllegalStateException(
                        "aniglow.storage.type=oss 但 OSS_ENDPOINT/OSS_BUCKET/OSS_ACCESS_KEY_ID/OSS_ACCESS_KEY_SECRET 未配置完整");
            }
            return new AliyunOssImageStore(endpoint, bucket, accessKeyId, accessKeySecret, customDomain);
        }
        return new LocalImageStore(uploadDir);
    }
}
