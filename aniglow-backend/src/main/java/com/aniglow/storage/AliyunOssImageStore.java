package com.aniglow.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

/**
 * 阿里云 OSS 实现。凭据仅通过环境变量注入（.env / .env.local），不落代码。
 * 同地域 ECS 上传走内网端点（内网传输免费）；用户访问走自定义域名或 Bucket 默认域名。
 */
@Slf4j
public class AliyunOssImageStore implements ImageStore {

    private static final String KEY_PREFIX = "images/";

    private final OSS ossClient;
    private final String bucket;
    private final String endpoint;
    private final String customDomain;

    public AliyunOssImageStore(String endpoint, String bucket,
                               String accessKeyId, String accessKeySecret,
                               String customDomain) {
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        this.bucket = bucket;
        this.endpoint = endpoint;
        this.customDomain = customDomain == null ? "" : customDomain.trim();
        log.info("阿里云 OSS 存储已初始化: bucket={}, endpoint={}, customDomain={}",
                bucket, endpoint, this.customDomain.isBlank() ? "(默认域名)" : this.customDomain);
    }

    @Override
    public String store(byte[] data, String key) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);
        metadata.setContentType("image/jpeg");
        // 让浏览器直接预览而不是下载
        metadata.setObjectAcl(null); // 使用 Bucket 级公共读，避免逐对象设置
        ossClient.putObject(bucket, KEY_PREFIX + key, new ByteArrayInputStream(data), metadata);
        return url(key);
    }

    @Override
    public boolean exists(String key) {
        return ossClient.doesObjectExist(bucket, KEY_PREFIX + key);
    }

    @Override
    public String url(String key) {
        String objectUrl = KEY_PREFIX + key;
        if (!customDomain.isBlank()) {
            String base = customDomain.endsWith("/") ? customDomain.substring(0, customDomain.length() - 1) : customDomain;
            return base + "/" + objectUrl;
        }
        String host = endpoint.replaceFirst("^https?://", "");
        return "https://" + bucket + "." + host + "/" + objectUrl;
    }

    @Override
    public String type() {
        return "oss";
    }
}
