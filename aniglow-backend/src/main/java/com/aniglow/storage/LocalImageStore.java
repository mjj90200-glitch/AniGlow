package com.aniglow.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地磁盘实现（默认）：写入 {aniglow.upload.dir}/images/{key}，
 * 由 WebConfig 的资源映射以 /api/images/** 对外提供服务。开发与单机部署兜底。
 */
@Slf4j
public class LocalImageStore implements ImageStore {

    private final Path baseDir;

    public LocalImageStore(String uploadDir) {
        this.baseDir = Path.of(uploadDir, "images");
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(baseDir);
            log.info("本地图片存储目录已初始化: {}", baseDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("无法创建图片存储目录: {}", baseDir, e);
        }
    }

    @Override
    public String store(byte[] data, String key) {
        Path dest = baseDir.resolve(key);
        try {
            Files.createDirectories(dest.getParent());
            Files.write(dest, data);
        } catch (IOException e) {
            throw new RuntimeException("写入本地图片失败: " + key, e);
        }
        return url(key);
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(baseDir.resolve(key));
    }

    @Override
    public String url(String key) {
        return "/api/images/" + key;
    }

    @Override
    public String type() {
        return "local";
    }
}
