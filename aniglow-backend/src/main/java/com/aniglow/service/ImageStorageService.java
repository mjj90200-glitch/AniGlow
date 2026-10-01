package com.aniglow.service;

import com.aniglow.storage.ImageStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

/**
 * 图片上传处理服务：负责校验、缩放与压缩，
 * 持久化委托给 ImageStore（本地磁盘 / 阿里云 OSS，由 aniglow.storage.type 决定）。
 */
@Service
@Slf4j
public class ImageStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_LONG_EDGE = 1920;
    private static final float JPEG_QUALITY = 0.80f;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final int MAX_IMAGES = 9;

    private final ImageStore imageStore;

    public ImageStorageService(ImageStore imageStore) {
        this.imageStore = imageStore;
    }

    public List<String> storeImages(MultipartFile[] files) {
        if (files == null || files.length == 0) return List.of();
        if (files.length > MAX_IMAGES) throw new IllegalArgumentException("最多上传 " + MAX_IMAGES + " 张图片");

        LocalDate today = LocalDate.now();

        // 并行处理图片，保持顺序
        String[] results = new String[files.length];
        IntStream.range(0, files.length).parallel().forEach(i -> {
            MultipartFile file = files[i];
            if (file.isEmpty()) return;
            validateFile(file);

            String key = String.format("%04d/%02d/%s.jpg",
                    today.getYear(), today.getMonthValue(), UUID.randomUUID().toString().replace("-", ""));

            try {
                byte[] payload;
                if (isDirectCopyEligible(file)) {
                    // JPEG 且长边不超过限制时，直接使用原始字节，省去解码→编码的 CPU 开销
                    payload = file.getBytes();
                } else {
                    BufferedImage original = ImageIO.read(file.getInputStream());
                    if (original == null)
                        throw new IllegalArgumentException("无法解析图片: " + file.getOriginalFilename());
                    // 透明 PNG 等含 alpha 的图必须先铺底转 RGB，否则 JPEG 编码器报 Bogus input colorspace
                    payload = toJpegBytes(resizeIfNeeded(toRgbWithWhiteBackground(original)));
                }

                results[i] = imageStore.store(payload, key);
            } catch (IOException e) {
                throw new RuntimeException("图片处理失败: " + file.getOriginalFilename(), e);
            }
        });

        return Arrays.stream(results).filter(Objects::nonNull).toList();
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("图片过大，单张不能超过 10MB: " + file.getOriginalFilename());
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的图片格式: " + contentType);
        }
    }

    /**
     * JPEG 且长边不超过限制时，直接拷贝原始字节（仅判断，不落盘）
     */
    private boolean isDirectCopyEligible(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("image/jpeg")) {
            return false;
        }
        try (ImageInputStream iis = ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return false;
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                return w <= MAX_LONG_EDGE && h <= MAX_LONG_EDGE;
            } finally {
                reader.dispose();
            }
        }
    }

    /** 含透明通道的图（PNG 等）铺白底转为 RGB，兼容 JPEG 编码器 */
    private BufferedImage toRgbWithWhiteBackground(BufferedImage src) {
        if (!src.getColorModel().hasAlpha()) return src;
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(src, 0, 0, java.awt.Color.WHITE, null);
        g.dispose();
        return rgb;
    }

    private BufferedImage resizeIfNeeded(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= MAX_LONG_EDGE && h <= MAX_LONG_EDGE) return original;

        double scale = (double) MAX_LONG_EDGE / Math.max(w, h);
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();
        return resized;
    }

    private byte[] toJpegBytes(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
            return out.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
