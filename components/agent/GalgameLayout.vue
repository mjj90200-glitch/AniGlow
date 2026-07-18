<template>
    <div class="hidden md:block">
    <section class="aigc-shell">
      <!-- ═══ Left Sidebar ═══ -->
      <aside class="aigc-rail">
        <!-- Mobile top row: character + actions -->
        <div class="rail-top-row">
          <!-- Current character avatar -->
          <div class="rail-current">
            <div class="rail-avatar">
              <img
                v-if="selectedRole && hasImage(selectedRole)"
                :src="`/images/${selectedRole}.png`"
                :alt="roleNames[selectedRole] || selectedRole"
                class="h-full w-full object-cover"
                @error="onSmallAvatarError"
              />
              <span v-else class="text-4xl">{{ roleEmojis[selectedRole] || '👤' }}</span>
            </div>
            <div class="rail-current-info">
              <p class="rail-name">{{ roleNames[selectedRole] || '选择角色' }}</p>
              <p class="rail-emotion">{{ emotionLabel }}</p>
            </div>
          </div>

          <!-- Mobile actions group -->
          <div class="rail-actions">
            <!-- Search toggle (mobile only) -->
            <button
              class="rail-search-toggle"
              type="button"
              @click="showMobileSearch = !showMobileSearch"
              :aria-label="showMobileSearch ? '关闭搜索' : '搜索角色'"
            >
              <Search v-if="!showMobileSearch" class="h-4 w-4" />
              <X v-else class="h-4 w-4" />
            </button>

            <!-- Membership -->
            <button
              class="rail-pass"
              type="button"
              @click="openDonatePopup()"
            >
              <Crown class="h-4 w-4" :class="membershipStatus?.active ? 'text-firefly' : 'text-gray-400'" />
              <span>{{ membershipStatus?.active ? '萤火月卡' : '普通番舍' }}</span>
              <small>每小时 {{ membershipStatus?.quotaLimitPerHour || 10 }} 次</small>
            </button>
          </div>
        </div>

        <!-- Search box (desktop always visible, mobile toggled) -->
        <div class="rail-search" :class="{ 'rail-search-open': showMobileSearch }">
          <Search class="h-4 w-4 text-gray-400 shrink-0" />
          <input
            ref="searchInputRef"
            v-model="searchText"
            type="text"
            placeholder="搜索角色..."
            class="search-input"
          />
          <button
            v-if="searchText"
            type="button"
            class="search-clear"
            @click="searchText = ''"
          >
            <X class="h-3.5 w-3.5" />
          </button>
        </div>

        <!-- Search results -->
        <div v-if="searchText.trim()" class="rail-role-list">
          <p class="rail-section-title">搜索结果</p>
          <button
            v-for="key in filteredSearchResults"
            :key="key"
            class="role-btn"
            :class="{ active: selectedRole === key }"
            type="button"
            @click="selectSearchResult(key)"
          >
            <span class="role-emoji">{{ roleEmojis[key] || '👤' }}</span>
            <span class="role-label">{{ roleNames[key] || key }}</span>
          </button>
          <p v-if="filteredSearchResults.length === 0" class="search-empty">
            未找到匹配的角色
          </p>
        </div>

        <!-- Role list (when not searching) -->
        <template v-else>
          <div class="rail-role-list">
            <p class="rail-section-title">Main Cast</p>
            <button
              v-for="key in mainRoles"
              :key="key"
              class="role-btn"
              :class="{ active: selectedRole === key }"
              type="button"
              @click="selectRole(key)"
            >
              <span class="role-emoji">{{ roleEmojis[key] || '👤' }}</span>
              <span class="role-label">{{ roleNames[key] || key }}</span>
            </button>
          </div>
        </template>
      </aside>

      <!-- ═══ Main Stage ═══ -->
      <main class="galgame-box">
        <!-- Background gradient -->
        <div class="galgame-bg-layer" />

        <button class="quota-badge-desktop" type="button" @click="openDonatePopup()">
          <span class="quota-badge-top">
            <Gauge class="h-3.5 w-3.5" />
            本小时剩余 <strong>{{ quotaStatus.remaining }}</strong> / {{ quotaStatus.limit }} 次
          </span>
          <span class="quota-badge-track"><span :style="{ width: `${quotaPercent}%` }" /></span>
          <small>{{ quotaResetLabel }}</small>
        </button>

        <!-- Character portrait -->
        <Transition name="sprite-crossfade" mode="out-in">
          <img
            v-if="selectedRole"
            :key="selectedRole + '-' + currentEmotion"
            :src="portraitSrc"
            :alt="roleNames[selectedRole] || selectedRole"
            class="galgame-portrait"
            @error="onPortraitError"
          />
        </Transition>

        <!-- Overlay gradients for depth -->
        <div class="galgame-wash-top" />
        <div class="galgame-wash-bottom" />

        <!-- Reply display box -->
        <Transition name="reply-fade">
          <div v-if="currentReply || sending" class="galgame-reply-box">
            <p class="reply-character">{{ roleNames[selectedRole] || selectedRole }}</p>
            <p class="reply-text">
              {{ currentReply || '...' }}
              <span v-if="sending && !currentReply" class="reply-cursor">|</span>
            </p>
          </div>
        </Transition>

        <!-- Input bar -->
        <div class="dialog-input-row">
          <input
            ref="inputRef"
            v-model="inputText"
            type="text"
            class="dialog-input"
            :placeholder="'对 ' + (roleNames[selectedRole] || selectedRole) + ' 说点什么...'"
            :disabled="sending || !selectedRole"
            @keydown.enter="sendMessage"
          />
          <button
            class="dialog-send"
            type="button"
            :disabled="sending || !inputText.trim() || !selectedRole"
            @click="sendMessage"
          >
            <Send class="h-4 w-4" />
          </button>
        </div>
      </main>
    </section>
    </div>
</template>
<script setup lang="ts">
import { Crown, Gauge, Search, Send, X } from 'lucide-vue-next'
const props = defineProps<{ chat: Record<string, any> }>()
const {
  selectedRole, hasImage, roleNames, roleEmojis, emotionLabel, showMobileSearch,
  membershipStatus, openDonatePopup, searchInputRef, searchText, filteredSearchResults,
  selectSearchResult, mainRoles, selectRole, quotaStatus, quotaPercent, quotaResetLabel,
  currentEmotion, portraitSrc, onPortraitError, currentReply, sending, inputRef, inputText,
  sendMessage, onSmallAvatarError,
} = toRefs(props.chat)
</script>
