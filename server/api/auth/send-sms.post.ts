/**
 * POST /api/auth/send-sms
 * 发送短信验证码（服务端调用 Authing SDK，避免客户端 Vite 兼容问题）
 */
import { AuthenticationClient } from 'authing-js-sdk'

export default defineEventHandler(async (event) => {
  enforceRequestRateLimit(event, 'auth-sms-ip', 3, 60)
  const config = useRuntimeConfig(event)
  const appId = config.public.authingAppId as string
  const host = (config.public.authingHost as string) || 'https://core.authing.cn'

  if (!appId) {
    throw createError({ statusCode: 500, message: 'Authing 未配置' })
  }

  const body = await readBody(event)
  const phone = body?.phone

  if (!phone || !/^1\d{10}$/.test(phone)) {
    throw createError({ statusCode: 400, message: '手机号格式不正确' })
  }
  enforceRequestRateLimit(event, 'auth-sms-phone', 1, 60, phone)

  try {
    const client = new AuthenticationClient({ appId, appHost: host })
    await client.sendSmsCode(phone)
    return { ok: true }
  } catch (e: any) {
    console.error('[Authing SMS] 发送失败:', e?.message || e)
    throw createError({
      statusCode: 500,
      message: e?.message || '验证码发送失败，请稍后重试',
    })
  }
})
