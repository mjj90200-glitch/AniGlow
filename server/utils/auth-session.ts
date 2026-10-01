import type { H3Event } from 'h3'

export const ACCESS_COOKIE = 'aniglow_access_token'
export const REFRESH_COOKIE = 'aniglow_refresh_token'

interface BackendAuthData {
  token?: string
  refreshToken?: string
  id: number
  username: string
  displayName?: string
  avatarUrl?: string
  phone?: string
  email?: string
  profileComplete?: boolean
  credentialsInitialized?: boolean
  roles?: string[]
}

export function publicAuthUser(data: BackendAuthData) {
  return {
    id: String(data.id),
    username: data.username,
    name: data.displayName || data.username,
    avatar: data.avatarUrl || '',
    phone: data.phone || undefined,
    email: data.email || undefined,
    profileComplete: data.profileComplete ?? false,
    credentialsInitialized: data.credentialsInitialized ?? true,
    roles: data.roles || [],
  }
}

function cookieOptions(event: H3Event, maxAge: number) {
  const siteUrl = String(useRuntimeConfig(event).public.siteUrl || '')
  return {
    httpOnly: true,
    sameSite: 'lax' as const,
    secure: siteUrl.startsWith('https://'),
    path: '/',
    maxAge,
  }
}

export function setAuthSession(event: H3Event, data: BackendAuthData) {
  if (!data.token || !data.refreshToken) {
    throw createError({ statusCode: 502, message: '认证服务未返回完整登录凭证' })
  }
  setCookie(event, ACCESS_COOKIE, data.token, cookieOptions(event, 15 * 60))
  setCookie(event, REFRESH_COOKIE, data.refreshToken, cookieOptions(event, 7 * 24 * 60 * 60))
  deleteCookie(event, 'aniglow_backend_token', cookieOptions(event, 0))
  deleteCookie(event, 'aniglow_token', cookieOptions(event, 0))
}

export function clearAuthSession(event: H3Event) {
  deleteCookie(event, ACCESS_COOKIE, cookieOptions(event, 0))
  deleteCookie(event, REFRESH_COOKIE, cookieOptions(event, 0))
  deleteCookie(event, 'aniglow_backend_token', cookieOptions(event, 0))
  deleteCookie(event, 'aniglow_token', cookieOptions(event, 0))
}

export function accessAuthorization(event: H3Event) {
  const token = getCookie(event, ACCESS_COOKIE) || getCookie(event, 'aniglow_backend_token')
  return token ? `Bearer ${token}` : undefined
}

export async function refreshAuthSession(event: H3Event): Promise<BackendAuthData | null> {
  const refreshToken = getCookie(event, REFRESH_COOKIE)
  if (!refreshToken) return null

  const config = useRuntimeConfig(event)
  const backendUrl = String(config.backendUrl || 'http://localhost:8081').replace(/\/$/, '')
  try {
    const response = await $fetch<{ data?: BackendAuthData }>(`${backendUrl}/api/auth/refresh`, {
      method: 'POST',
      body: { refreshToken },
      timeout: 10000,
    })
    if (!response.data?.token || !response.data.refreshToken) return null
    setAuthSession(event, response.data)
    return response.data
  } catch {
    clearAuthSession(event)
    return null
  }
}

export function backendStatus(error: any) {
  return Number(error?.statusCode || error?.status || error?.response?.status || 500)
}
