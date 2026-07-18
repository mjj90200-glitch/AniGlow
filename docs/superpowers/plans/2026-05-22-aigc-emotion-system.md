# AIGC 情感系统 + Galgame 对话窗口 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 AIGC 角色对话模块引入情感系统（3种情感 + 动态图片切换）并将 UI 重构为 Galgame 视觉小说风格全屏对话窗口。

**Architecture:** LLM 在回复中输出情感标签 `[happy]`/`[normal]`/`[complex]`，后端解析标签并剥离，SSE 流新增 `emotion` 事件；前端根据情感动态切换角色立绘图片，对话框只展示当前一条回复。

**Tech Stack:** Java 17 + Spring Boot (backend), Nuxt 3 + Vue 3 + TypeScript + Tailwind CSS (frontend), Volcengine Ark (LLM API)

---

### Task 1: AgentChatResponse 新增 emotion 字段

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/dto/agent/AgentChatResponse.java`

- [ ] **Step 1: 添加 emotion 字段**

```java
package com.aniglow.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {
    private String role;
    private String roleName;
    private String reply;
    private String emotion;      // "happy" / "normal" / "complex"
    private Boolean cached;
    private Boolean rateLimited;
    private Integer quotaLimit;
    private Integer quotaRemaining;
    private String quotaResetAt;
}
```

- [ ] **Step 2: 编译验证**

Run: `cd aniglow-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add aniglow-backend/src/main/java/com/aniglow/dto/agent/AgentChatResponse.java
git commit -m "feat: add emotion field to AgentChatResponse"
```

---

### Task 2: AgentService - Prompt 改造 + 情感解析方法

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/service/AgentService.java`

- [ ] **Step 1: 在类的静态常量区添加情感 Pattern 和 Prompt 片段**

在 `DEFAULT_PROMPT_TEMPLATE` 之后（约第92行后），添加：

```java
    private static final java.util.regex.Pattern EMOTION_PATTERN =
            java.util.regex.Pattern.compile("^\\[(happy|normal|complex)\\](.*)", java.util.regex.Pattern.DOTALL);

    private static final String EMOTION_INSTRUCTION = """

            【回复格式要求】
            你的每次回复必须以情感标签开头，格式为 [happy]、[normal] 或 [complex]。
            - [happy]：当对话让你感到开心、被认可、温暖、喜悦时使用
            - [normal]：日常聊天、中性话题、简单回答时使用
            - [complex]：当对话让你害羞、纠结、伤感、不知所措、被戳中内心时使用

            请根据角色性格和当前对话内容，选择最恰当的情感标签。
            """;
```

- [ ] **Step 2: 修改 DEFAULT_PROMPT_TEMPLATE，末尾加入 EMOTION_INSTRUCTION**

将第86-92行的 `DEFAULT_PROMPT_TEMPLATE` 改为：

```java
    private static final String DEFAULT_PROMPT_TEMPLATE = """
            你是{{displayName}}({{code}})，来自《{{sourceTitle}}》。
            性格：{{personality}}
            说话风格：{{speechStyle}}
            常用表达：{{catchphrases}}
            {{extraPrompt}}
            {{emotionInstruction}}
            """;
```

- [ ] **Step 3: 修改 buildPersonaPrompt 方法，注入情感指令**

在 `buildPersonaPrompt` 方法（第694-708行）中，在最后的 `.trim()` 之前加入 `.replace("{{emotionInstruction}}", EMOTION_INSTRUCTION)`：

```java
    private String buildPersonaPrompt(AgentCharacter character) {
        String template = hasText(character.getPromptTemplate())
                ? character.getPromptTemplate()
                : DEFAULT_PROMPT_TEMPLATE;

        return template
                .replace("{{code}}", safe(character.getCode()))
                .replace("{{displayName}}", safe(character.getDisplayName()))
                .replace("{{sourceTitle}}", safe(character.getSourceTitle()))
                .replace("{{personality}}", safe(character.getPersonality()))
                .replace("{{speechStyle}}", safe(character.getSpeechStyle()))
                .replace("{{catchphrases}}", safe(character.getCatchphrases()))
                .replace("{{extraPrompt}}", safe(character.getExtraPrompt()))
                .replace("{{emotionInstruction}}", EMOTION_INSTRUCTION)
                .trim();
    }
```

注意：数据库角色的 `promptTemplate` 若未包含 `{{emotionInstruction}}` 占位符则不会注入。为确保数据库角色也能获得情感指令，需要在 `resolveCharacter` 或构建 persona 后追加指令。修改 `buildPersonaPrompt`：

```java
    private String buildPersonaPrompt(AgentCharacter character) {
        String template = hasText(character.getPromptTemplate())
                ? character.getPromptTemplate()
                : DEFAULT_PROMPT_TEMPLATE;

        String persona = template
                .replace("{{code}}", safe(character.getCode()))
                .replace("{{displayName}}", safe(character.getDisplayName()))
                .replace("{{sourceTitle}}", safe(character.getSourceTitle()))
                .replace("{{personality}}", safe(character.getPersonality()))
                .replace("{{speechStyle}}", safe(character.getSpeechStyle()))
                .replace("{{catchphrases}}", safe(character.getCatchphrases()))
                .replace("{{extraPrompt}}", safe(character.getExtraPrompt()));

        // 如果自定义模板没有包含 {{emotionInstruction}}，追加情感指令
        if (!template.contains("{{emotionInstruction}}")) {
            persona += EMOTION_INSTRUCTION;
        } else {
            persona = persona.replace("{{emotionInstruction}}", EMOTION_INSTRUCTION);
        }

        return persona.trim();
    }
```

- [ ] **Step 4: 修改用户消息后缀，要求情感标签**

在 `buildMessages` 方法（第616-622行）中，将末句从 `"请用角色语气简短回复（50字以内）。"` 改为 `"请用角色语气简短回复（50字以内，必须以[情感]标签开头）。"`：

```java
    private List<Map<String, Object>> buildMessages(String persona, String um, List<Map<String, String>> hist) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        msgs.add(Map.of("role","system","content",persona));
        for (Map<String, String> e : hist) msgs.add(Map.of("role",e.get("role"),"content",e.get("content")));
        msgs.add(Map.of("role","user","content", um + "\n请用角色语气简短回复（50字以内，必须以[情感]标签开头）。"));
        return msgs;
    }
```

- [ ] **Step 5: 添加情感解析 record 和方法**

在 `QuotaResult` record 之后（约第580行后），添加：

```java
    private record ParsedMessage(String emotion, String content) {}

    private ParsedMessage parseEmotion(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedMessage("normal", "");
        }
        java.util.regex.Matcher m = EMOTION_PATTERN.matcher(raw.trim());
        if (m.find()) {
            return new ParsedMessage(m.group(1), m.group(2).trim());
        }
        return new ParsedMessage("normal", raw.trim());
    }
```

- [ ] **Step 6: 编译验证**

Run: `cd aniglow-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add aniglow-backend/src/main/java/com/aniglow/service/AgentService.java
git commit -m "feat: add emotion instruction to prompt and emotion parsing"
```

---

### Task 3: AgentService - 阻塞式 chat 方法集成情感解析

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/service/AgentService.java`

- [ ] **Step 1: 修改 buildResponse 方法，增加 emotion 参数**

将 `buildResponse` 方法（第518-536行）改为接受 `String emotion` 参数并在 builder 中设置：

```java
    private AgentChatResponse buildResponse(
            String role,
            String roleName,
            String reply,
            String emotion,
            boolean cached,
            boolean rateLimited,
            QuotaResult quota
    ) {
        return AgentChatResponse.builder()
                .role(role)
                .roleName(roleName)
                .reply(reply)
                .emotion(emotion)
                .cached(cached)
                .rateLimited(rateLimited)
                .quotaLimit(quota.limit())
                .quotaRemaining(quota.remaining())
                .quotaResetAt(quota.resetAt().format(QUOTA_RESET_FORMAT))
                .build();
    }
```

- [ ] **Step 2: 修改 chat 方法中所有 buildResponse 调用点**

在 `chat` 方法（第139-172行）中：

将第145-146行的空 API key 返回改为：
```java
            return AgentChatResponse.builder().role(role).roleName(roleName)
                    .reply("（" + roleName + "正看着你...）").emotion("normal").build();
```

将第155-156行的缓存回复返回改为解析情感：
```java
            if (cachedReply.isPresent()) {
                String reply = cachedReply.get();
                ParsedMessage parsed = parseEmotion(reply);
                saveHistory(uid, role, req.getMessage(), parsed.content());
                QuotaResult quota = peekQuota(uid);
                return buildResponse(role, roleName, parsed.content(), parsed.emotion(), true, false, quota);
            }
```

将第160-161行的额度限制返回改为：
```java
            QuotaResult quota = consumeQuota(uid);
            if (quota.limited()) {
                return buildResponse(role, roleName, buildQuotaReply(roleName, quota), "normal", false, true, quota);
            }
```

将第163-165行的正常回复改为解析情感：
```java
            String reply = callArkApi(character, persona, req.getMessage(), hist, false);
            ParsedMessage parsed = parseEmotion(reply);
            saveHistory(uid, role, req.getMessage(), parsed.content());
            return buildResponse(role, roleName, parsed.content(), parsed.emotion(), false, false, quota);
```

将第169-170行的异常返回改为：
```java
            return AgentChatResponse.builder().role(role).roleName(roleName)
                    .reply("抱歉，暂时无法回应...").emotion("normal").build();
```

- [ ] **Step 3: 编译验证**

Run: `cd aniglow-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add aniglow-backend/src/main/java/com/aniglow/service/AgentService.java
git commit -m "feat: integrate emotion parsing into blocking chat"
```

---

### Task 4: AgentService - SSE 流式新增 emotion 事件

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/service/AgentService.java`

- [ ] **Step 1: 修改 SSE 流式 token 处理逻辑**

在 `chatStream` 方法中（约第281-306行），修改 token 解析部分。将：

```java
                                Object content = delta.get("content");
                                if (content != null && !content.toString().isEmpty()) {
                                    String token = content.toString();
                                    fullReply.append(token);
                                    emitter.send(SseEmitter.event().name("token").data(token));
                                }
```

替换为带情感缓冲的逻辑：

```java
                                Object content = delta.get("content");
                                if (content != null && !content.toString().isEmpty()) {
                                    String token = content.toString();

                                    if (!emotionSent) {
                                        emotionBuffer.append(token);
                                        java.util.regex.Matcher em = EMOTION_PATTERN.matcher(emotionBuffer.toString());
                                        if (em.find()) {
                                            emitter.send(SseEmitter.event().name("emotion").data(em.group(1)));
                                            emotionSent = true;
                                            String rest = em.group(2);
                                            if (!rest.isEmpty()) {
                                                fullReply.append(rest);
                                                emitter.send(SseEmitter.event().name("token").data(rest));
                                            }
                                        }
                                    } else {
                                        fullReply.append(token);
                                        emitter.send(SseEmitter.event().name("token").data(token));
                                    }
                                }
```

- [ ] **Step 2: 在 SSE 流式方法开头声明变量**

在 `streamExecutor.execute(() -> {` 块内，`String uid` 变量声明之前（约第223行），添加：

```java
                StringBuilder emotionBuffer = new StringBuilder();
                boolean emotionSent = false;
```

完整上下文：
```java
        streamExecutor.execute(() -> {
            try {
                String uid = resolveUserId(req);
                List<Map<String, String>> hist = getHistory(uid, role);
                StringBuilder fullReply = new StringBuilder();
                StringBuilder emotionBuffer = new StringBuilder();
                boolean emotionSent = false;

                emitter.send(SseEmitter.event().name("role").data(roleName));
```

- [ ] **Step 3: 在流结束后处理未匹配情感的情况**

在 `while` 循环结束后、`if (fullReply.length() > 0)` 之前（约第309行），添加：

```java
                if (!emotionSent) {
                    emitter.send(SseEmitter.event().name("emotion").data("normal"));
                    String remaining = emotionBuffer.toString().trim();
                    if (!remaining.isEmpty()) {
                        fullReply.append(remaining);
                        emitter.send(SseEmitter.event().name("token").data(remaining));
                    }
                }
```

- [ ] **Step 4: 编译验证**

Run: `cd aniglow-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add aniglow-backend/src/main/java/com/aniglow/service/AgentService.java
git commit -m "feat: add emotion SSE event to streaming chat"
```

---

### Task 5: 后端编译完整验证

**Files:** 无改动，仅验证。

- [ ] **Step 1: 完整编译 + 打包验证**

Run: `cd aniglow-backend && mvn package -DskipTests -q`
Expected: BUILD SUCCESS

---

### Task 6: agent.vue - 模板重写为 Galgame 布局

**Files:**
- Modify: `pages/agent.vue`

- [ ] **Step 1: 重写整个 `<template>` 块**

将现有 template（第1-413行）替换为：

```vue
<template>
  <div class="galgame-stage" @click="onStageClick">
    <!-- ═══ 背景层：角色立绘 ═══ -->
    <Transition name="sprite-crossfade">
      <div v-if="selectedRole" :key="selectedRole + '-' + currentEmotion" class="sprite-layer">
        <img
          :src="portraitSrc"
          :alt="roleNames[selectedRole] || selectedRole"
          class="sprite-img"
          @error="onPortraitError"
        />
        <!-- 底部渐变遮罩，让对话框可读 -->
        <div class="sprite-gradient" />
      </div>
    </Transition>

    <!-- 无角色提示 -->
    <div v-if="!selectedRole" class="no-role-hint">
      <div class="no-role-icon"><Users class="w-12 h-12 text-gray-300" /></div>
      <p class="text-gray-400 text-sm mt-3">选择一个角色，开始对话</p>
    </div>

    <!-- ═══ 右上角：萤火会员图标 ═══ -->
    <button
      v-if="selectedRole"
      class="membership-icon-btn"
      @click.stop="toggleMembership"
    >
      <Crown
        class="w-5 h-5"
        :class="membershipStatus?.active ? 'text-firefly' : 'text-gray-400'"
      />
    </button>

    <!-- ═══ 会员状态浮窗 ═══ -->
    <Transition name="fade-scale">
      <div v-if="showMembershipPopover" class="membership-popover" @click.stop>
        <div class="membership-popover-content">
          <p class="text-sm font-bold text-gray-800">
            {{ membershipStatus?.active ? '萤火月卡生效中' : '普通番舍成员' }}
          </p>
          <p class="text-xs text-gray-500 mt-1">
            每小时 {{ membershipStatus?.quotaLimitPerHour || 10 }} 次对话
            <span v-if="membershipStatus?.active && membershipStatus?.expiresAt">
              · {{ formatMembershipTime(membershipStatus.expiresAt) }} 到期
            </span>
          </p>
          <button
            v-if="!membershipStatus?.active"
            class="mt-3 w-full rounded-full bg-firefly px-4 py-2 text-xs font-bold text-white"
            @click="openMembershipModal(); showMembershipPopover = false"
          >
            开通萤火月卡 ¥19.9/月
          </button>
        </div>
      </div>
    </Transition>

    <!-- ═══ 底部对话框 ═══ -->
    <div v-if="selectedRole" class="dialog-box" @click.stop>
      <div class="dialog-name-tag">
        {{ roleNames[selectedRole] || selectedRole }}
      </div>
      <div class="dialog-text">
        <span v-if="sending" class="typing-dots">
          <span class="dot" />
          <span class="dot" />
          <span class="dot" />
        </span>
        <span v-else>{{ currentReply || '……' }}</span>
      </div>
    </div>

    <!-- ═══ 输入栏：点击画面后滑入 ═══ -->
    <Transition name="input-slide">
      <div v-if="showInput && selectedRole" class="input-bar" @click.stop>
        <div class="input-bar-inner">
          <input
            ref="inputRef"
            v-model="inputText"
            type="text"
            class="input-field"
            :placeholder="'对 ' + (roleNames[selectedRole] || selectedRole) + ' 说点什么...'"
            :disabled="sending"
            @keydown.enter="sendMessage"
          />
          <button
            class="send-btn"
            :disabled="sending || !inputText.trim()"
            @click="sendMessage"
          >
            <Send class="w-4 h-4" />
          </button>
        </div>
      </div>
    </Transition>

    <!-- ═══ 左下角：角色切换按钮 ═══ -->
    <button
      v-if="selectedRole"
      class="char-switch-btn"
      @click.stop="showCharPanel = true"
    >
      <Users class="w-5 h-5" />
    </button>

    <!-- ═══ 角色选择面板（弹出层） ═══ -->
    <Teleport to="body">
      <Transition name="panel-slide">
        <div
          v-if="showCharPanel"
          class="char-panel-overlay"
          @click="showCharPanel = false"
        >
          <div class="char-panel" @click.stop>
            <div class="char-panel-header">
              <h3 class="text-lg font-extrabold text-gray-800">选择角色</h3>
              <button class="char-panel-close" @click="showCharPanel = false">
                <X class="w-5 h-5" />
              </button>
            </div>

            <!-- 搜索 -->
            <div class="char-panel-search">
              <SearchIcon class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                v-model="charPanelSearch"
                type="text"
                placeholder="搜索角色..."
                class="char-panel-search-input"
              />
            </div>

            <!-- 角色列表 -->
            <div class="char-panel-list">
              <button
                v-for="key in filteredCharList"
                :key="key"
                class="char-panel-item"
                :class="{ active: selectedRole === key }"
                @click="selectRole(key); showCharPanel = false"
              >
                <span class="text-lg">{{ roleEmojis[key] || '👤' }}</span>
                <span class="text-sm font-semibold text-gray-700">{{ roleNames[key] || key }}</span>
              </button>
            </div>

            <!-- 会员状态（面板底部） -->
            <div class="char-panel-footer">
              <div class="flex items-center gap-2">
                <Crown
                  class="w-4 h-4"
                  :class="membershipStatus?.active ? 'text-firefly' : 'text-gray-300'"
                />
                <span class="text-xs text-gray-500">
                  {{ membershipStatus?.active ? '萤火月卡' : '普通番舍' }}
                  · 每小时 {{ membershipStatus?.quotaLimitPerHour || 10 }} 次
                </span>
              </div>
              <button
                v-if="!membershipStatus?.active"
                class="text-xs font-bold text-firefly"
                @click="openMembershipModal(); showCharPanel = false"
              >
                升级
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 会员支付弹窗（保留原有逻辑） ═══ -->
    <Teleport to="body">
      <Transition name="member-modal">
        <div
          v-if="showMembershipModal"
          class="fixed inset-0 z-[80] flex items-center justify-center bg-gray-900/35 px-4 backdrop-blur-sm"
          @click.self="closeMembershipModal"
        >
          <div class="relative w-full max-w-3xl overflow-hidden rounded-[2rem] border border-white/70 bg-[#fffaf0]/90 shadow-[0_28px_90px_rgba(31,68,47,0.22)] backdrop-blur-xl">
            <div class="absolute left-8 top-6 h-24 w-24 rounded-full bg-firefly/20 blur-3xl" />
            <div class="absolute right-10 top-10 h-20 w-20 rounded-full bg-sky-200/40 blur-3xl" />

            <button
              type="button"
              class="absolute right-5 top-5 z-10 flex h-10 w-10 items-center justify-center rounded-full bg-white/70 text-gray-400 shadow-soft transition hover:text-gray-700"
              @click="closeMembershipModal"
            >
              <X class="h-5 w-5" />
            </button>

            <div class="relative grid gap-0 md:grid-cols-[1.1fr_0.9fr]">
              <div class="p-7 md:p-8">
                <p class="text-xs font-black uppercase tracking-[0.28em] text-firefly-600">Firefly Pass</p>
                <h2 class="mt-3 text-3xl font-black text-gray-800">萤火月卡</h2>
                <p class="mt-3 text-sm leading-7 text-gray-500">
                  {{ quotaModalTitle }}
                  开通后 30 天内，每个小时都可以和 AIGC 角色对话 100 次，后续语音功能也会优先开放给萤火会员。
                </p>

                <div class="mt-6 grid gap-3">
                  <div class="member-benefit">
                    <CheckCircle2 class="h-5 w-5 text-firefly-600" />
                    <span>AIGC 对话额度提升到每小时 100 次</span>
                  </div>
                  <div class="member-benefit">
                    <CheckCircle2 class="h-5 w-5 text-firefly-600" />
                    <span>预留角色语音功能权益，后续上线可直接使用</span>
                  </div>
                  <div class="member-benefit">
                    <CheckCircle2 class="h-5 w-5 text-firefly-600" />
                    <span>支持热门角色更长时间陪伴，减少额度打断感</span>
                  </div>
                </div>

                <div class="mt-7 flex flex-col gap-3 sm:flex-row">
                  <button
                    type="button"
                    class="inline-flex flex-1 items-center justify-center gap-2 rounded-full bg-firefly px-5 py-3 text-sm font-black text-white shadow-glow transition hover:scale-[1.02] active:scale-95"
                    @click="showPaymentPanel = true"
                  >
                    <Crown class="h-4 w-4" />
                    支付开通 {{ membershipStatus?.priceText || '¥19.9 / 月' }}
                  </button>
                  <button
                    type="button"
                    class="inline-flex items-center justify-center rounded-full border border-white/80 bg-white/60 px-5 py-3 text-sm font-bold text-gray-500 transition hover:bg-white"
                    @click="closeMembershipModal"
                  >
                    稍后再说
                  </button>
                </div>
              </div>

              <div class="border-t border-white/70 bg-white/45 p-7 md:border-l md:border-t-0 md:p-8">
                <div v-if="showPaymentPanel" class="text-center">
                  <p class="text-sm font-extrabold text-gray-700">支付宝收款码</p>
                  <p class="mt-1 text-xs text-gray-400">当前为占位图，下次替换为你的真实二维码</p>
                  <div class="mx-auto mt-5 flex h-56 w-56 items-center justify-center overflow-hidden rounded-[1.75rem] border border-white/80 bg-white p-4 shadow-soft">
                    <img :src="paymentQrUrl" alt="支付宝收款码占位图" class="h-full w-full rounded-2xl object-cover" />
                  </div>
                  <button
                    type="button"
                    class="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-full bg-gray-800 px-5 py-3 text-sm font-black text-white transition hover:bg-gray-700 disabled:opacity-60"
                    :disabled="activatingMembership"
                    @click="activateMonthlyMembership"
                  >
                    <CheckCircle2 class="h-4 w-4" />
                    {{ activatingMembership ? '正在开通...' : '我已完成支付，开通月卡' }}
                  </button>
                  <p class="mt-3 text-[11px] leading-5 text-gray-400">
                    开发期先用按钮模拟支付成功；正式接入支付宝后，这一步会改成支付回调自动开通。
                  </p>
                </div>

                <div v-else class="flex h-full min-h-[280px] flex-col items-center justify-center text-center">
                  <div class="flex h-24 w-24 items-center justify-center rounded-[2rem] bg-firefly/10 text-firefly-600">
                    <Sparkles class="h-11 w-11" />
                  </div>
                  <p class="mt-5 text-lg font-black text-gray-800">让角色多陪你一会儿</p>
                  <p class="mt-2 max-w-xs text-sm leading-7 text-gray-500">
                    每小时 100 次对话额度，更适合深聊剧情、角色理解和后续语音玩法。
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
```

- [ ] **Step 2: 提交**

```bash
git add pages/agent.vue
git commit -m "feat: rewrite agent template to Galgame fullscreen layout"
```

---

### Task 7: agent.vue - Script 逻辑重写

**Files:**
- Modify: `pages/agent.vue` (script 部分)

- [ ] **Step 1: 重写 `<script setup>` 块**

将现有 script（第416-745行）替换为：

```typescript
<script setup lang="ts">
import { Bot, CheckCircle2, Crown, Send, Search as SearchIcon, Sparkles, Users, Volume2, X } from 'lucide-vue-next'

// ═══ 角色数据 ═══
const roles = ref<Record<string, string>>({})
const roleNames = ref<Record<string, string>>({})
const roleOpeningAudios = ref<Record<string, string>>({})
const roleEmojis: Record<string, string> = {
  Makima: '🔴', Yor: '⚔️', Rem: '💙', Elsa: '🔪',
  Makoto: '🏊', Onodera: '🍬', Tsugumi: '🔫', Nagisa: '🍡',
  Frieren: '🪄',
}

const route = useRoute()
const userStore = useUserStore()
const { open: openAuthModal } = useAuthModal()

// ═══ UI 状态 ═══
const selectedRole = ref('')
const inputText = ref('')
const sending = ref(false)
const showInput = ref(false)
const showCharPanel = ref(false)
const charPanelSearch = ref('')
const showMembershipPopover = ref(false)
const inputRef = ref<HTMLInputElement>()

// ═══ 情感状态 ═══
const currentEmotion = ref<'happy' | 'normal' | 'complex'>('normal')
const currentReply = ref('')
let openingAudio: HTMLAudioElement | null = null

// ═══ 会员 ═══
interface MembershipStatus {
  active: boolean
  planCode: string
  planName: string
  priceText: string
  quotaLimitPerHour: number
  voiceEnabled: boolean
  expiresAt?: string | null
  paymentQrUrl?: string | null
}

const membershipStatus = ref<MembershipStatus | null>(null)
const showMembershipModal = ref(false)
const showPaymentPanel = ref(false)
const activatingMembership = ref(false)
const quotaNotice = ref('')

const currentAgentUserId = computed(() => userStore.user?.phone || userStore.user?.id || 'anonymous')
const paymentQrUrl = computed(() => membershipStatus.value?.paymentQrUrl || '/images/Onodera.png')
const quotaModalTitle = computed(() => quotaNotice.value || '萤火提醒：你的普通对话额度已用完。')

// ═══ 对话历史（保留用于上下文，但 UI 不展示） ═══
const chatHistories = ref<Record<string, { sender: string; text: string }[]>>({})

// ═══ 角色列表过滤 ═══
const filteredCharList = computed(() => {
  const q = charPanelSearch.value.trim().toLowerCase()
  const keys = Object.keys(roles.value)
  if (!q) return keys
  return keys.filter(key => {
    const name = (roleNames.value[key] || key).toLowerCase()
    const emoji = roleEmojis[key] || ''
    return name.includes(q) || key.toLowerCase().includes(q) || emoji.includes(q)
  })
})

// ═══ 动态图片 ═══
const portraitSrc = computed(() => {
  if (!selectedRole.value) return ''
  const code = selectedRole.value
  if (currentEmotion.value === 'happy') return `/images/${code}_happy.png`
  if (currentEmotion.value === 'complex') return `/images/${code}_complex.png`
  return `/images/${code}.png`
})

// ═══ 图片加载失败降级 ═══
const portraitFallback = ref('')
function onPortraitError(e: Event) {
  const img = e.target as HTMLImageElement
  const code = selectedRole.value
  if (!code) return
  // 如果情感图加载失败，降级到默认图
  if (img.src.includes('_happy') || img.src.includes('_complex')) {
    if (portraitFallback.value !== `/images/${code}.png`) {
      portraitFallback.value = `/images/${code}.png`
      img.src = `/images/${code}.png`
    }
  }
}

// ═══ API ═══
const config = useRuntimeConfig()
const apiBase = config.public.apiBase || '/api'

interface AgentCharacterConfig {
  code: string
  displayName: string
  openingAudioUrl?: string | null
}

const { data: charactersData } = await useAsyncData('agent-characters', async () => {
  try {
    const res = await $fetch<{ data: AgentCharacterConfig[] }>(`${apiBase}/agent/characters`)
    return res?.data ?? []
  } catch { return [] }
})

const { data: rolesData } = await useAsyncData('agent-roles', async () => {
  try {
    const res = await $fetch<{ data: Record<string, string> }>(`${apiBase}/agent/roles`)
    return res?.data ?? {}
  } catch { return {} }
})

watchEffect(() => {
  if (rolesData.value) {
    roles.value = rolesData.value
    roleNames.value = rolesData.value
  }
})

watch(
  () => currentAgentUserId.value,
  () => { void refreshMembershipStatus() },
  { immediate: true }
)

watchEffect(() => {
  if (!charactersData.value?.length) return
  const nextRoles: Record<string, string> = {}
  const nextOpeningAudios: Record<string, string> = {}
  for (const c of charactersData.value) {
    nextRoles[c.code] = c.displayName
    if (c.openingAudioUrl) nextOpeningAudios[c.code] = c.openingAudioUrl
  }
  roles.value = { ...roles.value, ...nextRoles }
  roleNames.value = { ...roleNames.value, ...nextRoles }
  roleOpeningAudios.value = nextOpeningAudios
})

watch(
  [() => route.query.role, () => Object.keys(roles.value).join('|')],
  ([role]) => {
    const roleCode = Array.isArray(role) ? role[0] : role
    if (roleCode && roles.value[roleCode] && selectedRole.value !== roleCode) {
      selectRole(roleCode)
    }
  },
  { immediate: true }
)

// ═══ 方法 ═══

function selectRole(key: string) {
  if (selectedRole.value === key) {
    playOpeningAudio(key)
    return
  }
  selectedRole.value = key
  currentEmotion.value = 'normal'
  currentReply.value = ''
  inputText.value = ''
  portraitFallback.value = ''
  showInput.value = false
  playOpeningAudio(key)
}

function playOpeningAudio(key: string) {
  if (import.meta.server) return
  const src = roleOpeningAudios.value[key]
  if (!src) return
  try {
    openingAudio?.pause()
    openingAudio = new Audio(src)
    openingAudio.volume = 0.82
    void openingAudio.play().catch(() => {})
  } catch {}
}

function onStageClick() {
  if (selectedRole.value && !sending.value) {
    showInput.value = !showInput.value
    if (showInput.value) {
      nextTick(() => inputRef.value?.focus())
    }
  }
  showMembershipPopover.value = false
}

function toggleMembership() {
  showMembershipPopover.value = !showMembershipPopover.value
}

function pushHistory(sender: string, text: string) {
  if (!selectedRole.value) return
  if (!chatHistories.value[selectedRole.value]) {
    chatHistories.value[selectedRole.value] = []
  }
  chatHistories.value[selectedRole.value].push({ sender, text })
}

async function refreshMembershipStatus() {
  try {
    const res = await $fetch<{ data?: MembershipStatus }>(`${apiBase}/agent/membership/status`, {
      params: { userId: currentAgentUserId.value },
    })
    membershipStatus.value = res?.data ?? null
  } catch {
    membershipStatus.value = {
      active: false, planCode: 'FREE', planName: '普通番舍成员', priceText: '¥19.9 / 月',
      quotaLimitPerHour: 10, voiceEnabled: false, paymentQrUrl: '/images/Onodera.png',
    }
  }
}

function openMembershipModal(message?: string) {
  quotaNotice.value = message || ''
  showPaymentPanel.value = false
  showMembershipModal.value = true
}

function closeMembershipModal() {
  showMembershipModal.value = false
  showPaymentPanel.value = false
}

async function activateMonthlyMembership() {
  if (!userStore.isLoggedIn || currentAgentUserId.value === 'anonymous') {
    closeMembershipModal()
    openAuthModal()
    return
  }
  activatingMembership.value = true
  try {
    const res = await $fetch<{ data?: MembershipStatus }>(`${apiBase}/agent/membership/activate-monthly`, {
      method: 'POST',
      body: { userId: currentAgentUserId.value },
    })
    membershipStatus.value = res?.data ?? membershipStatus.value
    closeMembershipModal()
    currentReply.value = '萤火月卡已经点亮啦。这个月里，每小时可以和角色对话 100 次，之后语音功能上线也会优先开放给你。'
  } catch {
    quotaNotice.value = '月卡开通暂时失败，请稍后再试。'
  } finally {
    activatingMembership.value = false
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !selectedRole.value || sending.value) return

  pushHistory('user', text)
  inputText.value = ''
  sending.value = true
  showInput.value = false

  const userId = currentAgentUserId.value
  const role = selectedRole.value

  try {
    const response = await fetch(`${apiBase}/agent/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text, role, userId }),
    })

    if (!response.ok || !response.body) throw new Error('SSE 连接失败')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let replyText = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const eventBlock of events) {
        const dataMatch = eventBlock.match(/^data:\s*(.+)$/m)
        const eventMatch = eventBlock.match(/^event:\s*(\w+)$/m)
        const eventName = eventMatch ? eventMatch[1] : 'token'
        const payload = dataMatch ? dataMatch[1].trim() : ''

        if (eventName === 'emotion' && payload) {
          currentEmotion.value = payload as 'happy' | 'normal' | 'complex'
        } else if (eventName === 'token' && payload) {
          replyText += payload
          currentReply.value = replyText
        } else if (eventName === 'quota') {
          const limit = parseQuotaLimit(payload)
          openMembershipModal(`${limit} 次对话额度已用光。`)
        }
      }
    }

    if (!replyText.trim()) {
      const res = await $fetch<{ data?: { reply?: string; emotion?: string; rateLimited?: boolean; quotaLimit?: number } }>(`${apiBase}/agent/chat`, {
        method: 'POST',
        body: { message: text, role, userId },
      })
      const data = res?.data
      replyText = data?.reply || `（${roleNames.value[role] || role} 轻轻沉默了片刻...）`
      currentReply.value = replyText
      if (data?.emotion) currentEmotion.value = data.emotion as 'happy' | 'normal' | 'complex'
      if (data?.rateLimited) openMembershipModal(`${data.quotaLimit || 10} 次对话额度已用光。`)
    }

    pushHistory('agent', replyText)
  } catch {
    currentReply.value = `（${roleNames.value[role] || role} 暂时无法回应...）`
    pushHistory('agent', currentReply.value)
  } finally {
    sending.value = false
  }
}

function parseQuotaLimit(payload: string) {
  const match = payload.match(/limit=(\d+)/)
  return match ? Number(match[1]) : 10
}

function formatMembershipTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}
</script>
```

- [ ] **Step 2: 提交**

```bash
git add pages/agent.vue
git commit -m "feat: rewrite agent script for Galgame mode with emotion state"
```

---

### Task 8: agent.vue - Galgame 风格 CSS

**Files:**
- Modify: `pages/agent.vue` (style 部分)

- [ ] **Step 1: 重写 `<style scoped>` 块**

将现有 style（第748-795行）替换为：

```css
<style scoped>
/* ═══ 全屏舞台 ═══ */
.galgame-stage {
  position: fixed;
  inset: 0;
  background: #1a1a2e;
  overflow: hidden;
  user-select: none;
  -webkit-user-select: none;
}

/* ═══ 背景层 ═══ */
.sprite-layer {
  position: absolute;
  inset: 0;
}
.sprite-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
}
.sprite-gradient {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(0deg, rgba(26,26,46,0.92) 0%, rgba(26,26,46,0.4) 25%, transparent 45%),
    linear-gradient(90deg, rgba(26,26,46,0.3) 0%, transparent 20%, transparent 80%, rgba(26,26,46,0.3) 100%);
}

/* ═══ 立绘切换动画 ═══ */
.sprite-crossfade-enter-active { transition: opacity 0.5s ease; }
.sprite-crossfade-leave-active { transition: opacity 0.3s ease; }
.sprite-crossfade-enter-from { opacity: 0; }
.sprite-crossfade-leave-to { opacity: 0; }

/* ═══ 无角色提示 ═══ */
.no-role-hint {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.no-role-icon {
  width: 80px; height: 80px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.05);
}

/* ═══ 萤火会员图标 ═══ */
.membership-icon-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 20;
  width: 40px; height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: background 0.2s;
}
.membership-icon-btn:hover {
  background: rgba(255,255,255,0.22);
}

/* ═══ 会员浮窗 ═══ */
.membership-popover {
  position: absolute;
  top: 68px;
  right: 20px;
  z-index: 25;
}
.membership-popover-content {
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 16px;
  padding: 16px 20px;
  min-width: 220px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

/* ═══ 对话框 ═══ */
.dialog-box {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 10;
  padding: 24px 32px 28px;
  background: linear-gradient(0deg, rgba(26,26,46,0.95) 0%, rgba(26,26,46,0.85) 60%, transparent 100%);
}
.dialog-name-tag {
  display: inline-block;
  padding: 4px 16px;
  margin-bottom: 10px;
  border-radius: 9999px;
  background: rgba(0,230,118,0.18);
  color: #00E676;
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 0.05em;
}
.dialog-text {
  font-size: 1.15rem;
  line-height: 1.8;
  color: rgba(255,255,255,0.9);
  font-weight: 500;
  min-height: 2.8em;
}

/* ═══ 打字动画 ═══ */
.typing-dots {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}
.dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: rgba(0,230,118,0.7);
  animation: dot-bounce 1.4s infinite ease-in-out both;
}
.dot:nth-child(1) { animation-delay: 0s; }
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ═══ 输入栏 ═══ */
.input-bar {
  position: absolute;
  bottom: 100px;
  left: 0;
  right: 0;
  z-index: 15;
  padding: 0 32px;
}
.input-bar-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 6px 6px 20px;
  border-radius: 9999px;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.15);
}
.input-field {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: rgba(255,255,255,0.9);
  font-size: 0.95rem;
  padding: 8px 0;
}
.input-field::placeholder {
  color: rgba(255,255,255,0.35);
}
.send-btn {
  width: 40px; height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #00E676;
  color: #1a1a2e;
  transition: transform 0.15s, opacity 0.2s;
  flex-shrink: 0;
}
.send-btn:disabled {
  opacity: 0.3;
  transform: scale(0.9);
}
.send-btn:not(:disabled):hover {
  transform: scale(1.08);
}
.send-btn:not(:disabled):active {
  transform: scale(0.95);
}

/* ═══ 输入栏动画 ═══ */
.input-slide-enter-active { transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.input-slide-leave-active { transition: all 0.2s ease-in; }
.input-slide-enter-from { opacity: 0; transform: translateY(16px); }
.input-slide-leave-to { opacity: 0; transform: translateY(8px); }

/* ═══ 角色切换按钮 ═══ */
.char-switch-btn {
  position: absolute;
  bottom: 24px;
  left: 24px;
  z-index: 20;
  width: 44px; height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: rgba(255,255,255,0.7);
  transition: background 0.2s, color 0.2s;
}
.char-switch-btn:hover {
  background: rgba(255,255,255,0.22);
  color: #fff;
}

/* ═══ 角色选择面板 ═══ */
.char-panel-overlay {
  position: fixed;
  inset: 0;
  z-index: 70;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.char-panel {
  width: 100%;
  max-width: 420px;
  max-height: 70vh;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px 24px 0 0;
  padding: 20px 20px 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}
.char-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.char-panel-close {
  width: 36px; height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.05);
  color: #666;
}
.char-panel-search {
  position: relative;
}
.char-panel-search-input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  border-radius: 14px;
  border: 1px solid rgba(0,0,0,0.08);
  background: rgba(0,0,0,0.03);
  outline: none;
  font-size: 0.9rem;
  color: #333;
}
.char-panel-search-input:focus {
  border-color: #00E676;
  background: #fff;
}
.char-panel-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.char-panel-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  transition: background 0.15s;
  text-align: left;
}
.char-panel-item:hover { background: rgba(0,230,118,0.06); }
.char-panel-item.active { background: rgba(0,230,118,0.12); }
.char-panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(0,0,0,0.03);
}

/* ═══ 面板动画 ═══ */
.panel-slide-enter-active { transition: all 0.28s cubic-bezier(0.16, 1, 0.3, 1); }
.panel-slide-leave-active { transition: all 0.2s ease-in; }
.panel-slide-enter-from { opacity: 0; }
.panel-slide-enter-from .char-panel { transform: translateY(100%); }
.panel-slide-leave-to { opacity: 0; }
.panel-slide-leave-to .char-panel { transform: translateY(40px); }

/* ═══ 浮窗动画 ═══ */
.fade-scale-enter-active { transition: all 0.2s ease; }
.fade-scale-leave-active { transition: all 0.15s ease-in; }
.fade-scale-enter-from { opacity: 0; transform: scale(0.92); }
.fade-scale-leave-to { opacity: 0; transform: scale(0.95); }

/* ═══ 会员弹窗（保留原有样式） ═══ */
.member-benefit {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.58);
  padding: 0.8rem 0.9rem;
  color: #4b5563;
  font-size: 0.875rem;
  font-weight: 700;
}
.member-modal-enter-active,
.member-modal-leave-active { transition: opacity 0.24s ease; }
.member-modal-enter-from,
.member-modal-leave-to { opacity: 0; }
.member-modal-enter-active > div,
.member-modal-leave-active > div { transition: transform 0.24s ease, opacity 0.24s ease; }
.member-modal-enter-from > div,
.member-modal-leave-to > div { transform: translateY(12px) scale(0.98); opacity: 0; }

/* ═══ 移动端适配 ═══ */
@media (max-width: 768px) {
  .dialog-box {
    padding: 20px 20px 24px;
  }
  .dialog-text {
    font-size: 1rem;
  }
  .input-bar {
    bottom: 90px;
    padding: 0 16px;
  }
  .char-switch-btn {
    bottom: 16px;
    left: 16px;
  }
  .membership-icon-btn {
    top: 12px;
    right: 12px;
  }
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add pages/agent.vue
git commit -m "feat: add Galgame stage CSS with animations and mobile responsive"
```

---

### Task 9: 关联跳转适配 - 其他页面到 `/agent?role=xxx` 的链接保持不变

**Files:**
- No changes needed. `/agent?role=Makima` 等 URL 参数在当前 `selectRole` 逻辑中已正确处理。

无需额外改动。验证即可。

- [ ] **Step 1: 验证 URL 参数逻辑**

当前代码中已有的 watch（第546-555行）监听 `route.query.role` 并在角色加载后调用 `selectRole`，逻辑无需修改。

---

### Task 10: 缓存回复适配情感标签

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/service/AgentService.java`

- [ ] **Step 1: 给默认缓存回复添加情感标签前缀**

将 `DEFAULT_COMMON_REPLIES`（第71-84行）中的回复加上情感标签：

```java
    private static final Map<String, Map<String, String>> DEFAULT_COMMON_REPLIES = Map.of(
            "Rem", Map.of(
                    "hello", "[happy]蕾姆在这里哦。能听见您的声音，蕾姆觉得很安心。",
                    "what_doing", "[normal]蕾姆正在整理房间，也在等您来找蕾姆说话呢。",
                    "thanks", "[happy]不用谢哦，能帮上您，蕾姆已经很开心了。",
                    "goodnight", "[complex]晚安。蕾姆会守在这里，愿您今晚做个温柔的梦。"
            ),
            "Onodera", Map.of(
                    "hello", "[complex]你、你好呀……能见到你，我有一点开心呢。（脸红）",
                    "what_doing", "[normal]我正在想着今天要不要烤点点心……那个，你想尝尝吗？",
                    "thanks", "[complex]不、不用这么客气啦……能帮到你我也很开心。",
                    "goodnight", "[normal]晚安哦。希望你明天也能遇到一点温柔的事情。"
            )
    );
```

- [ ] **Step 2: 编译验证**

Run: `cd aniglow-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add aniglow-backend/src/main/java/com/aniglow/service/AgentService.java
git commit -m "feat: add emotion tags to default cached replies"
```

---

### Task 11: 完整编译验证

- [ ] **Step 1: 后端完整编译**

Run: `cd aniglow-backend && mvn package -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端类型检查**

Run: `cd /Users/mac/Desktop/四季番约 && npx nuxi typecheck 2>&1 || true`

- [ ] **Step 3: 提交（如有 lint 修复）**

如有自动修复的 lint 问题，提交修复 commit。

---

### Task 12: 功能验证清单

启动前后端后手动验证：

- [ ] **Step 1:** 访问 `/agent` 页面，确认 Galgame 全屏布局正确渲染
- [ ] **Step 2:** 点击左下角角色按钮，弹出角色面板，搜索和选择角色正常
- [ ] **Step 3:** 选择角色后，立绘和对话框正常显示
- [ ] **Step 4:** 点击画面，输入栏从底部滑入
- [ ] **Step 5:** 发送消息，对话框显示角色回复，立绘图根据情感切换
- [ ] **Step 6:** 点击画面空白处，输入栏滑出隐藏
- [ ] **Step 7:** 右上角萤火图标，点击弹出会员状态浮窗
- [ ] **Step 8:** 切换角色，立绘切换，情感重置为 normal
- [ ] **Step 9:** 移动端（Chrome DevTools 模拟）布局正常
- [ ] **Step 10:** 历史对话上下文正常（多轮对话角色记得之前内容）
