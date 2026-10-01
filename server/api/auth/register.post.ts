export default defineEventHandler(async (event) => {
  enforceRequestRateLimit(event, 'auth-register-ip', 3, 60)
  const config = useRuntimeConfig(event)
  const body = await readBody(event)
  const username = String(body?.username || '').trim()
  const displayName = String(body?.displayName || '').trim()
  const password = String(body?.password || '')
  if (!/^[\p{Script=Han}A-Za-z0-9_]{3,24}$/u.test(username)) {
    throw createError({ statusCode: 400, message: '用户名需为3-24位中文、字母、数字或下划线' })
  }
  if (password.length < 8) throw createError({ statusCode: 400, message: '密码至少8位' })

  try {
    const backendUrl = String(config.backendUrl || 'http://localhost:8081').replace(/\/$/, '')
    const response = await $fetch<any>(`${backendUrl}/api/auth/register`, {
      method: 'POST', body: { username, displayName: displayName || undefined, password }, timeout: 10000,
    })
    setAuthSession(event, response.data)
    return { ...response, data: publicAuthUser(response.data) }
  } catch (error: any) {
    throw createError({
      statusCode: backendStatus(error),
      message: error?.data?.message || '注册失败，请稍后重试',
    })
  }
})
