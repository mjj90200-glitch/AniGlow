# 社区图文发帖功能 — 设计文档

**日期**: 2026-05-22
**状态**: 已确认

## 概述

为社区发帖功能新增图片附件能力，类似微信朋友圈：用户发帖时可选择附带 1-9 张图片，图片展示在帖子正文下方。也可纯文字发帖（向后兼容）。

## 设计决策

### 方案选择：图片附件 + 九宫格展示

- 帖子内容保持纯文本，新增 `images` 字段存储图片URL数组
- 图片上传到后端本地文件系统，WebP 压缩
- 帖子详情页以自适应网格展示（1张→大图，2-3张→并排，4张→四宫格，5-9张→九宫格）
- 点击图片进入 Lightbox 灯箱预览，支持左右切换

## 数据结构变更

### CommunityPost 实体

```java
// 新增字段
@Column(columnDefinition = "TEXT")
private String images; // JSON数组: ["url1","url2",...]
```

### CommunityPostRequest

```java
private List<String> images; // 图片URL数组，可选
```

### CommunityPostDto

```java
private List<String> images; // 透传给前端
```

## API 变更

### 新增端点

```
POST /api/communities/upload/images
Content-Type: multipart/form-data
Auth: 需要登录

Request:  files (MultipartFile[], 最多9张)
Response: ["/api/images/2026/05/uuid.webp", ...]
```

### 现有端点修改

`POST /api/communities/{slug}/posts` — 请求体新增 `images` 字段

## 图片存储

- **路径**: `~/aniglow-data/images/{yyyy}/{MM}/{uuid}.webp`
- **访问**: 通过 Spring Boot 静态资源映射或 ResourceHandler 提供
- **代理**: Nuxt Nitro catch-all 代理 `/api/images/**` 到后端
- **处理**: 前端上传后后端即时压缩至 WebP（长边≤1920px，质量80%），单张约 300KB
- **限制**: 1-9张，单张原始≤5MB，仅允许 JPG/PNG/GIF/WebP

## 前端变更

### 文件清单

| 文件 | 改动 |
|------|------|
| `composables/useCommunity.ts` | 新增 `uploadImages()` 函数，类型定义加 `images` |
| `pages/community/[slug].vue` | 发帖弹窗加图片选择器；帖子卡片加底部图片预览条 |
| `pages/community/[slug]/posts/[id].vue` | 帖子详情加自适应网格 + Lightbox 灯箱组件 |

### 发帖流程

1. 用户点击"发布帖子" → 弹窗出现
2. 填写标题、正文
3. 点击 + 按钮选择图片（accept="image/jpeg,image/png,image/gif,image/webp"）
4. 选图后即时上传到后端，获得 URL 数组
5. 弹窗内显示缩略图预览，可删除单张或继续添加
6. 点击发布 → 提交时携带 images URL 数组

### 帖子卡片

- 有图片时：正文下方显示一排图片预览条（最多4张 + "+N"）
- 无图片时：与现有纯文字卡片一致

### 帖子详情

- 正文下方渲染自适应图片网格
- 点击图片 → 全屏 Lightbox（黑色遮罩 + 左右箭头 + 页码指示器）
- 点击遮罩空白处或关闭按钮退出

### 图片上传状态处理

- 上传中：图片占位区域显示加载动画
- 上传失败：对应图片显示错误状态，可重试
- 超过9张：+ 按钮隐藏或禁用

## 兼容性

- `images` 为空或 null → 纯文字帖子，现有数据和功能不受影响
- `coverImage` 字段保留不动（用于帖子的可选封面装饰）
- 旧帖子无需数据迁移

## 后续扩展点

- 图片审核（如需要）
- 迁移至 OSS/CDN（存储路径兼容）
- 支持 GIF 动图播放
