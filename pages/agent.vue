<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 lg:pt-24 pb-4 lg:pb-24">

    <!-- ═══════════════════════════════════════════════════════════
         MOBILE · 微信风格聊天界面
         ═══════════════════════════════════════════════════════════ -->

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
              <div class="bubble-sticker">
                <img
                  v-if="selectedRole && hasImage(selectedRole)"
                  :src="stickerSrc(msg.emotion)"
                  :alt="roleNames[selectedRole] || selectedRole"
                  class="h-full w-full object-cover"
                  @error="(e: Event) => { (e.target as HTMLImageElement).src = `/images/${selectedRole}.png` }"
                />
                <span v-else class="text-2xl">{{ roleEmojis[selectedRole] || '👤' }}</span>
              </div>
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

    <!-- ═══════════════════════════════════════════════════════════
         DESKTOP · 原有 Galgame 布局
         ═══════════════════════════════════════════════════════════ -->

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

    <Teleport to="body">
      <Transition name="donate-popup">
        <div
          v-if="showDonatePopup"
          class="fixed inset-0 z-[100] flex items-center justify-center bg-gray-900/20 px-4 backdrop-blur-md"
          role="dialog"
          aria-modal="true"
          aria-labelledby="agent-membership-title"
          @click.self="showDonatePopup = false"
        >
          <section class="relative w-full max-w-sm rounded-[2rem] border border-white/75 bg-cream/90 p-6 text-center shadow-[0_28px_80px_rgba(31,68,47,0.2)] backdrop-blur-2xl">
            <button class="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-full bg-white/75 text-gray-400 transition hover:text-gray-700" aria-label="关闭会员窗口" @click="showDonatePopup = false">
              <X class="h-4 w-4" />
            </button>
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-firefly/15 text-firefly-700">
              <Crown class="h-6 w-6" />
            </div>
            <h2 id="agent-membership-title" class="mt-4 text-xl font-black text-gray-800">{{ donateTitle }}</h2>
            <p class="mt-2 text-sm leading-6 text-gray-500">
              当前剩余 {{ quotaStatus.remaining }} / {{ quotaStatus.limit }} 次，{{ quotaResetLabel }}。
            </p>
            <div class="mx-auto mt-4 h-44 w-44 overflow-hidden rounded-2xl border border-white/80 bg-white p-3 shadow-soft">
              <img :src="douyinQrUrl" alt="萤火会员支付二维码" class="h-full w-full rounded-xl object-cover" />
            </div>
            <div class="mt-4 rounded-3xl bg-white/60 p-4 text-left">
              <p class="text-sm font-black text-firefly-700">萤火月卡 · ¥9.9 / 月</p>
              <p class="mt-2 text-xs font-bold leading-6 text-gray-500">每小时 100 次 AIGC 对话，会员期内可使用后续上线的角色语音功能。</p>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup lang="ts">
import { ChevronDown, Crown, Gauge, Search, Send, X } from 'lucide-vue-next'

// ═══ Hardcoded fallback character data ═══
const DEFAULT_ROLES: Record<string, string> = {
  Makima: '玛奇玛',
  Rem: '蕾姆',
  Onodera: '小野寺',
  Nagisa: '古河渚',
  Frieren: '芙莉莲',
  Kaoruko: '薰子',
  Marin: '海梦',
}

const DEFAULT_CHARACTERS: { code: string; displayName: string; sourceTitle?: string | null; openingAudioUrl: string | null }[] = [
  { code: 'Makima', displayName: '玛奇玛', openingAudioUrl: null },
  { code: 'Rem', displayName: '蕾姆', openingAudioUrl: null },
  { code: 'Onodera', displayName: '小野寺', openingAudioUrl: null },
  { code: 'Nagisa', displayName: '古河渚', openingAudioUrl: null },
  { code: 'Frieren', displayName: '芙莉莲', openingAudioUrl: null },
  { code: 'Kaoruko', displayName: '薰子', openingAudioUrl: null },
  { code: 'Marin', displayName: '海梦', openingAudioUrl: null },
]

const mainRoles = ['Kaoruko', 'Onodera', 'Marin']

// 所有角色（用于移动端横滑列表）
const allRoleKeys = computed(() => Object.keys(roles.value))

const roleEmojis: Record<string, string> = {
  Makima: '🔴', Rem: '💙',
  Onodera: '🍬', Nagisa: '🍡',
  Frieren: '🪄', Kaoruko: '🍰', Marin: '👗',
}

// ═══ State ═══
const roles = ref<Record<string, string>>({ ...DEFAULT_ROLES })
const roleNames = ref<Record<string, string>>({ ...DEFAULT_ROLES })
const roleOpeningAudios = ref<Record<string, string>>({})
const selectedRole = ref('Onodera')
const inputText = ref('')
const sending = ref(false)
const searchText = ref('')
const showMobileSearch = ref(false)
const showMobileRoles = ref(false)
const streamingText = ref('')
const inputRef = ref<HTMLInputElement>()
const searchInputRef = ref<HTMLInputElement>()
const chatContainerRef = ref<HTMLElement>()
const currentEmotion = ref<'happy' | 'normal' | 'complex'>('normal')
const currentReply = ref('')
let openingAudio: HTMLAudioElement | null = null

const route = useRoute()
const userStore = useUserStore()
const { open: openAuthModal } = useAuthModal()

// ═══ 移动端聊天数据 ═══

const hasMessages = computed(() => {
  const msgs = chatHistories.value[selectedRole.value]
  return msgs && msgs.length > 0
})

const currentMessages = computed(() => {
  return chatHistories.value[selectedRole.value] || []
})

const roleGreetings: Record<string, string> = {
  Kaoruko: '嗨！我是薰子，喜欢甜食和努力的感觉～想聊什么都可以哦！',
  Onodera: '那个……你好……我是小野寺小咲。虽然有点紧张，但我会努力的！',
  Marin: '哟！我是喜多川海梦～动画和cosplay超喜欢！你也喜欢动漫吗？',
  Makima: '你好。我是玛奇玛。请多指教。',
  Rem: '蕾姆在这里。有什么需要帮忙的吗？',
  Nagisa: '那个...你好。我是古河渚。你喜欢团子大家族吗？',
  Frieren: '嗯...我是芙莉莲。这是旅途中的一段小憩。',
}

const suggestTopics = computed(() => {
  const topics: Record<string, string[]> = {
    Kaoruko: ['今天吃了什么好吃的？', '学习好累，给我打打气', '你觉得努力的意义是什么？'],
    Onodera: ['你最近在忙什么？', '有没有喜欢的动漫？', '可以给我推荐一部番吗？'],
    Marin: ['最近在cos什么角色？', '推荐一部热血番！', '你也喜欢手作吗？'],
  }
  return topics[selectedRole.value] || ['你好呀！', '今天过得怎么样？', '聊聊最近在看的番吧']
})

function emotionIcon(emotion: string) {
  if (emotion === 'happy') return '😊'
  if (emotion === 'complex') return '💭'
  return ''
}

function stickerSrc(emotion?: string) {
  if (!selectedRole.value) return ''
  const code = selectedRole.value
  if (emotion === 'happy') return `/images/${code}_happy.png`
  if (emotion === 'complex') return `/images/${code}_complex.png`
  return `/images/${code}.png`
}

function autoScrollToBottom() {
  nextTick(() => {
    const el = chatContainerRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function sendSuggestion(topic: string) {
  inputText.value = topic
  sendMessage()
}

// ═══ Membership ═══
interface MembershipStatus {
  active: boolean
  planCode: string
  planName: string
  priceText: string
  quotaLimitPerHour: number
  quotaLimitPerDay?: number
  voiceEnabled: boolean
  expiresAt?: string | null
  paymentQrUrl?: string | null
}

const membershipStatus = ref<MembershipStatus | null>(null)
const showDonatePopup = ref(false)
const quotaNotice = ref('')

interface QuotaStatus {
  limit: number
  used: number
  remaining: number
  resetAt: string
  activeMember: boolean
  planName: string
  voiceEnabled: boolean
}

const quotaStatus = ref<QuotaStatus>({
  limit: 10,
  used: 0,
  remaining: 10,
  resetAt: '',
  activeMember: false,
  planName: '普通番舍成员',
  voiceEnabled: false,
})

const currentAgentUserId = computed(() => userStore.user?.phone || userStore.user?.id || 'anonymous')
const douyinQrUrl = computed(() => membershipStatus.value?.paymentQrUrl || '/images/douyin-qr.png')
const donateTitle = computed(() => quotaNotice.value || '喜欢角色的陪伴吗？')
const quotaPercent = computed(() => quotaStatus.value.limit > 0
  ? Math.max(0, Math.min(100, (quotaStatus.value.remaining / quotaStatus.value.limit) * 100))
  : 0)
const quotaResetLabel = computed(() => {
  if (!quotaStatus.value.resetAt) return '下个整点重置'
  const reset = new Date(quotaStatus.value.resetAt)
  if (Number.isNaN(reset.getTime())) return '下个整点重置'
  return `${reset.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })} 重置`
})

// ═══ Emotion helpers ═══
const emotionLabel = computed(() => {
  if (!currentReply.value) return '等待中...'
  if (currentEmotion.value === 'happy') return '心情愉悦 ✨'
  if (currentEmotion.value === 'complex') return '若有所思 💭'
  return '平静'
})

// ═══ Chat history ═══
const chatHistories = ref<Record<string, { sender: string; text: string; emotion?: string }[]>>({})

// ═══ Role lists ═══
const CHAR_VARIANT_MAP: Record<string, string> = {
  '熏': '薰', // 熏 → 薰
}

function normalizeForSearch(s: string): string {
  return [...s.toLowerCase()].map(c => CHAR_VARIANT_MAP[c] || c).join('')
}

const filteredSearchResults = computed(() => {
  const q = normalizeForSearch(searchText.value.trim())
  if (!q) return []
  return Object.keys(roles.value).filter(key => {
    const name = normalizeForSearch(roleNames.value[key] || key)
    return name.includes(q) || key.toLowerCase().includes(q)
  })
})

// ═══ Dynamic portrait based on emotion ═══
const portraitSrc = computed(() => {
  if (!selectedRole.value) return ''
  const code = selectedRole.value
  if (currentEmotion.value === 'happy') return `/images/${code}_happy.png`
  if (currentEmotion.value === 'complex') return `/images/${code}_complex.png`
  return `/images/${code}.png`
})

function hasImage(key: string): boolean {
  return ['Makima', 'Rem', 'Onodera', 'Nagisa', 'Frieren', 'Kaoruko', 'Marin'].includes(key)
}

function onSmallAvatarError(e: Event) {
  const img = e.target as HTMLImageElement
  const box = img.parentElement
  if (!box) return
  const alt = img.alt || ''
  const roleKey = Object.keys(roleNames.value).find(key => roleNames.value[key] === alt) || selectedRole.value
  box.innerHTML = `<span class="text-xl">${roleEmojis[roleKey] || '👤'}</span>`
}

const portraitFallback = ref('')
function onPortraitError(e: Event) {
  const img = e.target as HTMLImageElement
  const code = selectedRole.value
  if (!code) return
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
  sourceTitle?: string | null
  openingAudioUrl?: string | null
}

const { data: charactersData } = await useAsyncData('agent-characters', async () => {
  try {
    const res = await $fetch<{ data: AgentCharacterConfig[] }>(`${apiBase}/agent/characters`)
    return res?.data?.length ? res.data : DEFAULT_CHARACTERS
  } catch { return DEFAULT_CHARACTERS }
})

const currentCharacterSource = computed(() => {
  const chars = charactersData.value ?? []
  const found = chars.find(c => c.code === selectedRole.value)
  return found?.sourceTitle || ''
})

const { data: rolesData } = await useAsyncData('agent-roles', async () => {
  try {
    const res = await $fetch<{ data: Record<string, string> }>(`${apiBase}/agent/roles`)
    return res?.data && Object.keys(res.data).length > 0 ? res.data : DEFAULT_ROLES
  } catch { return DEFAULT_ROLES }
})

watchEffect(() => {
  if (rolesData.value) {
    roles.value = { ...DEFAULT_ROLES, ...rolesData.value }
    roleNames.value = { ...DEFAULT_ROLES, ...rolesData.value }
  }
})

watch(
  () => currentAgentUserId.value,
  () => { void Promise.all([refreshMembershipStatus(), refreshQuotaStatus()]) },
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
  roles.value = { ...DEFAULT_ROLES, ...roles.value, ...nextRoles }
  roleNames.value = { ...DEFAULT_ROLES, ...roleNames.value, ...nextRoles }
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

// ═══ Methods ═══

function selectRole(key: string) {
  showMobileRoles.value = false
  showMobileSearch.value = false
  if (selectedRole.value === key) {
    playOpeningAudio(key)
    return
  }
  closeChat()
  nextTick(() => {
    selectedRole.value = key
    currentEmotion.value = 'normal'
    currentReply.value = ''
    inputText.value = ''
    portraitFallback.value = ''
    streamingText.value = ''
    nextTick(() => inputRef.value?.focus())
    playOpeningAudio(key)
  })
}

function selectSearchResult(key: string) {
  searchText.value = ''
  selectRole(key)
}

function closeChat() {
  selectedRole.value = ''
  currentEmotion.value = 'normal'
  currentReply.value = ''
  inputText.value = ''
  sending.value = false
  portraitFallback.value = ''
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

function pushHistory(sender: string, text: string, emotion?: string) {
  if (!selectedRole.value) return
  if (!chatHistories.value[selectedRole.value]) {
    chatHistories.value[selectedRole.value] = []
  }
  chatHistories.value[selectedRole.value].push({ sender, text, emotion } as any)
}

// 更新最后一条 agent 消息（用于 SSE 流式追加）
function updateLastAgentMessage(text: string, emotion?: string) {
  if (!selectedRole.value) return
  const msgs = chatHistories.value[selectedRole.value]
  if (!msgs || msgs.length === 0) return
  const last = msgs[msgs.length - 1]
  if (last.sender === 'agent') {
    last.text = text
    if (emotion) (last as any).emotion = emotion
  }
}

async function refreshMembershipStatus() {
  if (!await userStore.ensureBackendToken()) return
  try {
    const res = await $fetch<{ data?: MembershipStatus }>(`${apiBase}/agent/membership/status`, {
      headers: { Authorization: `Bearer ${userStore.backendToken}` },
    })
    membershipStatus.value = res?.data ?? null
  } catch {
    membershipStatus.value = {
      active: false, planCode: 'FREE', planName: '普通番舍成员', priceText: '¥9.9 / 月',
      quotaLimitPerHour: 10, quotaLimitPerDay: 10, voiceEnabled: false, paymentQrUrl: '/images/douyin-qr.png',
    }
  }
}

async function refreshQuotaStatus() {
  if (!await userStore.ensureBackendToken()) return
  try {
    const res = await $fetch<{ data?: QuotaStatus }>(`${apiBase}/agent/quota`, {
      headers: { Authorization: `Bearer ${userStore.backendToken}` },
    })
    if (res?.data) quotaStatus.value = res.data
  } catch {
    // 保留上一次成功读取的额度，网络恢复后会在下一次对话重新同步。
  }
}

function openDonatePopup(message?: string) {
  quotaNotice.value = message || ''
  showDonatePopup.value = true
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !selectedRole.value || sending.value) return

  if (!await userStore.ensureBackendToken()) {
    openAuthModal()
    return
  }

  // 推送用户消息到聊天记录
  pushHistory('user', text)
  inputText.value = ''
  sending.value = true
  streamingText.value = ''

  // 先插入一条空的 agent 消息占位
  pushHistory('agent', '')
  autoScrollToBottom()

  const role = selectedRole.value

  try {
    const response = await fetch(`${apiBase}/agent/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userStore.backendToken}`,
      },
      body: JSON.stringify({ message: text, role }),
    })

    if (!response.ok || !response.body) throw new Error('SSE 连接失败')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let replyText = ''
    let replyEmotion = ''

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
          replyEmotion = payload
          updateLastAgentMessage(replyText, replyEmotion)
        } else if (eventName === 'token' && payload) {
          replyText += payload
          streamingText.value = replyText
          currentReply.value = replyText
          updateLastAgentMessage(replyText, replyEmotion)
          autoScrollToBottom()
        } else if (eventName === 'quota') {
          const limit = parseQuotaLimit(payload)
          openDonatePopup(`${limit} 次对话额度已用光。`)
        }
      }
    }

    if (!replyText.trim()) {
      const res = await $fetch<{ data?: { reply?: string; emotion?: string; rateLimited?: boolean; quotaLimit?: number } }>(`${apiBase}/agent/chat`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${userStore.backendToken}` },
        body: { message: text, role },
      })
      const data = res?.data
      replyText = data?.reply || `（${roleNames.value[role] || role} 轻轻沉默了片刻...）`
      currentReply.value = replyText
      streamingText.value = replyText
      if (data?.emotion) {
        currentEmotion.value = data.emotion as 'happy' | 'normal' | 'complex'
        replyEmotion = data.emotion
      }
      if (data?.rateLimited) openDonatePopup(`${data.quotaLimit || 10} 次对话额度已用光。`)
      updateLastAgentMessage(replyText, replyEmotion)
    }
  } catch {
    const errText = `（${roleNames.value[role] || role} 暂时无法回应...）`
    currentReply.value = errText
    updateLastAgentMessage(errText)
  } finally {
    sending.value = false
    streamingText.value = ''
    void refreshQuotaStatus()
    autoScrollToBottom()
  }
}

function parseQuotaLimit(payload: string) {
  const match = payload.match(/limit=(\d+)/)
  return match ? Number(match[1]) : 10
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════════════
   MOBILE · 微信风格聊天界面
   ═══════════════════════════════════════════════════════════════ */

/* ── 顶栏 ── */
.chat-topbar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
}

.chat-topbar-role {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 0;
  border: 0;
  background: none;
  padding: 0;
  cursor: pointer;
}

.chat-topbar-avatar {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.75rem;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(224, 247, 250, 0.7), rgba(255, 228, 233, 0.4));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chat-topbar-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-topbar-name {
  font-size: 0.85rem;
  font-weight: 900;
  color: #1e293b;
  line-height: 1.3;
}

.chat-topbar-emotion {
  font-size: 0.65rem;
  font-weight: 700;
  color: #0e7490;
}

.chat-topbar-actions {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  flex-shrink: 0;
}

.chat-topbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.65rem;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.5);
  color: #94a3b8;
  transition: all 0.15s ease;
  cursor: pointer;
}

.chat-topbar-btn:active {
  background: rgba(255, 255, 255, 0.8);
  color: #0e7490;
}

.quota-strip-mobile {
  display: grid;
  grid-template-columns: auto minmax(3rem, 1fr) auto;
  align-items: center;
  gap: 0.55rem;
  margin-top: 0.4rem;
  padding: 0.42rem 0.7rem;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 0.85rem;
  background: rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(14px);
  color: #64748b;
  flex-shrink: 0;
}

.quota-strip-copy {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  font-size: 0.66rem;
  font-weight: 800;
}

.quota-strip-copy strong {
  color: #07873f;
  font-size: 0.75rem;
}

.quota-strip-track,
.quota-badge-track {
  display: block;
  height: 0.3rem;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
}

.quota-strip-track > span,
.quota-badge-track > span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #7bc4e0, #00e676);
  transition: width 0.35s ease;
}

.quota-strip-reset {
  font-size: 0.62rem;
  font-weight: 800;
  white-space: nowrap;
  color: #94a3b8;
}

/* ── 搜索栏 ── */
.chat-search-bar {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.6rem;
  margin-top: 0.35rem;
  border-radius: 0.75rem;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
}

/* ── 角色列表面板 ── */
.chat-role-panel {
  margin-top: 0.35rem;
  padding: 0.5rem;
  border-radius: 0.75rem;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
}

.chat-role-scroll {
  display: flex;
  gap: 0.4rem;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  padding-bottom: 0.15rem;
}

.chat-role-scroll::-webkit-scrollbar { display: none; }

.chat-role-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
  flex: 0 0 auto;
  padding: 0.4rem 0.6rem;
  border-radius: 0.75rem;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  transition: all 0.15s ease;
  min-width: 3.25rem;
}

.chat-role-item.active {
  background: rgba(224, 247, 250, 0.7);
  border-color: rgba(160, 216, 239, 0.45);
  box-shadow: 0 2px 8px rgba(160, 216, 239, 0.15);
}

/* ── 聊天气泡区域 ── */
.chat-bubble-area {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0.75rem 0.25rem;
}

/* ── 空状态 ── */
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 2rem 1.5rem;
  text-align: center;
}

.chat-empty-avatar {
  width: 5rem;
  height: 5rem;
  border-radius: 1.25rem;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(224, 247, 250, 0.6), rgba(255, 228, 233, 0.4));
  box-shadow: 0 8px 24px rgba(160, 216, 239, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
  margin-top: 1.25rem;
}

.chat-suggestion-btn {
  padding: 0.5rem 0.85rem;
  border-radius: 1.25rem;
  border: 1px solid rgba(0, 230, 118, 0.25);
  background: rgba(0, 230, 118, 0.06);
  color: #00aa44;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chat-suggestion-btn:active {
  background: rgba(0, 230, 118, 0.15);
}

/* ── 消息行 ── */
.bubble-row {
  display: flex;
  margin-bottom: 0.75rem;
  padding: 0 0.25rem;
}

.bubble-row-user {
  justify-content: flex-end;
}

.bubble-row-agent {
  justify-content: flex-start;
  align-items: flex-start;
  gap: 0.55rem;
}

/* ── AI 表情贴图 ── */
.bubble-sticker {
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(224, 247, 250, 0.6), rgba(255, 228, 233, 0.4));
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 3px 12px rgba(160, 216, 239, 0.15);
}

.bubble-agent-col {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.bubble-name {
  display: block;
  font-size: 0.65rem;
  font-weight: 800;
  color: #94a3b8;
  margin-bottom: 0.15rem;
  margin-left: 0.25rem;
}

/* ── AI 气泡 ── */
.bubble-agent {
  max-width: 80vw;
  padding: 0.6rem 0.8rem;
  border-radius: 0 1rem 1rem 1rem;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  font-size: 0.875rem;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ── 用户气泡 ── */
.bubble-user {
  max-width: 78vw;
  padding: 0.6rem 0.85rem;
  border-radius: 1rem 0 1rem 1rem;
  background: linear-gradient(135deg, #00e676, #00c853);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 2px 12px rgba(0, 230, 118, 0.2);
}

/* ── 正在输入动画 ── */
.typing-dots {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.5rem 0.75rem;
  margin-left: 2.5rem;
}

.typing-dots span {
  width: 0.4rem;
  height: 0.4rem;
  border-radius: 999px;
  background: #94a3b8;
  animation: typingBounce 1.2s ease-in-out infinite;
}

.typing-dots span:nth-child(2) { animation-delay: 0.15s; }
.typing-dots span:nth-child(3) { animation-delay: 0.3s; }

@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.35; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* ── 底部输入栏 ── */
.chat-input-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.5rem;
  padding-bottom: calc(0.4rem + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(253, 251, 247, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  min-width: 0;
  padding: 0.55rem 0.85rem;
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
  font-weight: 600;
  color: #1e293b;
  outline: none;
}

.chat-input::placeholder {
  color: #b0b7c3;
}

.chat-input:focus {
  border-color: rgba(0, 230, 118, 0.4);
  box-shadow: 0 0 0 3px rgba(0, 230, 118, 0.08);
}

.chat-send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999px;
  border: 0;
  background: linear-gradient(135deg, #00e676, #00c853);
  color: #fff;
  box-shadow: 0 4px 16px rgba(0, 230, 118, 0.3);
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.chat-send-btn:disabled {
  opacity: 0.35;
  box-shadow: none;
}

.chat-send-btn:not(:disabled):active {
  transform: scale(0.93);
}

/* ═══ AIGC Shell (Desktop) ═══ */
.aigc-shell {
  display: grid;
  min-height: min(820px, calc(100vh - 6rem));
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 1.25rem;
}

/* ═══ Left Rail ═══ */
.aigc-rail {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow: hidden;
  border-radius: 2rem;
  background: linear-gradient(180deg, rgba(253, 251, 247, 0.9) 0%, rgba(250, 246, 238, 0.85) 100%);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(160, 216, 239, 0.15), 0 2px 8px rgba(255, 192, 203, 0.08);
  padding: 1rem;
}

/* ── Mobile top row (hidden on desktop) ── */
.rail-top-row {
  display: contents;
}

/* ── Mobile actions group (hidden on desktop) ── */
.rail-actions {
  display: contents;
}

/* ── Mobile search toggle (hidden on desktop) ── */
.rail-search-toggle {
  display: none;
}

/* ── Current character ── */
.rail-current {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  border-radius: 1.5rem;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.7);
  padding: 1rem 0.85rem 0.9rem;
}

.rail-avatar {
  width: 5.5rem;
  height: 5.5rem;
  border-radius: 1.5rem;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(224, 247, 250, 0.7), rgba(255, 228, 233, 0.4));
  box-shadow: 0 12px 28px rgba(160, 216, 239, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rail-current-info {
  margin-top: 0.6rem;
}

.rail-name {
  font-size: 0.95rem;
  font-weight: 900;
  color: #1e293b;
  line-height: 1.3;
}

.rail-emotion {
  font-size: 0.72rem;
  font-weight: 700;
  color: #0e7490;
  margin-top: 0.15rem;
}

/* ── Membership ── */
.rail-pass {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.5);
  padding: 0.65rem 0.8rem;
  color: #374151;
  font-size: 0.78rem;
  font-weight: 900;
  transition: background 0.2s ease, transform 0.2s ease;
}

.rail-pass:hover {
  background: rgba(255, 255, 255, 0.72);
  transform: translateY(-1px);
}

.rail-pass small {
  color: #9ca3af;
  font-size: 0.68rem;
  font-weight: 800;
  margin-left: auto;
}

/* ── Search ── */
.rail-search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.5);
  padding: 0.5rem 0.7rem;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 0.8rem;
  font-weight: 700;
  color: #374151;
}

.search-input::placeholder {
  color: #b0b7c3;
}

.search-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 999px;
  background: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
  transition: background 0.15s ease;
}

.search-clear:hover {
  background: rgba(156, 163, 175, 0.35);
}

.search-empty {
  padding: 0.75rem;
  text-align: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: #b0b7c3;
}

/* ── Role list ── */
.rail-role-list {
  display: grid;
  gap: 0.35rem;
  overflow-y: auto;
}

.rail-section-title {
  font-size: 0.68rem;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  color: #94a3b8;
  padding: 0.25rem 0.35rem 0.1rem;
}

.role-btn {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  width: 100%;
  border-radius: 1rem;
  border: 1px solid transparent;
  background: transparent;
  padding: 0.5rem 0.65rem;
  color: #4b5563;
  font-size: 0.82rem;
  font-weight: 800;
  transition: all 0.2s ease;
  cursor: pointer;
}

.role-btn:hover {
  background: rgba(255, 255, 255, 0.65);
  border-color: rgba(255, 255, 255, 0.8);
  color: #0e7490;
}

.role-btn.active {
  background: rgba(224, 247, 250, 0.7);
  border-color: rgba(160, 216, 239, 0.45);
  color: #0e7490;
  box-shadow: 0 4px 14px rgba(160, 216, 239, 0.15);
}

.role-emoji {
  font-size: 1.1rem;
  flex-shrink: 0;
  width: 1.5rem;
  text-align: center;
}

.role-label {
  min-width: 0;
  flex: 1;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ═══ Galgame Stage ═══ */
.galgame-box {
  position: relative;
  min-height: inherit;
  overflow: hidden;
  border-radius: 2.3rem;
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 16px 48px rgba(160, 216, 239, 0.18), 0 4px 16px rgba(255, 192, 203, 0.1);
}

.quota-badge-desktop {
  position: absolute;
  z-index: 6;
  top: 1.25rem;
  right: 1.25rem;
  width: 13.5rem;
  padding: 0.7rem 0.85rem;
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: 1.1rem;
  background: rgba(245, 252, 250, 0.7);
  backdrop-filter: blur(18px);
  box-shadow: 0 10px 28px rgba(31, 68, 47, 0.08);
  color: #64748b;
  text-align: left;
  transition: transform 0.2s ease, background 0.2s ease;
}

.quota-badge-desktop:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.82);
}

.quota-badge-top {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  font-weight: 800;
}

.quota-badge-top strong {
  color: #07873f;
  font-size: 0.86rem;
}

.quota-badge-track {
  margin-top: 0.48rem;
}

.quota-badge-desktop small {
  display: block;
  margin-top: 0.36rem;
  color: #94a3b8;
  font-size: 0.62rem;
  font-weight: 800;
  text-align: right;
}

/* ── Background layer ── */
.galgame-bg-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(ellipse 55% 45% at 25% 15%, rgba(160, 216, 239, 0.22), transparent 50%),
    radial-gradient(ellipse 45% 40% at 75% 55%, rgba(255, 192, 203, 0.14), transparent 50%),
    radial-gradient(ellipse 40% 35% at 50% 85%, rgba(0, 230, 118, 0.06), transparent 50%),
    linear-gradient(175deg, #FDFBF7 0%, #F0F4F8 35%, #FAF6EE 70%, #F5F1E8 100%);
}

/* ── Character portrait ── */
.galgame-portrait {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center bottom;
  filter: drop-shadow(0 8px 32px rgba(31, 68, 47, 0.12));
}

/* ── Overlay washes ── */
.galgame-wash-top {
  position: absolute;
  inset: 0 0 auto 0;
  z-index: 2;
  height: 30%;
  background: linear-gradient(180deg, rgba(253, 251, 247, 0.25), transparent);
  pointer-events: none;
}

.galgame-wash-bottom {
  position: absolute;
  inset: auto 0 0 0;
  z-index: 2;
  height: 40%;
  background: linear-gradient(0deg, rgba(245, 241, 232, 0.55) 0%, rgba(250, 246, 238, 0.25) 40%, transparent 100%);
  pointer-events: none;
}

/* ── Input bar ── */
.dialog-input-row {
  position: absolute;
  z-index: 5;
  right: 1.25rem;
  bottom: 1.25rem;
  left: 1.25rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  padding: 0.35rem;
}

.dialog-input {
  min-width: 0;
  flex: 1;
  background: transparent;
  border: 0;
  outline: 0;
  color: #1e293b;
  font-size: 0.95rem;
  font-weight: 700;
  padding: 0.55rem 0.8rem;
}

.dialog-input::placeholder {
  color: #94a3b8;
}

.dialog-send {
  display: flex;
  width: 2.65rem;
  height: 2.65rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: linear-gradient(135deg, #67e8f9, #00e676);
  color: #fff;
  box-shadow: 0 8px 22px rgba(0, 230, 118, 0.25);
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.dialog-send:not(:disabled):hover {
  transform: scale(1.05);
}

.dialog-send:disabled {
  opacity: 0.4;
}

/* ── Portrait crossfade ── */
.sprite-crossfade-enter-active { transition: opacity 0.4s ease; }
.sprite-crossfade-leave-active { transition: opacity 0.25s ease; }
.sprite-crossfade-enter-from { opacity: 0; }
.sprite-crossfade-leave-to { opacity: 0; }

/* ── Reply box ── */
.galgame-reply-box {
  position: absolute;
  z-index: 4;
  right: 1.25rem;
  bottom: 5.5rem;
  left: 1.25rem;
  max-height: 180px;
  overflow-y: auto;
  border-radius: 1.25rem;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 0.9rem 1.15rem;
}

.reply-character {
  font-size: 0.72rem;
  font-weight: 900;
  color: #0e7490;
  margin-bottom: 0.3rem;
  letter-spacing: 0.05em;
}

.reply-text {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-cursor {
  display: inline-block;
  color: #0e7490;
  font-weight: 400;
  animation: reply-blink 0.8s step-end infinite;
}

@keyframes reply-blink {
  50% { opacity: 0; }
}

/* ── Reply fade transition ── */
.reply-fade-enter-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.reply-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.reply-fade-enter-from { opacity: 0; transform: translateY(6px); }
.reply-fade-leave-to { opacity: 0; transform: translateY(6px); }

/* ═══ Mobile ═══ */
@media (max-width: 768px) {
  /* Shell: single column, auto-height rail + flexible stage */
  .aigc-shell {
    min-height: calc(100vh - 4.5rem);
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
    gap: 0.5rem;
  }

  /* Rail: compact top bar */
  .aigc-rail {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
    padding: 0.5rem 0.6rem;
    border-radius: 1.25rem;
    overflow: visible;
  }

  /* Top row: character | actions */
  .rail-top-row {
    display: flex;
    align-items: center;
    gap: 0.4rem;
  }

  /* Actions group */
  .rail-actions {
    display: flex;
    align-items: center;
    gap: 0.35rem;
    flex-shrink: 0;
  }

  /* Search toggle button */
  .rail-search-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 2.15rem;
    height: 2.15rem;
    border-radius: 0.75rem;
    border: 1px solid rgba(255, 255, 255, 0.7);
    background: rgba(255, 255, 255, 0.5);
    color: #94a3b8;
    transition: all 0.2s ease;
    flex-shrink: 0;
  }

  .rail-search-toggle:active {
    background: rgba(255, 255, 255, 0.75);
    color: #0e7490;
  }

  /* Current character: compact inline */
  .rail-current {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 0.45rem;
    flex: 1;
    min-width: 0;
    padding: 0.3rem 0.5rem;
    border-radius: 1rem;
    background: rgba(255, 255, 255, 0.55);
    border: 1px solid rgba(255, 255, 255, 0.7);
    text-align: left;
  }

  .rail-current-info {
    margin-top: 0;
    min-width: 0;
    overflow: hidden;
  }

  .rail-name {
    font-size: 0.78rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .rail-emotion {
    font-size: 0.6rem;
    margin-top: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .rail-avatar {
    width: 2.25rem;
    height: 2.25rem;
    border-radius: 0.65rem;
    box-shadow: 0 4px 10px rgba(160, 216, 239, 0.15);
  }

  /* Membership: icon only */
  .rail-pass {
    padding: 0.35rem 0.45rem;
    border-radius: 0.75rem;
    gap: 0.3rem;
    flex-shrink: 0;
  }

  .rail-pass span {
    display: none;
  }

  .rail-pass small {
    display: none;
  }

  /* Search: hidden until toggled */
  .rail-search {
    display: none;
    padding: 0.35rem 0.5rem;
    border-radius: 0.75rem;
    gap: 0.35rem;
  }

  .rail-search.rail-search-open {
    display: flex;
  }

  .search-input {
    font-size: 0.75rem;
  }

  /* Role list: horizontal scrolling pills */
  .rail-role-list {
    display: flex;
    flex-direction: row;
    gap: 0.3rem;
    overflow-x: auto;
    overflow-y: hidden;
    max-height: none;
    padding-bottom: 0.15rem;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }

  .rail-role-list::-webkit-scrollbar {
    display: none;
  }

  .rail-section-title {
    display: none;
  }

  /* Role buttons: compact pills */
  .role-btn {
    flex: 0 0 auto;
    width: auto;
    flex-direction: row;
    align-items: center;
    gap: 0.3rem;
    padding: 0.3rem 0.55rem;
    font-size: 0.7rem;
    border-radius: 1.5rem;
    white-space: nowrap;
  }

  .role-emoji {
    font-size: 0.9rem;
    width: auto;
  }

  .role-label {
    flex: 0 0 auto;
    width: auto;
    overflow: visible;
    text-overflow: clip;
    white-space: nowrap;
  }

  /* Search empty state */
  .search-empty {
    padding: 0.35rem 0.5rem;
    font-size: 0.7rem;
    flex: 0 0 auto;
    white-space: nowrap;
  }

  /* Galgame stage: fill remaining viewport */
  .galgame-box {
    min-height: 0;
    border-radius: 1.5rem;
  }

  .galgame-reply-box {
    right: 0.75rem;
    bottom: 4.75rem;
    left: 0.75rem;
    max-height: 140px;
    padding: 0.75rem 0.9rem;
  }

  .dialog-input-row {
    right: 0.75rem;
    bottom: 0.75rem;
    left: 0.75rem;
  }
}

/* ═══ Donate FAB ═══ */
.donate-fab {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 999px;
  border: 1.5px solid rgba(255, 255, 255, 0.7);
  background: linear-gradient(135deg, rgba(253, 251, 247, 0.92), rgba(240, 244, 248, 0.9));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 4px 18px rgba(160, 216, 239, 0.22), 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
}

.donate-fab::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(0, 230, 118, 0.25), rgba(103, 232, 249, 0.2));
  opacity: 0;
  transition: opacity 0.25s ease;
}

.donate-fab:hover::before,
.donate-fab-active::before {
  opacity: 1;
}

.donate-fab:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 24px rgba(160, 216, 239, 0.3), 0 2px 8px rgba(0, 230, 118, 0.15);
}

.donate-fab-active {
  transform: scale(1.06);
  border-color: rgba(0, 230, 118, 0.25);
}

.donate-fab-text {
  font-size: 1.2rem;
  font-weight: 900;
  color: #374151;
  letter-spacing: 0.02em;
  user-select: none;
}

/* ═══ Donate popup transition ═══ */
.donate-popup-enter-active {
  transition: all 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.donate-popup-leave-active {
  transition: all 0.2s ease-in;
}

.donate-popup-enter-from {
  opacity: 0;
  transform: translateY(8px) scale(0.94);
}

.donate-popup-leave-to {
  opacity: 0;
  transform: translateY(4px) scale(0.96);
}
</style>
