export default defineEventHandler(async (event) => {
  enforceRequestRateLimit(event, 'auth-login-ip', 5, 60)
  const body = await readBody(event)
  const username = String(body?.username || '').trim()
  const password = String(body?.password || '')
  if (!username || !password) {
    throw createError({ statusCode: 400, message: '请输入用户名和密码' })
  }

  const backendUrl = String(useRuntimeConfig(event).backendUrl || 'http://localhost:8081').replace(/\/$/, '')
  try {
    const response = await $fetch<any>(`${backendUrl}/api/auth/login`, {
      method: 'POST',
      body: { username, password },
      timeout: 10000,
    })
    setAuthSession(event, response.data)
    return { ...response, data: publicAuthUser(response.data) }
  } catch (error: any) {
    throw createError({
      statusCode: backendStatus(error),
      message: error?.data?.message || '用户名或密码错误',
    })
  }
})

