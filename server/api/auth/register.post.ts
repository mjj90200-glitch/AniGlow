/**
 * POST /api/auth/register
 * 手机号 + 短信验证码 + 密码 注册（仅注册时消耗短信额度）
 */
import { AuthenticationClient } from 'authing-js-sdk'

export default defineEventHandler(async (event) => {
  enforceRequestRateLimit(event, 'auth-register-ip', 3, 60)
  const config = useRuntimeConfig(event)
  const appId = config.public.authingAppId as string
  const host = (config.public.authingHost as string) || 'https://core.authing.cn'

  if (!appId) throw createError({ statusCode: 500, message: 'Authing 未配置' })

  const body = await readBody(event)
  const phone = body?.phone
  const code = body?.code
  const password = body?.password

  if (!phone || !/^1\d{10}$/.test(phone)) throw createError({ statusCode: 400, message: '手机号格式不正确' })
  if (!code || code.length < 4) throw createError({ statusCode: 400, message: '验证码不能为空' })
  if (!password || password.length < 6) throw createError({ statusCode: 400, message: '密码至少6位' })
  enforceRequestRateLimit(event, 'auth-register-phone', 3, 600, phone)

  try {
    const client = new AuthenticationClient({ appId, appHost: host })
    const result: any = await client.registerByPhoneCode(phone, code, password)

    const authingUser = {
      id: result?.id || result?._id || result?.userId,
      name: result?.nickname || result?.name || result?.username || '',
      avatar: result?.photo || result?.avatar || result?.picture || '',
      email: result?.email || '',
      phone,
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
      token: result?.token || result?.accessToken || result?.idToken,
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
    console.error('[Authing] 注册失败:', e?.message || e)
    throw createError({ statusCode: 400, message: e?.message || '注册失败，请检查验证码' })
  }
})
