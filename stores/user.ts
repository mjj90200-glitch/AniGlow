import { defineStore } from 'pinia'

export interface User {
  id: string
  authingId?: string
  name: string
  avatar?: string
  email?: string
  phone?: string
  profileComplete?: boolean
}

export const useUserStore = defineStore('user', () => {
  // ─── State ─────────────────────────────────────────────────

  const user = ref<User | null>(null)

  // token 存 cookie，30 天有效期保证一周内同设备不退出
  const token = useCookie<string | null>('aniglow_token', {
    sameSite: 'lax',
    path: '/',
    maxAge: 60 * 60 * 24 * 30,
    secure: false,
  })

  // 当前用户在后端的数据库 ID，用于权限判断
  const backendUserId = ref<number | null>(null)

  // 后端 JWT，用于 API 鉴权
  const backendToken = useCookie<string | null>('aniglow_backend_token', {
    sameSite: 'lax',
    path: '/',
    maxAge: 60 * 60 * 24 * 30,
    secure: false,
  })

  const isLoggedIn = computed(() => !!user.value && !!token.value)
  const canUseBackendApi = computed(() => !!user.value && !!backendToken.value)
  const DEFAULT_PROFILE_NAME = '番舍同好'

  const normalizeText = (value?: string | null) => (value || '').trim()

  const isUsableProfileName = (name?: string | null, phone?: string | null) => {
    const value = normalizeText(name)
    const normalizedPhone = normalizeText(phone)
    if (!value || value === DEFAULT_PROFILE_NAME) return false
    if (normalizedPhone && value === normalizedPhone) return false
    if (/^1\d{10}$/.test(value)) return false
    return true
  }

  const resolveStableName = (
    candidate?: string | null,
    phone?: string | null,
    saved?: { name?: string; avatar?: string } | null,
    current?: User | null
  ) => {
    if (isUsableProfileName(candidate, phone)) return normalizeText(candidate)
    if (isUsableProfileName(saved?.name, phone)) return normalizeText(saved?.name)
    if (isUsableProfileName(current?.name, phone)) return normalizeText(current?.name)
    return ''
  }

  const resolveStableAvatar = (
    candidate?: string | null,
    saved?: { name?: string; avatar?: string } | null,
    current?: User | null
  ) => normalizeText(candidate) || normalizeText(saved?.avatar) || normalizeText(current?.avatar) || ''

  // ─── 登录状态恢复（刷新页面时从 localStorage 读取） ──────

  const SESSION_KEY = 'aniglow_session'

  const saveSession = (u: User) => {
    if (import.meta.server) return
    try {
      localStorage.setItem(SESSION_KEY, JSON.stringify(u))
    } catch { /* 静默 */ }
  }

  const loadSession = (): User | null => {
    if (import.meta.server) return null
    try {
      const raw = localStorage.getItem(SESSION_KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  const clearSession = () => {
    if (import.meta.server) return
    try {
      localStorage.removeItem(SESSION_KEY)
    } catch { /* 静默 */ }
  }

  /**
   * 初始化时尝试恢复登录状态
   * 条件：cookie 中有 token 且 localStorage 中有用户数据
   */
  const restoreSession = () => {
    if (user.value) return // 已登录，不覆盖
    if (!token.value) return // 无 token，跳过

    const saved = loadSession()
    if (saved) {
      user.value = saved
      console.log('[Session] 已恢复登录:', saved.name || saved.phone)
    }
  }

  // ─── Computed ─────────────────────────────────────────────

  // 资料编辑改为用户主动触发：登录只恢复账号，不再因为后端 profileComplete=false 强制弹窗。
  const isNewUser = computed(() => false)

  // ─── Actions ──────────────────────────────────────────────

  const login = async (email: string, password: string) => {
    const u: User = {
      id: '1',
      name: '动漫迷',
      avatar: '',
      email,
      profileComplete: true,
    }
    user.value = u
    saveSession(u)
  }

  const loginWithAuthing = (authingUser: any, authingToken?: string) => {
    const resolvedToken = authingToken
      || authingUser?.token
      || authingUser?.accessToken
      || authingUser?.idToken

    token.value = resolvedToken ?? null

    // 保存后端 JWT
    const resolvedBackendToken = (authingUser as any)?.backendToken || null
    if (resolvedBackendToken) {
      backendToken.value = resolvedBackendToken
    }

    const rawPhone = authingUser?.phone || authingUser?.phone_number || ''
    const rawName = authingUser?.displayName || authingUser?.nickname || authingUser?.name || authingUser?.username || ''
    const rawAvatar = authingUser?.avatarUrl || authingUser?.photo || authingUser?.avatar || authingUser?.picture || ''

    const savedProfile = loadLocalProfile(rawPhone || String(authingUser?.id || ''))
    const stableName = resolveStableName(rawName, rawPhone, savedProfile, user.value)
    const stableAvatar = resolveStableAvatar(rawAvatar, savedProfile, user.value)

    const u: User = {
      id: String(authingUser?.id || authingUser?._id || authingUser?.userId || 'authing-user'),
      authingId: authingUser?.authingId ? String(authingUser.authingId) : undefined,
      name: stableName,
      avatar: stableAvatar,
      email: authingUser?.email,
      phone: rawPhone,
      profileComplete: authingUser?.profileComplete === true || isUsableProfileName(stableName, rawPhone),
    }

    user.value = u
    saveSession(u)
  }

  const completeProfile = async (profileData: { name: string; avatar: string }) => {
    if (!user.value) return
    user.value.name = profileData.name
    user.value.avatar = profileData.avatar
    user.value.profileComplete = true

    const key = user.value.phone || user.value.id
    saveLocalProfile(key, { name: profileData.name, avatar: profileData.avatar })
    saveSession(user.value)

    try {
      const apiReady = await ensureBackendToken()
      if (!apiReady) return

      const res = await $fetch<{
        data?: {
          displayName?: string
          avatarUrl?: string
          phone?: string
          profileComplete?: boolean
        }
      }>('/api/auth/profile', {
        method: 'PUT',
        body: {
          displayName: profileData.name,
          avatarUrl: profileData.avatar,
        },
      })

      if (res?.data && user.value) {
        user.value = {
          ...user.value,
          name: res.data.displayName || profileData.name,
          avatar: res.data.avatarUrl || profileData.avatar,
          phone: res.data.phone || user.value.phone,
          profileComplete: res.data.profileComplete ?? true,
        }
        saveSession(user.value)
      }
    } catch (e) {
      console.warn('[Profile] 后端资料同步失败，已保留本地资料:', e)
    }
  }

  const logout = () => {
    user.value = null
    token.value = null
    backendToken.value = null
    clearSession()
  }

  const updateProfile = (data: Partial<User>) => {
    if (!user.value) return
    user.value = { ...user.value, ...data }
    const key = user.value.phone || user.value.id
    saveLocalProfile(key, { name: user.value.name, avatar: user.value.avatar || '' })
    saveSession(user.value)
  }

  const ensureBackendToken = async () => {
    // 检查已有 token 是否过期（JWT 内嵌 exp 字段）
    if (backendToken.value) {
      try {
        const payload = JSON.parse(atob(backendToken.value.split('.')[1]))
        const now = Math.floor(Date.now() / 1000)
        if (payload.exp && payload.exp > now + 60) {
          return true // token 至少在 1 分钟内有效
        }
        // token 已过期或即将过期，清除
        backendToken.value = null
      } catch {
        // 无法解析则视为无效，清除后重新获取
        backendToken.value = null
      }
    }

    if (!user.value || !token.value) return false

    try {
      const res = await $fetch<{
        backendToken?: string
        backendUserId?: number
        username?: string
        displayName?: string
        avatarUrl?: string
        phone?: string
        profileComplete?: boolean
      }>('/api/auth/backend-token', {
        method: 'POST',
        body: { user: user.value },
      })

      if (!res?.backendToken) return false
      backendToken.value = res.backendToken
      if (res.backendUserId) {
        backendUserId.value = res.backendUserId
      }
      if (user.value) {
        const stableName = resolveStableName(res.displayName, res.phone || user.value.phone, null, user.value)
        const stableAvatar = resolveStableAvatar(res.avatarUrl, null, user.value)
        user.value = {
          ...user.value,
          name: stableName || user.value.name,
          avatar: stableAvatar || user.value.avatar,
          phone: res.phone || user.value.phone,
          profileComplete: res.profileComplete === true || user.value.profileComplete,
        }
        saveSession(user.value)
      }
      return true
    } catch (e) {
      console.warn('[Session] 后端登录态恢复失败:', e)
      return false
    }
  }

  // ─── localStorage 工具 ────────────────────────────────────

  const PROFILE_PREFIX = 'aniglow_profile_'

  const loadLocalProfile = (key: string): { name: string; avatar: string } | null => {
    if (import.meta.server) return null
    try {
      const raw = localStorage.getItem(PROFILE_PREFIX + key)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  const saveLocalProfile = (key: string, data: { name: string; avatar: string }) => {
    if (import.meta.server) return
    try {
      localStorage.setItem(PROFILE_PREFIX + key, JSON.stringify(data))
    } catch { /* 静默 */ }
  }

  return {
    user,
    token,
    backendToken,
    backendUserId,
    isLoggedIn,
    canUseBackendApi,
    isNewUser,
    login,
    loginWithAuthing,
    restoreSession,
    completeProfile,
    logout,
    updateProfile,
    ensureBackendToken,
  }
})
