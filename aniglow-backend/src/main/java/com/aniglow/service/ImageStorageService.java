package com.aniglow.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

@Service
@Slf4j
public class ImageStorageService {

    private final Path baseDir;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_LONG_EDGE = 1920;
    private static final float JPEG_QUALITY = 0.80f;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final int MAX_IMAGES = 9;

    public ImageStorageService(@Value("${aniglow.upload.dir}") String uploadDir) {
        this.baseDir = Path.of(uploadDir, "images");
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(baseDir);
            log.info("图片存储目录已初始化: {}", baseDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("无法创建图片存储目录: {}", baseDir, e);
        }
    }

    public List<String> storeImages(MultipartFile[] files) {
        if (files == null || files.length == 0) return List.of();
        if (files.length > MAX_IMAGES) throw new IllegalArgumentException("最多上传 " + MAX_IMAGES + " 张图片");

        LocalDate today = LocalDate.now();
        Path dateDir = baseDir.resolve(String.format("%04d/%02d", today.getYear(), today.getMonthValue()));
        try {
            Files.createDirectories(dateDir);
        } catch (IOException e) {
            throw new RuntimeException("创建日期目录失败", e);
        }

        // 并行处理图片，保持顺序
        String[] results = new String[files.length];
        IntStream.range(0, files.length).parallel().forEach(i -> {
            MultipartFile file = files[i];
            if (file.isEmpty()) return;
            validateFile(file);

            String uuid = UUID.randomUUID().toString().replace("-", "");
            Path dest = dateDir.resolve(uuid + ".jpg");

            try {
                if (!tryDirectCopy(file, dest)) {
                    BufferedImage original = ImageIO.read(file.getInputStream());
                    if (original == null)
                        throw new IllegalArgumentException("无法解析图片: " + file.getOriginalFilename());
                    BufferedImage processed = resizeIfNeeded(original);
                    writeAsJpeg(processed, dest.toFile());
                }

                String urlPath = String.format("/api/images/%04d/%02d/%s.jpg",
                        today.getYear(), today.getMonthValue(), uuid);
                results[i] = urlPath;
            } catch (IOException e) {
                throw new RuntimeException("图片处理失败: " + file.getOriginalFilename(), e);
            }
        });

        return Arrays.stream(results).filter(Objects::nonNull).toList();
    }

    /**
     * JPEG 且长边不超过限制时，直接拷贝原始字节，省去解码→编码的 CPU 开销
     */
    private boolean tryDirectCopy(MultipartFile file, Path dest) throws IOException {
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
                if (w <= MAX_LONG_EDGE && h <= MAX_LONG_EDGE) {
                    Files.copy(file.getInputStream(), dest);
                    return true;
                }
            } finally {
                reader.dispose();
            }
        }
        return false;
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

    private void writeAsJpeg(BufferedImage image, java.io.File dest) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(dest)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
