package com.aniglow.controller;

import com.aniglow.storage.ImageStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 存量图片兼容层（仅 oss 模式启用）：
 * 数据库里已保存的相对路径 /api/images/{key} 通过 302 跳转到对象存储地址，
 * 新旧地址同时可用，待存量迁移完成后可逐步淘汰。
 */
@RestController
@ConditionalOnProperty(name = "aniglow.storage.type", havingValue = "oss")
public class ImageCompatController {

    private final ImageStore imageStore;

    public ImageCompatController(ImageStore imageStore) {
        this.imageStore = imageStore;
    }

    @GetMapping("/images/**")
    public void redirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String key = extractKey(request);
        if (key.isBlank() || key.contains("..")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // 不做 exists 预检（省一次 OSS API 调用），缺失对象由 OSS 返回 404
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.sendRedirect(imageStore.url(key));
    }

    private String extractKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());
        if (!path.startsWith("/images/")) return "";
        String key = path.substring("/images/".length());
        return URLDecoder.decode(key, StandardCharsets.UTF_8);
    }
}
