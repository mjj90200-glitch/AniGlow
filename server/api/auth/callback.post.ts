/**
 * POST /api/auth/callback
 * 处理 Authing OIDC 回调 — 用 code 交换 token
 */
export default defineEventHandler(async (event) => {
  enforceRequestRateLimit(event, 'auth-callback-ip', 10, 60)
  const config = useRuntimeConfig(event)
  const body = await readBody(event)

  const code = body?.code
  if (!code) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Bad Request',
      message: '缺少授权码',
    })
  }

  const appId = config.public.authingAppId
  const appSecret = config.authingAppSecret
  const siteUrl = config.public.siteUrl || 'http://localhost:3001'

  if (!appId || !appSecret) {
    throw createError({
      statusCode: 500,
      statusMessage: 'Server Error',
      message: 'Authing 配置缺失，请检查环境变量',
    })
  }

  const redirectUri = `${siteUrl}/login/callback`

  try {
    // 用 authorization code 交换 access token
    const tokenResponse = await $fetch<{
      access_token: string
      id_token: string
      token_type: string
      expires_in: number
    }>('https://core.authing.cn/oidc/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        client_id: appId,
        client_secret: appSecret,
        grant_type: 'authorization_code',
        code,
        redirect_uri: redirectUri,
      }).toString(),
    })

    // 用 access token 获取用户信息
    const userInfo = await $fetch<{
      sub: string
      name: string
      nickname: string
      picture: string
      email: string
      phone_number: string
    }>('https://core.authing.cn/oidc/me', {
      headers: {
        Authorization: `Bearer ${tokenResponse.access_token}`,
      },
    })

    const authingUser = {
      id: userInfo.sub,
      name: userInfo.nickname || userInfo.name || '番舍同好',
      avatar: userInfo.picture,
      email: userInfo.email,
      phone: userInfo.phone_number,
    }

    // 交换后端 JWT
    const backendUrl = (config.backendUrl as string) || 'http://localhost:8081'
    const backendAuth = await exchangeBackendToken(
      authingUser,
      backendUrl,
      String(config.authBridgeSecret || ''),
      requestClientIp(event),
    )

    return {
      token: tokenResponse.id_token || tokenResponse.access_token,
      backendToken: backendAuth?.token || null,
      user: {
        id: String(backendAuth?.userId || authingUser.id),
        authingId: authingUser.id,
        displayName: backendAuth?.displayName || '',
        name: backendAuth?.displayName || '',
        avatarUrl: backendAuth?.avatarUrl || authingUser.avatar,
        avatar: backendAuth?.avatarUrl || authingUser.avatar,
        email: authingUser.email,
        phone: backendAuth?.phone || authingUser.phone,
        profileComplete: backendAuth?.profileComplete ?? false,
      },
    }
  } catch (e: any) {
    console.error('[Auth] Token exchange failed:', e)
    throw createError({
      statusCode: 500,
      statusMessage: 'Auth Failed',
      message: e?.data?.error_description || e?.message || '登录验证失败',
    })
  }
})
