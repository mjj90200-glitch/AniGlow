export async function useAgentChatPage() {
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
const { request, stream } = useApi()

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
interface AgentCharacterConfig {
  code: string
  displayName: string
  sourceTitle?: string | null
  openingAudioUrl?: string | null
}

const { data: charactersData } = await useAsyncData('agent-characters', async () => {
  try {
    const data = await request<AgentCharacterConfig[]>('/agent/characters')
    return data?.length ? data : DEFAULT_CHARACTERS
  } catch { return DEFAULT_CHARACTERS }
})

const currentCharacterSource = computed(() => {
  const chars = charactersData.value ?? []
  const found = chars.find(c => c.code === selectedRole.value)
  return found?.sourceTitle || ''
})

const { data: rolesData } = await useAsyncData('agent-roles', async () => {
  try {
    const data = await request<Record<string, string>>('/agent/roles')
    return data && Object.keys(data).length > 0 ? data : DEFAULT_ROLES
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
  try {
    membershipStatus.value = await request<MembershipStatus>('/agent/membership/status', { auth: true })
  } catch {
    membershipStatus.value = {
      active: false, planCode: 'FREE', planName: '普通番舍成员', priceText: '¥9.9 / 月',
      quotaLimitPerHour: 10, quotaLimitPerDay: 10, voiceEnabled: false, paymentQrUrl: '/images/douyin-qr.png',
    }
  }
}

async function refreshQuotaStatus() {
  try {
    quotaStatus.value = await request<QuotaStatus>('/agent/quota', { auth: true })
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
    const response = await stream('/agent/chat/stream', {
      method: 'POST', auth: true, body: { message: text, role },
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
      const data = await request<{ reply?: string; emotion?: string; rateLimited?: boolean; quotaLimit?: number }>('/agent/chat', {
        method: 'POST', auth: true, body: { message: text, role },
      })
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
  return {
    mainRoles, allRoleKeys, roleEmojis, selectedRole, inputText, sending, searchText,
    showMobileSearch, showMobileRoles, streamingText, inputRef, searchInputRef, chatContainerRef,
    currentEmotion, currentReply, hasMessages, currentMessages, roleGreetings, suggestTopics,
    quotaStatus, quotaPercent, quotaResetLabel, membershipStatus, showDonatePopup, donateTitle,
    douyinQrUrl, roles, roleNames, currentCharacterSource, emotionLabel, filteredSearchResults,
    portraitSrc, hasImage, stickerSrc, onSmallAvatarError, onPortraitError, selectSearchResult,
    selectRole, sendSuggestion, sendMessage, openDonatePopup,
  }
}
