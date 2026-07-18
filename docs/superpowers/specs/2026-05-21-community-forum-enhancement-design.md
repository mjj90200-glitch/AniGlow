# 社区论坛功能完善 — 设计文档

## 概述

在现有社区盒子功能基础上，完善为类似百度贴吧的论坛模式。核心增强：扩充预设社区盒子、增加删帖/删回复功能、分页加载、UI 交互优化。

## 技术栈

- **前端**: Nuxt 3 + Vue 3 + TypeScript + Tailwind CSS（玻璃拟态风格，复用现有设计系统）
- **后端**: Spring Boot 3 + JPA + MySQL（复用现有架构）
- **无新增依赖**

## 现有功能盘点

| 功能 | 状态 |
|------|------|
| 社区盒子列表（按分类分组） | 已完成 |
| 社区详情页（封面 + 简介 + 帖子列表） | 已完成 |
| 发帖 | 已完成 |
| 帖子详情页 | 已完成 |
| 回复帖子（评论） | 已完成 |
| 点赞帖子 / 点赞回复 | 已完成 |
| 排序（最新回复 / 最多点亮） | 已完成 |
| 删帖 | **缺失** |
| 删回复 | **缺失** |
| 分页加载（后端已有，前端未接） | **缺失** |
| 社区盒子数量（仅 6 个） | **不足** |

## 权限模型

- 帖子删除：作者本人 or 该社区的吧主（创建者）
- 回复删除：回复作者本人 or 帖子作者 or 该社区的吧主
- 社区盒子：仅管理员预设，普通用户不可创建

## 数据模型改动

### Community 表

新增一个字段：

```
creator_id  BIGINT  FK → users.id  社区创建者/吧主
```

现有字段不变。`creator_id` 为 NULL 表示系统预设盒子（由管理员维护）。

### CommunityPost 表

新增软删除标记：

```
is_deleted  BOOLEAN  DEFAULT FALSE  软删除
```

查询帖子时过滤 `is_deleted = FALSE`。

### CommunityReply 表

新增软删除标记：

```
is_deleted  BOOLEAN  DEFAULT FALSE  软删除
```

查询回复时过滤 `is_deleted = FALSE`。

## API 设计

### 新增端点

| 方法 | 路径 | 说明 | 鉴权 | 权限 |
|------|------|------|------|------|
| DELETE | `/communities/posts/{postId}` | 软删除帖子 | 需要 | 作者 or 吧主 |
| DELETE | `/communities/replies/{replyId}` | 软删除回复 | 需要 | 回复作者 or 帖子作者 or 吧主 |

### 现有端点修改

| 方法 | 路径 | 改动 |
|------|------|------|
| GET | `/communities/{slug}/posts` | 返回分页元数据（total, page, size）；过滤软删除帖子 |
| GET | `/communities/posts/{postId}/replies` | 过滤 `is_deleted = FALSE` |
| GET | `/communities` | postCount 不计算软删除帖子 |

### 软删除时的计数器维护

- 删除帖子：`community.postCount -= 1`，帖子下所有未删除回复数同步扣减
- 删除回复：`post.replyCount -= 1`
- 热度分（heatScore）不回退，保持历史积累

## 前端改动

### 文件清单

```
pages/community.vue                     ← 社区盒子列表（展示更多盒子）
pages/community/[slug].vue              ← 社区详情 + 帖子列表（增加删帖、分页）
pages/community/[slug]/posts/[id].vue   ← 帖子详情 + 回复（增加删回复）
composables/useCommunity.ts             ← 新增 deletePost / deleteReply
types/anime.ts                          ← 类型补充（如有需要）
```

### 交互细节

1. **删帖**：帖子卡片右上角三点菜单 → "删除" → 确认弹窗 → 执行软删除 → 刷新列表
2. **删回复**：回复右侧三点菜单 → "删除" → 确认 → 执行删除 → 刷新回复列表
3. **分页**：帖子列表底部"加载更多"按钮，点击追加下一页数据
4. **权限 UI**：删除按钮仅对作者本人或吧主可见

### 复用现有设计系统

- 社区盒子卡片：复用 `glass-card` + 封面图 + 标签样式
- 帖子列表：复用现有 `glass-card` 帖子卡片
- 发帖/回复弹窗：复用现有 `glass-card-cream` + 毛玻璃遮罩
- 删除确认弹窗：新建小型确认弹窗组件，复用玻璃风格

## 预设社区盒子扩充

从 6 个 → 15 个：

| 分类 | slug | 名称 | 关联番剧 |
|------|------|------|---------|
| 热门番剧 | frieren | 芙莉莲社区 | 葬送的芙莉莲 |
| 热门番剧 | naruto | 火影忍者社区 | 火影忍者 |
| 热门番剧 | jujutsu-kaisen | 咒术回战社区 | 咒术回战 |
| 热门番剧 | kimetsu | 鬼灭之刃社区 | 鬼灭之刃 |
| 热门番剧 | shingeki | 进击的巨人社区 | 进击的巨人 |
| 热门番剧 | one-piece | 海贼王社区 | 海贼王 |
| 游戏跨界 | delta-force | 三角洲行动社区 | - |
| 游戏跨界 | genshin | 原神社区 | - |
| 创作交流 | new-anime | 新番安利盒 | - |
| 创作交流 | aigc-tea | AIGC 角色茶会 | - |
| 创作交流 | doujin | 同人创作阁 | - |
| 创作交流 | light-novel | 轻小说讨论室 | - |
| 日常闲聊 | daily-chat | 番舍闲聊角 | - |
| 日常闲聊 | seiyuu | 声优应援会 | - |
| 日常闲聊 | anime-music | 动漫音乐厅 | - |

所有新盒子 `creator_id = NULL`（系统预设）。

## 实施步骤

1. **数据库迁移** — 新增 `creator_id`, `is_deleted` 字段
2. **后端实体 + 仓库** — 更新 Entity，修改查询过滤软删除
3. **后端 API** — 新增 DELETE 端点，修改列表端点返回分页元数据
4. **种子数据** — DataInitializer 更新为 15 个社区盒子
5. **前端 Composable** — 新增 `deletePost`, `deleteReply` 方法
6. **前端页面** — 社区列表页、社区详情页、帖子详情页增加删除和分页
7. **验证测试** — 全流程：发帖 → 回复 → 删除回复 → 删帖
