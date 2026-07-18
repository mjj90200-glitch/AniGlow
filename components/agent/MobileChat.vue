<template>
    <section class="aigc-mobile md:hidden flex flex-col" style="height: calc(100vh - 5rem);">
      <!-- 顶栏：当前角色 + 情绪 -->
      <div class="chat-topbar">
        <button class="chat-topbar-role" @click="showMobileRoles = !showMobileRoles">
          <div class="chat-topbar-avatar">
            <img
              v-if="selectedRole && hasImage(selectedRole)"
              :src="`/images/${selectedRole}.png`"
              :alt="roleNames[selectedRole] || selectedRole"
              class="h-full w-full object-cover"
              @error="onSmallAvatarError"
            />
            <span v-else class="text-lg">{{ roleEmojis[selectedRole] || '👤' }}</span>
          </div>
          <div class="chat-topbar-info">
            <span class="chat-topbar-name">{{ roleNames[selectedRole] || '选择角色' }}</span>
            <span class="chat-topbar-emotion">{{ emotionLabel }}</span>
          </div>
          <ChevronDown class="w-4 h-4 text-gray-400" :class="{ 'rotate-180': showMobileRoles }" />
        </button>
        <div class="chat-topbar-actions">
          <button class="chat-topbar-btn" @click="showMobileSearch = !showMobileSearch" :aria-label="showMobileSearch ? '关闭搜索' : '搜索角色'">
            <Search v-if="!showMobileSearch" class="w-4 h-4" />
            <X v-else class="w-4 h-4" />
          </button>
          <button class="chat-topbar-btn" @click="openDonatePopup()">
            <Crown class="w-4 h-4" :class="membershipStatus?.active ? 'text-firefly' : 'text-gray-400'" />
          </button>
        </div>
      </div>

      <button class="quota-strip-mobile" type="button" @click="openDonatePopup()">
        <span class="quota-strip-copy">
          <span class="quota-strip-label">本小时对话</span>
          <strong>{{ quotaStatus.remaining }} / {{ quotaStatus.limit }}</strong>
        </span>
        <span class="quota-strip-track"><span :style="{ width: `${quotaPercent}%` }" /></span>
        <span class="quota-strip-reset">{{ quotaResetLabel }}</span>
      </button>

      <!-- 搜索栏 -->
      <div v-if="showMobileSearch" class="chat-search-bar">
        <Search class="h-3.5 w-3.5 text-gray-400 shrink-0" />
        <input
          ref="searchInputRef"
          v-model="searchText"
          type="text"
          placeholder="搜索角色..."
          class="flex-1 bg-transparent outline-none text-sm font-bold text-gray-700"
        />
        <button v-if="searchText" class="text-gray-400" @click="searchText = ''"><X class="h-3.5 w-3.5" /></button>
      </div>

      <!-- 角色列表面板 -->
      <div v-if="showMobileRoles" class="chat-role-panel">
        <div v-if="searchText.trim()" class="chat-role-scroll">
          <button
            v-for="key in filteredSearchResults"
            :key="key"
            class="chat-role-item" :class="{ active: selectedRole === key }"
            @click="selectSearchResult(key)"
          >
            <span class="text-base">{{ roleEmojis[key] || '👤' }}</span>
            <span class="text-xs font-bold">{{ roleNames[key] || key }}</span>
          </button>
          <p v-if="filteredSearchResults.length === 0" class="text-xs text-gray-400 text-center py-4">未找到匹配的角色</p>
        </div>
        <div v-else class="chat-role-scroll">
          <button
            v-for="key in allRoleKeys"
            :key="key"
            class="chat-role-item" :class="{ active: selectedRole === key }"
            @click="selectRole(key)"
          >
            <span class="text-lg">{{ roleEmojis[key] || '👤' }}</span>
            <span class="text-xs font-bold">{{ roleNames[key] || key }}</span>
          </button>
        </div>
      </div>

      <!-- 聊天气泡区 -->
      <div ref="chatContainerRef" class="chat-bubble-area">
        <!-- 空状态 -->
        <div v-if="!hasMessages" class="chat-empty">
          <div class="chat-empty-avatar">
            <img
              v-if="selectedRole && hasImage(selectedRole)"
              :src="`/images/${selectedRole}.png`"
              :alt="roleNames[selectedRole] || selectedRole"
              class="h-full w-full object-cover rounded-2xl"
            />
            <span v-else class="text-5xl">{{ roleEmojis[selectedRole] || '👤' }}</span>
          </div>
          <h3 class="text-lg font-extrabold text-gray-700 mt-4">{{ roleNames[selectedRole] || '选择一个角色' }}</h3>
          <p v-if="currentCharacterSource" class="text-xs font-bold text-firefly-600 mt-1">
            来自《{{ currentCharacterSource }}》
          </p>
          <p class="text-sm text-gray-400 mt-2">{{ roleGreetings[selectedRole] || '发送第一条消息，开始对话吧' }}</p>
          <!-- 建议话题 -->
          <div class="chat-suggestions" v-if="selectedRole">
            <button
              v-for="topic in suggestTopics"
              :key="topic"
              class="chat-suggestion-btn"
              @click="sendSuggestion(topic)"
            >
              {{ topic }}
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <template v-else>
          <div
            v-for="(msg, i) in currentMessages"
            :key="i"
            class="bubble-row"
            :class="msg.sender === 'user' ? 'bubble-row-user' : 'bubble-row-agent'"
          >
            <!-- AI消息：表情贴图 + 气泡 -->
            <template v-if="msg.sender === 'agent'">
              <AgentEmotionStickers
                :src="stickerSrc(msg.emotion)"
                :fallback-src="`/images/${selectedRole}.png`"
                :alt="roleNames[selectedRole] || selectedRole"
                :fallback-emoji="roleEmojis[selectedRole]"
                :image-available="Boolean(selectedRole && hasImage(selectedRole))"
              />
              <div class="bubble-agent-col">
                <span class="bubble-name">{{ roleNames[selectedRole] || '角色' }}</span>
                <div class="bubble-agent">
                  {{ msg.text }}
                </div>
              </div>
            </template>
            <!-- 用户消息：只有气泡 -->
            <template v-else>
              <div class="bubble-user">{{ msg.text }}</div>
            </template>
          </div>
          <!-- 正在输入动画 -->
          <div v-if="sending && !streamingText" class="typing-dots">
            <span></span><span></span><span></span>
          </div>
        </template>
      </div>

      <!-- 底部输入栏 -->
      <div class="chat-input-bar">
        <input
          ref="inputRef"
          v-model="inputText"
          type="text"
          class="chat-input"
          :placeholder="selectedRole ? '对 ' + roleNames[selectedRole] + ' 说点什么...' : '选择一个角色开始聊天'"
          :disabled="sending || !selectedRole"
          @keydown.enter="sendMessage"
        />
        <button
          class="chat-send-btn"
          :disabled="sending || !inputText.trim() || !selectedRole"
          @click="sendMessage"
        >
          <Send class="w-4 h-4" />
        </button>
      </div>
    </section>
</template>
<script setup lang="ts">
import { ChevronDown, Crown, Search, Send, X } from 'lucide-vue-next'
const props = defineProps<{ chat: Record<string, any> }>()
const {
  selectedRole, hasImage, roleNames, roleEmojis, currentCharacterSource, emotionLabel,
  showMobileRoles, showMobileSearch, openDonatePopup, quotaStatus, quotaPercent, quotaResetLabel,
  searchInputRef, searchText, filteredSearchResults, selectSearchResult, allRoleKeys, selectRole,
  chatContainerRef, hasMessages, currentMessages, stickerSrc, sending, streamingText,
  suggestTopics, sendSuggestion, inputRef, inputText, sendMessage, onSmallAvatarError,
  membershipStatus, roleGreetings,
} = toRefs(props.chat)
</script>
