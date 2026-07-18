# 社区图文发帖功能 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为社区发帖新增图片附件功能（朋友圈式：可纯文字可附带1-9张图片），图片上传本地存储WebP压缩，帖子详情九宫格展示+灯箱预览。

**Architecture:** 后端新增 `CommunityPost.images` TEXT字段存JSON数组，新增图片上传端点 `POST /api/communities/upload/images`，图片存本地 `~/aniglow-data/images/` 并通过Spring Boot ResourceHandler提供访问。前端发帖弹窗加图片选择器+即时上传，帖子卡片加底部图片预览条，帖子详情加自适应网格+Lightbox。

**Tech Stack:** Spring Boot 3.2.5 + JPA + MySQL, Nuxt 3 + Vue 3 + Tailwind CSS, webp-imageio (图片处理)

**重要:** 用户通过 www.mjj520.top:3001 访问，后端在8081，Nuxt代理在3001。每次改动后需验证网站正常。

---

### Task 1: 添加 webp-imageio 依赖

**Files:**
- Modify: `aniglow-backend/pom.xml`

- [ ] **Step 1: 在 pom.xml dependencies 中添加 webp-imageio**

在 `<dependencies>` 标签内，最后一个 `</dependency>` 之后插入：

```xml
        <!-- WebP 图片处理 -->
        <dependency>
            <groupId>org.sejda.imageio</groupId>
            <artifactId>webp-imageio</artifactId>
            <version>0.2.2</version>
        </dependency>
```

- [ ] **Step 2: 验证依赖下载**

```bash
cd aniglow-backend && mvn dependency:resolve -q | grep webp-imageio
```

Expected: 输出 `org.sejda.imageio:webp-imageio:jar:0.2.2` 相关信息。

- [ ] **Step 3: Commit**

```bash
git add aniglow-backend/pom.xml
git commit -m "build: add webp-imageio dependency for image processing

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: 后端 — 实体/DTO/Request 新增 images 字段

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/entity/CommunityPost.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/dto/community/CommunityPostRequest.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/dto/community/CommunityPostDto.java`

- [ ] **Step 1: CommunityPost.java — 在 coverImage 字段后添加 images 字段**

在 `private String coverImage;` 行之后插入：

```java
    @Column(name = "images", columnDefinition = "TEXT")
    private String images;
```

注意：`@Getter @Setter` 是类级别注解，新字段自动生成 getter/setter，无需额外添加。

- [ ] **Step 2: CommunityPostRequest.java — 添加 images 字段**

在 `private String avatarUrl;` 行之前插入：

```java
    private List<String> images;
```

文件顶部需要添加 import：

```java
import java.util.List;
```

- [ ] **Step 3: CommunityPostDto.java — 添加 images 字段**

在 `private String coverImage;` 行之后插入：

```java
    private List<String> images;
```

文件顶部需要添加 import：

```java
import java.util.List;
```

- [ ] **Step 4: Commit**

```bash
git add aniglow-backend/src/main/java/com/aniglow/entity/CommunityPost.java \
        aniglow-backend/src/main/java/com/aniglow/dto/community/CommunityPostRequest.java \
        aniglow-backend/src/main/java/com/aniglow/dto/community/CommunityPostDto.java
git commit -m "feat: add images field to CommunityPost entity, DTO, and request

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: 后端 — 创建 ImageStorageService

**Files:**
- Create: `aniglow-backend/src/main/java/com/aniglow/service/ImageStorageService.java`

- [ ] **Step 1: 创建 ImageStorageService.java**

```java
package com.aniglow.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class ImageStorageService {

    private final Path baseDir;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
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

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            validateFile(file);

            String uuid = UUID.randomUUID().toString().replace("-", "");
            String ext = detectExtension(file);
            Path dest = dateDir.resolve(uuid + ext);

            try {
                BufferedImage original = ImageIO.read(file.getInputStream());
                if (original == null) throw new IllegalArgumentException("无法解析图片: " + file.getOriginalFilename());
                BufferedImage processed = resizeIfNeeded(original);
                writeAsJpeg(processed, dest.toFile());

                String urlPath = String.format("/api/images/%04d/%02d/%s.jpg",
                        today.getYear(), today.getMonthValue(), uuid);
                urls.add(urlPath);
            } catch (IOException e) {
                throw new RuntimeException("图片处理失败: " + file.getOriginalFilename(), e);
            }
        }
        return urls;
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("图片过大，单张不能超过 5MB: " + file.getOriginalFilename());
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的图片格式: " + contentType);
        }
    }

    private String detectExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            return original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        return ".jpg";
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
```

- [ ] **Step 2: Commit**

```bash
git add aniglow-backend/src/main/java/com/aniglow/service/ImageStorageService.java
git commit -m "feat: add ImageStorageService for image upload, resize, and compression

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: 后端 — 配置文件 + 静态资源映射

**Files:**
- Modify: `aniglow-backend/src/main/resources/application.yml`
- Create: `aniglow-backend/src/main/java/com/aniglow/config/WebConfig.java`

- [ ] **Step 1: application.yml — 在 aniglow 段落添加 upload 配置**

找到 `aniglow:` 段落（在 `bayesian:` 之前），在其下方插入 upload 配置。当前文件结构是：

```yaml
aniglow:
  bayesian:
    global-mean: 7.0
```

修改为：

```yaml
aniglow:
  upload:
    dir: ${user.home}/aniglow-data
    max-file-size: 5242880
    allowed-types: image/jpeg,image/png,image/gif,image/webp
  bayesian:
    global-mean: 7.0
```

同时需要增加 Spring 的 multipart 上传限制。在 `spring:` 段落下的任意位置添加（可在 `data.redis` 配置块之后）：

```yaml
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 50MB
```

- [ ] **Step 2: 创建 WebConfig.java**

```java
package com.aniglow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebConfig(@Value("${aniglow.upload.dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir + "/images/")
                .setCachePeriod(86400);
    }
}
```

由于 `server.servlet.context-path=/api`，`/images/**` 的实际访问路径为 `/api/images/**`。

- [ ] **Step 3: Commit**

```bash
git add aniglow-backend/src/main/resources/application.yml \
        aniglow-backend/src/main/java/com/aniglow/config/WebConfig.java
git commit -m "feat: add upload config and static resource mapping for images

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: 后端 — Controller 新增上传端点 + 更新现有逻辑

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/controller/CommunityController.java`

- [ ] **Step 1: 添加依赖注入和 import**

在文件顶部 import 区添加：

```java
import com.aniglow.service.ImageStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
```

修改构造函数注入，将：

```java
private final UserRepository userRepository;
```

改为：

```java
private final UserRepository userRepository;
private final ImageStorageService imageStorageService;
private final ObjectMapper objectMapper;
```

> 注意：`ObjectMapper` 由 Spring Boot 自动配置提供，直接声明即可注入。

- [ ] **Step 2: 添加图片上传端点**

在 `listCommunities()` 方法之前插入：

```java
    @PostMapping("/upload/images")
    @Operation(summary = "上传帖子图片", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @RequestParam("files") MultipartFile[] files) {
        try {
            List<String> urls = imageStorageService.storeImages(files);
            return ResponseEntity.ok(ApiResponse.success("上传成功", urls));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
```

- [ ] **Step 3: 更新 createPost 方法 — 存储 images JSON**

修改 `createPost` 方法中构建 `CommunityPost` 的部分。在 `.coverImage(trimToNull(request.getCoverImage()))` 之后添加：

```java
                .images(serializeImages(request.getImages()))
```

然后在类底部 `hasText` 方法旁边添加辅助方法：

```java
    private String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(images);
        } catch (IOException e) {
            return null;
        }
    }
```

需要添加 `IOException` import（已在 Step 1 处理）。

- [ ] **Step 4: 更新 convertPost 方法 — 解析 images JSON**

在 `convertPost` 方法的 builder 中，`.coverImage(post.getCoverImage())` 之后添加：

```java
                .images(parseImages(post.getImages()))
```

然后在 `serializeImages` 方法旁边添加：

```java
    private List<String> parseImages(String imagesJson) {
        if (!hasText(imagesJson)) return List.of();
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            return List.of();
        }
    }
```

- [ ] **Step 5: 编译验证**

```bash
cd aniglow-backend && mvn compile -q
```

Expected: BUILD SUCCESS，无编译错误。

- [ ] **Step 6: Commit**

```bash
git add aniglow-backend/src/main/java/com/aniglow/controller/CommunityController.java
git commit -m "feat: add image upload endpoint and wire images into post create/convert

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5.5: 前端 — 创建图片上传专用 Nitro 路由

**Files:**
- Create: `server/api/communities/upload/images.post.ts`

> **原因:** 现有 catch-all 代理 `server/api/[...].ts` 使用 `readBody(event)` 处理请求体，无法正确转发 multipart/form-data 文件数据。需要创建专用路由用 `readRawBody` + 原始 Content-Type 转发。

- [ ] **Step 1: 创建 `server/api/communities/upload/images.post.ts`**

```typescript
/**
 * 图片上传专用代理 — 转发 multipart/form-data 到 Spring Boot 后端
 * 必须使用 readRawBody 读取原始二进制数据，并保留 Content-Type 中的 boundary
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const backendUrl = (config.backendUrl || 'http://localhost:8081').replace(/\/$/, '')
  const target = `${backendUrl}${event.path}`

  const contentType = getHeader(event, 'content-type') || ''
  const rawBody = await readRawBody(event)

  try {
    const response = await $fetch(target, {
      method: 'POST',
      headers: {
        'Content-Type': contentType,
        ...(getCookie(event, 'aniglow_backend_token')
          ? { Authorization: `Bearer ${getCookie(event, 'aniglow_backend_token')}` }
          : {}),
      },
      body: rawBody,
      ignoreResponseError: false,
      timeout: 60000,
    })

    return response
  } catch (error: any) {
    if (error.data !== undefined || error.status || error.statusCode) {
      setResponseStatus(event, error.status || error.statusCode || 500)
      return error.data
    }
    console.warn(`[Upload Proxy] 后端不可用: ${error.message}`)
    return {
      success: false,
      message: '图片上传服务暂时不可用，请稍后再试',
      data: null,
    }
  }
})
```

> Nitro 文件路由中，`server/api/communities/upload/images.post.ts` 比 catch-all `server/api/[...].ts` 更具体，会优先匹配 `POST /api/communities/upload/images`。

- [ ] **Step 2: Commit**

```bash
git add server/api/communities/upload/images.post.ts
git commit -m "feat: add dedicated Nitro route for multipart image upload proxy

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: 前端 — 更新 useCommunity composable

**Files:**
- Modify: `composables/useCommunity.ts`

- [ ] **Step 1: CommunityPostDto 类型添加 images 字段**

在 `coverImage?: string` 行之后添加：

```typescript
  images?: string[]
```

- [ ] **Step 2: createPost 函数签名更新**

将：

```typescript
  const createPost = async (slug: string, data: { title: string; content: string; coverImage?: string }) => {
```

改为：

```typescript
  const createPost = async (slug: string, data: { title: string; content: string; coverImage?: string; images?: string[] }) => {
```

- [ ] **Step 3: 新增 uploadImages 函数**

在 `createPost` 函数之前插入：

```typescript
  const uploadImages = async (files: File[]): Promise<string[]> => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再上传')

    const formData = new FormData()
    files.forEach(f => formData.append('files', f))

    const res = await $fetch<ApiResponse<string[]>>(api('/communities/upload/images'), {
      method: 'POST',
      body: formData,
    })
    if (res?.success === false || !res?.data) {
      throw new Error(res?.message || '图片上传失败，请稍后再试')
    }
    return res.data
  }
```

- [ ] **Step 4: 导出 uploadImages**

在 return 对象中，`createPost,` 之前添加：

```typescript
    uploadImages,
```

- [ ] **Step 5: Commit**

```bash
git add composables/useCommunity.ts
git commit -m "feat: add uploadImages function and images field to community types

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 7: 前端 — 发帖弹窗添加图片选择器

**Files:**
- Modify: `pages/community/[slug].vue`

- [ ] **Step 1: 添加图片相关响应式状态**

在 `<script setup>` 区块，`const submitting = ref(false)` 之后添加：

```typescript
const draftImages = ref<{ file: File; preview: string; uploading: boolean; url?: string; error?: string }[]>([])
const MAX_IMAGES = 9
```

- [ ] **Step 2: 添加图片选择和处理函数**

在 `openPostBox` 函数之后插入：

```typescript
function handleImageSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input?.files
  if (!files || files.length === 0) return

  const remaining = MAX_IMAGES - draftImages.value.length
  const toAdd = Array.from(files).slice(0, remaining)

  toAdd.forEach(file => {
    if (!file.type.match(/^image\/(jpeg|png|gif|webp)$/)) {
      return
    }
    const item = {
      file,
      preview: URL.createObjectURL(file),
      uploading: true,
      url: undefined as string | undefined,
      error: undefined as string | undefined,
    }
    draftImages.value.push(item)
    uploadSingleImage(item)
  })
  input.value = ''
}

async function uploadSingleImage(item: typeof draftImages.value[number]) {
  try {
    const { uploadImages } = useCommunity()
    const urls = await uploadImages([item.file])
    item.url = urls[0]
    item.uploading = false
  } catch (e: any) {
    item.error = e?.message || '上传失败'
    item.uploading = false
  }
}

function removeDraftImage(index: number) {
  const item = draftImages.value[index]
  if (item?.preview) URL.revokeObjectURL(item.preview)
  draftImages.value.splice(index, 1)
}
```

- [ ] **Step 3: 修改提交函数 — 携带 images**

在 `submitPost` 函数中，将：

```typescript
    const post = await createPost(community.value.slug, {
      title: draftTitle.value.trim(),
      content: draftContent.value.trim(),
      coverImage: community.value.coverImage,
    })
```

改为：

```typescript
    const uploadedUrls = draftImages.value
      .filter(img => img.url)
      .map(img => img.url!)
    const post = await createPost(community.value.slug, {
      title: draftTitle.value.trim(),
      content: draftContent.value.trim(),
      coverImage: community.value.coverImage,
      ...(uploadedUrls.length > 0 ? { images: uploadedUrls } : {}),
    })
```

并在 `submitPost` 成功后（`showPostBox.value = false` 之前），清理图片：

```typescript
    draftImages.value.forEach(img => {
      if (img.preview) URL.revokeObjectURL(img.preview)
    })
    draftImages.value = []
```

- [ ] **Step 4: 修改模板 — 在发帖弹窗中添加图片选择区域**

在 textarea 和提交错误信息之间，插入图片选择区域。在 `<textarea ... />` 之后、`<p v-if="submitError" ...>` 之前插入：

```html
            <!-- 图片选择区域 -->
            <div class="mt-3 flex flex-wrap gap-2">
              <div
                v-for="(img, idx) in draftImages"
                :key="idx"
                class="relative w-20 h-20 rounded-2xl overflow-hidden shrink-0"
              >
                <img :src="img.preview" class="w-full h-full object-cover" />
                <div v-if="img.uploading" class="absolute inset-0 bg-black/40 flex items-center justify-center">
                  <span class="w-5 h-5 border-2 border-white/60 border-t-white rounded-full animate-spin"></span>
                </div>
                <div v-if="img.error" class="absolute inset-0 bg-sakura/80 flex items-center justify-center text-white text-[10px] font-bold text-center leading-tight p-1">
                  {{ img.error }}
                </div>
                <button
                  class="absolute top-1 right-1 w-5 h-5 rounded-full bg-black/50 text-white flex items-center justify-center text-xs hover:bg-black/70"
                  @click="removeDraftImage(idx)"
                >×</button>
              </div>
              <label
                v-if="draftImages.length < MAX_IMAGES"
                class="w-20 h-20 rounded-2xl border-2 border-dashed border-firefly/30 flex items-center justify-center cursor-pointer hover:border-firefly/60 transition shrink-0"
              >
                <span class="text-firefly/50 text-2xl font-light">+</span>
                <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple class="hidden" @change="handleImageSelect" />
              </label>
            </div>
            <p v-if="draftImages.length > 0" class="mt-1.5 text-[11px] text-gray-400 font-bold">
              已选 {{ draftImages.length }}/{{ MAX_IMAGES }} 张 · 单张不超过 5MB
            </p>
```

- [ ] **Step 5: 修改帖子卡片 — 添加底部图片预览条**

在当前帖子卡片的 `<p class="text-sm text-gray-500 leading-6 line-clamp-2">{{ post.content }}</p>` 之后，作者信息之前，插入：

```html
                  <!-- 图片预览条 -->
                  <div v-if="post.images && post.images.length" class="flex gap-1.5 mt-3">
                    <img
                      v-for="(imgUrl, imgIdx) in post.images.slice(0, 4)"
                      :key="imgIdx"
                      :src="imgUrl"
                      class="w-14 h-14 rounded-xl object-cover shrink-0"
                      loading="lazy"
                    />
                    <div
                      v-if="post.images.length > 4"
                      class="w-14 h-14 rounded-xl bg-black/40 flex items-center justify-center text-xs font-black text-white/70 shrink-0"
                    >
                      +{{ post.images.length - 4 }}
                    </div>
                  </div>
```

- [ ] **Step 6: Commit**

```bash
git add pages/community/\[slug\].vue
git commit -m "feat: add image picker to post modal and preview bar to post cards

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 8: 前端 — 帖子详情页图片网格 + 灯箱

**Files:**
- Modify: `pages/community/[slug]/posts/[id].vue`

- [ ] **Step 1: 添加灯箱响应式状态**

在 `<script setup>` 区块，`const deletingReply = ref(false)` 之后添加：

```typescript
const lightboxOpen = ref(false)
const lightboxIndex = ref(0)
```

- [ ] **Step 2: 添加灯箱控制函数**

在 `formatTime` 函数之前插入：

```typescript
function openLightbox(index: number) {
  lightboxIndex.value = index
  lightboxOpen.value = true
  document.body.style.overflow = 'hidden'
}

function closeLightbox() {
  lightboxOpen.value = false
  document.body.style.overflow = ''
}

function prevImage() {
  if (!post.value?.images) return
  lightboxIndex.value = (lightboxIndex.value - 1 + post.value.images.length) % post.value.images.length
}

function nextImage() {
  if (!post.value?.images) return
  lightboxIndex.value = (lightboxIndex.value + 1) % post.value.images.length
}

function gridCols(count: number): string {
  if (count === 1) return 'grid-cols-1'
  if (count === 2 || count === 4) return 'grid-cols-2'
  return 'grid-cols-3'
}

function onLightboxKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') closeLightbox()
  if (e.key === 'ArrowLeft') prevImage()
  if (e.key === 'ArrowRight') nextImage()
}
```

- [ ] **Step 3: 修改模板 — 在帖子正文后添加图片网格**

在 `<p class="whitespace-pre-wrap text-gray-600 leading-8">{{ post.content }}</p>` 之后、action bar `<div class="flex flex-wrap items-center gap-3 mt-8">` 之前插入：

```html
          <!-- 图片网格 -->
          <div v-if="post.images && post.images.length" class="mt-6">
            <div :class="`grid ${gridCols(post.images.length)} gap-1.5 max-w-2xl`">
              <div
                v-for="(imgUrl, imgIdx) in post.images"
                :key="imgIdx"
                class="overflow-hidden rounded-2xl cursor-pointer"
                :class="post.images.length === 1 ? '' : 'aspect-square'"
                @click="openLightbox(imgIdx)"
              >
                <img
                  :src="imgUrl"
                  :alt="`图片 ${imgIdx + 1}`"
                  class="w-full h-full object-cover hover:scale-105 transition duration-300"
                  loading="lazy"
                />
              </div>
            </div>
          </div>
```

- [ ] **Step 4: 修改模板 — 在文章闭合后添加灯箱**

在 `</article>` 闭合标签之后（即帖子 article 和回复 section 之间）插入灯箱组件：

```html
      <!-- Lightbox 灯箱 -->
      <Teleport to="body">
        <Transition enter-active-class="transition duration-200" enter-from-class="opacity-0" leave-active-class="transition duration-150" leave-to-class="opacity-0">
          <div
            v-if="lightboxOpen && post?.images"
            class="fixed inset-0 z-[200] bg-black/95 flex items-center justify-center"
            @click.self="closeLightbox"
            @keydown="onLightboxKeydown"
            tabindex="0"
          >
            <button class="absolute top-4 right-4 text-white/70 hover:text-white text-2xl z-10" @click="closeLightbox">✕</button>
            <button
              v-if="post.images.length > 1"
              class="absolute left-4 top-1/2 -translate-y-1/2 text-white/70 hover:text-white text-3xl z-10"
              @click.stop="prevImage"
            >‹</button>
            <button
              v-if="post.images.length > 1"
              class="absolute right-4 top-1/2 -translate-y-1/2 text-white/70 hover:text-white text-3xl z-10"
              @click.stop="nextImage"
            >›</button>
            <img
              :src="post.images[lightboxIndex]"
              class="max-w-full max-h-[90vh] object-contain select-none"
              :alt="`图片 ${lightboxIndex + 1}`"
              @click.stop
            />
            <div v-if="post.images.length > 1" class="absolute bottom-4 text-white/50 text-sm">
              {{ lightboxIndex + 1 }} / {{ post.images.length }}
            </div>
          </div>
        </Transition>
      </Teleport>
```

- [ ] **Step 5: Commit**

```bash
git add pages/community/\[slug\]/posts/\[id\].vue
git commit -m "feat: add image grid and lightbox to post detail page

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 9: 端到端验证

- [ ] **Step 1: 编译后端**

```bash
cd aniglow-backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 启动后端**

```bash
cd aniglow-backend && mvn spring-boot:run -q &
```

等待启动完成（约10-15秒），检查日志无错误。

- [ ] **Step 3: 测试图片上传端点**

```bash
# 确认后端运行中
curl -s http://localhost:8081/api/communities | head -c 100
```

Expected: 返回社区列表 JSON。

- [ ] **Step 4: 启动前端**

```bash
cd /Users/mac/Desktop/四季番约 && npm run dev &
```

- [ ] **Step 5: 验证网站正常访问**

浏览器访问 `http://www.mjj520.top:3001/community` 确认：
- 社区广场页面正常加载
- 进入任意社区盒子，帖子列表正常
- 点击"发布帖子"，弹窗中出现图片选择 + 按钮
- 选择图片后可看到缩略图预览
- 发布带图帖子后，帖子详情页显示图片网格
- 点击图片可打开灯箱，左右切换、关闭正常
- 纯文字发帖不受影响

- [ ] **Step 6: Commit（如有微调）**

如有微调，提交。否则此任务仅验证，不产生新 commit。
