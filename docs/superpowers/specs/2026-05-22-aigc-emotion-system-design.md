# AIGC 情感系统设计方案

## 概述

为 AIGC 角色对话模块引入情感系统，使角色能根据自身人设理解用户对话并做出情感反应，同时动态切换角色图片。

## 情感定义

| 情感标签 | 含义 | 典型触发场景 |
|---------|------|-------------|
| `happy` | 高兴/开心/喜悦 | 被夸奖、聊到喜欢的话题、用户表达善意 |
| `normal` | 正常/平静/默认 | 普通寒暄、日常对话、中立问题 |
| `complex` | 复杂情绪 | 害羞、纠结、傲娇、伤感、被戳中内心、不知所措 |

## 图片规范

```
/public/images/{code}.png           → 正常（默认，已有）
/public/images/{code}_happy.png     → 高兴
/public/images/{code}_complex.png   → 复杂情绪
```

- `_happy` 或 `_complex` 图片不存在时，前端自动降级使用默认图（`{code}.png`）
- 用户自行准备图片，每角色最多新增 2 张，共 9 角色 × 3 种 = 27 张

---

## 后端改动

### 文件：`AgentService.java`

#### Prompt 改造

系统 prompt 末尾增加情感指令：

```
【回复格式要求】
你的每次回复必须以情感标签开头，格式为 [happy]、[normal] 或 [complex]。
- [happy]：当对话让你感到开心、被认可、温暖、喜悦时使用
- [normal]：日常聊天、中性话题、简单回答时使用
- [complex]：当对话让你害羞、纠结、伤感、不知所措、被戳中内心时使用

请根据角色性格和当前对话内容，选择最恰当的情感标签。
```

用户消息末尾改为：
```
请用角色语气简短回复（50字以内，必须以[情感]标签开头）。
```

#### 情感标签解析

新增方法提取并剥离情感标签：
- 正则匹配 `^\[(happy|normal|complex)\](.*)`
- 不匹配或匹配失败时默认 `normal`

#### 流式 SSE (chatStream)

1. 缓冲前几个 token 直到检测到 `]`
2. 解析出情感标签，发送独立 SSE 事件：
   ```
   event: emotion
   data: happy
   ```
3. 后续 token 剥离标签前缀后按原有逻辑流式发送
4. 如果没有匹配到情感标签，发送默认 `emotion: normal`

#### 阻塞式 (chat)

- 解析回复中的情感标签，填充 `AgentChatResponse.emotion`
- `reply` 字段返回剥离标签后的纯文本

#### 常见回复缓存 (resolveCommonCachedReply)

- 缓存回复也需包含情感标签前缀
- 或保持现状不做修改（短期可接受）

### 文件：`AgentChatResponse.java`

新增字段：
```java
private String emotion;  // "happy" / "normal" / "complex"
```

### 文件：`AgentController.java`

无需改动，SSE 事件由 Service 层处理。

---

## 前端改动

### 文件：`pages/agent.vue`

#### 新增情感状态

```typescript
const currentEmotion = ref<'happy' | 'normal' | 'complex'>('normal')
```

#### 动态图片计算

```typescript
const portraitSrc = computed(() => {
  if (!selectedRole.value) return ''
  const code = selectedRole.value
  if (currentEmotion.value === 'happy') return `/images/${code}_happy.png`
  if (currentEmotion.value === 'complex') return `/images/${code}_complex.png`
  return `/images/${code}.png`
})
```

#### SSE 事件处理

流式解析中新增 `emotion` 事件处理：
```typescript
if (eventName === 'emotion' && payload) {
  currentEmotion.value = payload as 'happy' | 'normal' | 'complex'
}
```

#### 图片加载降级

`<img>` 的 `@error` 处理：情感图加载失败时回退到 `/images/{code}.png`。

#### 角色切换时重置

`selectRole()` 中重置 `currentEmotion.value = 'normal'`。

#### 视觉过渡

复用现有 `scene-crossfade` transition，切换情感图片时有交叉淡入淡出效果。

---

## 影响范围

| 文件 | 改动类型 |
|------|---------|
| `AgentService.java` | Prompt 改造 + 情感解析 + SSE event 新增 |
| `AgentChatResponse.java` | 新增 `emotion` 字段 |
| `pages/agent.vue` | 情感状态 + 动态图片 + SSE 解析 + 降级处理 |
| `server/api/agent/chat/stream.post.ts` | 无需改动（原始字节透传） |
| 数据库 | 无需改动 |
| 静态资源 | 用户新增 `_happy.png` 和 `_complex.png` 图片 |

## Galgame 式对话窗口

将当前微信气泡式对话界面重构为 Galgame 视觉小说风格。

### 桌面端布局

```
┌──────────────────────────────────────────┐
│  [萤火图标]                          ✦   │  ← 右上角会员入口（小图标）
│                                          │
│         角色立绘（全屏背景）               │  ← 根据 currentEmotion 切换
│                                          │
│  ┌──────────────────────────────────┐    │
│  │  玛奇玛                           │    │  ← 角色名标签
│  │  "真是个乖孩子呢。"                │    │  ← 仅显示当前这一条对话
│  └──────────────────────────────────┘    │
│  [输入框                          ][发送] │  ← 点击画面后滑入，失焦/发送后隐藏
│                                    [👥]  │  ← 左下角悬浮按钮 → 角色选择面板
└──────────────────────────────────────────┘
```

### 移动端布局

```
┌──────────────┐
│ [萤火]       │  ← 右上角
│              │
│   角色立绘    │  ← 上半屏
│              │
├──────────────┤
│  玛奇玛      │
│  "乖孩子。"  │
├──────────────┤
│ [输入...] ✈  │  ← 点击后出现
│        [👥]  │
└──────────────┘
```

### 交互规则

- **沉浸模式**：页面加载后全屏显示角色立绘 + 底部对话框，无历史消息堆积
- **输入栏**：平时隐藏，点击画面任意位置从底部滑入，发送消息或点击外部后滑出
- **对话框**：仅展示角色最新一条回复，不显示历史消息列表
- **角色切换**：左下角悬浮 `👥` 按钮，点击弹出角色选择面板（搜索栏 + 角色列表 + 会员状态入口）
- **情感切换**：收到 SSE `emotion` 事件后，立绘交叉淡入淡出切换 0.5s，对话框文字同步更新
- **会员入口**：右上角半透明萤火图标，点击弹出会员状态浮窗（含开通按钮），不再占用页面顶部横幅

### 移除的旧 UI 元素

- 左侧栏（角色列表、搜索、头像卡片）
- 页面顶部标题横幅（"AIGC 角色对话"）
- 顶部会员状态横条
- 消息历史气泡列表
- 移动端底部角色横条

### 新增 UI 元素

- 全屏角色立绘背景层
- 底部半透明对话框（Galgame 风格文字框）
- 左下悬浮角色切换按钮
- 右上萤火会员图标
- 角色选择弹出面板
- 输入栏滑入/滑出动画

### 历史对话处理

当前仍在内存中保留完整历史（`chatHistories`），发送给后端的对话上下文不变。只是 UI 层不展示历史气泡，用户看到的是 Galgame 式的单句推进体验。

---

## 降级与容错

1. LLM 未输出情感标签 → 默认 `normal`，不影响正常对话
2. 情感图片不存在 → 前端 `@error` 回退默认图
3. SSE emotion 事件丢失 → 前端保持上一次情感状态，初始为 `normal`
4. 阻塞式 API 无 emotion 字段 → 前端按 `normal` 处理
