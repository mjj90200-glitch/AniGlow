/**
 * POST /api/auth/backend-token
 * 使用当前前端会话里的用户信息补发 Spring Boot 后端 JWT。
 *
 * 这个接口用于兼容旧会话：用户前端仍显示已登录，但浏览器里缺少
 * aniglow_backend_token 时，评论、投票等后端鉴权接口会失败。
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const body = await readBody(event)
  const user = body?.user ?? {}

  const authingId = user.authingId || user._id || user.userId || user.phone || user.id
  if (!authingId) {
    throw createError({
      statusCode: 401,
      message: '登录信息不完整，请重新登录',
    })
  }

  const backendUrl = (config.backendUrl as string) || 'http://localhost:8081'
  const backendAuth = await exchangeBackendToken({
    id: String(authingId),
    name: user.name || user.username || user.phone || '',
    avatar: user.avatar || user.avatarUrl || '',
    email: user.email || '',
    phone: user.phone || '',
  }, backendUrl)

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
