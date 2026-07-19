/**
 * POST /api/auth/backend-token
 * 使用当前前端会话里的用户信息补发 Spring Boot 后端 JWT。
 *
 * 这个接口用于兼容旧会话：用户前端仍显示已登录，但浏览器里缺少
 * aniglow_backend_token 时，评论、投票等后端鉴权接口会失败。
 */
import { AuthenticationClient } from 'authing-js-sdk'

export default defineEventHandler(async (event) => {
  enforceRequestRateLimit(event, 'auth-backend-token-ip', 5, 60)
  const config = useRuntimeConfig(event)
  const cookieToken = getCookie(event, 'aniglow_token')
  const authorization = getHeader(event, 'authorization')
  const token = cookieToken || authorization?.replace(/^Bearer\s+/i, '')

  if (!token) {
    throw createError({
      statusCode: 401,
      message: 'Authing 登录凭证缺失，请重新登录',
    })
  }

  const appId = String(config.public.authingAppId || '')
  const host = String(config.public.authingHost || 'https://core.authing.cn')
  if (!appId) throw createError({ statusCode: 500, message: 'Authing 未配置' })

  let verifiedUser: any
  try {
    const client = new AuthenticationClient({ appId, appHost: host, token })
    const loginStatus: any = await client.checkLoginStatus(token)
    if (!loginStatus?.status) throw new Error('Authing Token 已失效')
    verifiedUser = await client.getCurrentUser()
  } catch (error: any) {
    console.warn('[Auth Bridge] Authing Token 校验失败:', error?.message || error)
    throw createError({ statusCode: 401, message: '登录凭证已失效，请重新登录' })
  }

  const authingId = verifiedUser?.id || verifiedUser?._id || verifiedUser?.userId
  if (!authingId) throw createError({ statusCode: 401, message: 'Authing 用户信息无效' })

  const backendUrl = String(config.backendUrl || 'http://localhost:8081')
  const backendAuth = await exchangeBackendToken({
    id: String(authingId),
    name: verifiedUser?.nickname || verifiedUser?.name || verifiedUser?.username || '',
    avatar: verifiedUser?.photo || verifiedUser?.avatar || verifiedUser?.picture || '',
    email: verifiedUser?.email || '',
    phone: verifiedUser?.phone || '',
  }, backendUrl, String(config.authBridgeSecret || ''), requestClientIp(event))

  if (!backendAuth?.token) {
    throw createError({
      statusCode: 401,
      message: '后端登录态同步失败，请重新登录',
    })
  }

  return {
    backendToken: backendAuth.token,
    backendUserId: backendAuth.userId,
    username: backendAuth.username,
    displayName: backendAuth.displayName,
    avatarUrl: backendAuth.avatarUrl,
    phone: backendAuth.phone,
    profileComplete: backendAuth.profileComplete,
  }
})
