/**
 * Authing → Spring Boot 后端 JWT 桥接
 * 在 Authing 认证成功后调用，获取后端 JWT 用于 API 鉴权
 */
export async function exchangeBackendToken(
  authingUser: {
    id: string
    name?: string
    avatar?: string
    email?: string
    phone?: string
  },
  backendUrl: string,
  bridgeSecret: string,
  clientIp: string,
): Promise<{
  token: string
  refreshToken: string
  userId: number
  username: string
  displayName?: string
  avatarUrl?: string
  phone?: string
  profileComplete?: boolean
  credentialsInitialized?: boolean
} | null> {
  if (!bridgeSecret) {
    throw new Error('认证桥接密钥未配置')
  }
  try {
    const res: any = await $fetch(`${backendUrl}/api/auth/authing-login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Bridge-Secret': bridgeSecret,
        'X-Forwarded-For': clientIp,
      },
      body: {
        authingId: String(authingUser.id),
        username: authingUser.name || '',
        email: authingUser.email || '',
        phone: authingUser.phone || '',
        avatarUrl: authingUser.avatar || '',
      },
      ignoreResponseError: true,
      timeout: 10000,
    })

    if (res?.data?.token) {
      return {
        token: res.data.token,
        refreshToken: res.data.refreshToken,
        userId: res.data.id,
        username: res.data.username,
        displayName: res.data.displayName,
        avatarUrl: res.data.avatarUrl,
        phone: res.data.phone,
        profileComplete: res.data.profileComplete,
        credentialsInitialized: res.data.credentialsInitialized,
      }
    }
    console.warn('[Auth Bridge] 后端 JWT 交换失败:', res?.message || res)
    return null
  } catch (e: any) {
    console.warn('[Auth Bridge] 后端不可用，跳过 JWT 交换:', e?.message || e)
    return null
  }
}
