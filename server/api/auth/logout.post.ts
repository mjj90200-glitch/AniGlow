export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const backendUrl = String(config.backendUrl || 'http://localhost:8081').replace(/\/$/, '')
  const accessToken = getCookie(event, ACCESS_COOKIE) || getCookie(event, 'aniglow_backend_token')
  const refreshToken = getCookie(event, REFRESH_COOKIE)
  try {
    await $fetch(`${backendUrl}/api/auth/logout`, {
      method: 'POST', body: { accessToken, refreshToken }, timeout: 5000,
    })
  } finally {
    clearAuthSession(event)
  }
  return { code: 200, message: '已安全退出', data: null }
})
