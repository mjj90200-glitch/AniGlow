import { defineStore } from 'pinia'

export interface User {
  id: string
  username: string
  name: string
  avatar?: string
  email?: string
  phone?: string
  profileComplete?: boolean
  credentialsInitialized?: boolean
  roles?: string[]
}

interface AuthEnvelope {
  data?: User
  message?: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const restoring = ref(false)
  let restoreRequest: Promise<boolean> | null = null

  const isLoggedIn = computed(() => Boolean(user.value))
  const canUseBackendApi = computed(() => Boolean(user.value))
  const backendUserId = computed(() => user.value ? Number(user.value.id) : null)
  const isNewUser = computed(() => false)
  const needsCredentials = computed(() => user.value?.credentialsInitialized === false)

  function setSession(nextUser: User | null) {
    user.value = nextUser
  }

  async function login(username: string, password: string) {
    const response = await $fetch<AuthEnvelope>('/api/auth/login', {
      method: 'POST', body: { username, password },
    })
    if (!response.data) throw new Error(response.message || '登录失败')
    setSession(response.data)
    return response.data
  }

  async function register(username: string, password: string, displayName?: string) {
    const response = await $fetch<AuthEnvelope>('/api/auth/register', {
      method: 'POST', body: { username, password, displayName },
    })
    if (!response.data) throw new Error(response.message || '注册失败')
    setSession(response.data)
    return response.data
  }

  async function restoreSession() {
    if (user.value) return true
    if (restoreRequest) return restoreRequest
    restoring.value = true
    restoreRequest = $fetch<AuthEnvelope>('/api/auth/me')
      .then((response) => {
        setSession(response.data || null)
        return Boolean(response.data)
      })
      .catch(() => {
        setSession(null)
        return false
      })
      .finally(() => {
        restoring.value = false
        restoreRequest = null
      })
    return restoreRequest
  }

  async function ensureBackendToken() {
    return user.value ? true : restoreSession()
  }

  async function completeProfile(profileData: { name: string; avatar: string }) {
    if (!await ensureBackendToken()) return
    const response = await $fetch<AuthEnvelope>('/api/auth/profile', {
      method: 'PUT',
      body: { displayName: profileData.name, avatarUrl: profileData.avatar },
    })
    if (response.data) {
      const data = response.data as User & { displayName?: string; avatarUrl?: string }
      setSession({
        ...user.value!,
        ...data,
        name: data.name || data.displayName || user.value!.name,
        avatar: data.avatar || data.avatarUrl || '',
      })
    }
  }

  async function setCredentials(username: string, password: string) {
    const response = await $fetch<AuthEnvelope>('/api/auth/set-credentials', {
      method: 'POST', body: { username, password },
    })
    if (!response.data) throw new Error(response.message || '设置登录信息失败')
    setSession({ ...user.value!, ...response.data, credentialsInitialized: true })
  }

  async function logout() {
    try {
      await $fetch('/api/auth/logout', { method: 'POST' })
    } finally {
      setSession(null)
    }
  }

  function updateProfile(data: Partial<User>) {
    if (user.value) setSession({ ...user.value, ...data })
  }

  // 仅供旧 Authing 回调迁移期间使用，新的登录入口不会调用它。
  function loginWithAuthing(authingUser: any) {
    setSession({
      id: String(authingUser?.id || authingUser?.userId || ''),
      username: authingUser?.username || String(authingUser?.id || ''),
      name: authingUser?.displayName || authingUser?.name || authingUser?.username || '番舍同好',
      avatar: authingUser?.avatarUrl || authingUser?.avatar || '',
      email: authingUser?.email,
      phone: authingUser?.phone,
      profileComplete: authingUser?.profileComplete,
      credentialsInitialized: authingUser?.credentialsInitialized ?? false,
    })
  }

  return {
    user,
    restoring,
    backendUserId,
    isLoggedIn,
    canUseBackendApi,
    isNewUser,
    needsCredentials,
    setSession,
    login,
    register,
    loginWithAuthing,
    restoreSession,
    completeProfile,
    setCredentials,
    logout,
    updateProfile,
    ensureBackendToken,
  }
})
