export default defineEventHandler(async (event) => {
  const authorization = accessAuthorization(event)
  if (!authorization) throw createError({ statusCode: 401, message: '登录已过期，请重新登录' })
  const backendUrl = String(useRuntimeConfig(event).backendUrl || 'http://localhost:8081').replace(/\/$/, '')
  const body = await readBody(event)
  try {
    const response = await $fetch<any>(`${backendUrl}/api/auth/set-credentials`, {
      method: 'POST', headers: { Authorization: authorization }, body, timeout: 10000,
    })
    setAuthSession(event, response.data)
    return { ...response, data: publicAuthUser(response.data) }
  } catch (error: any) {
    throw createError({ statusCode: backendStatus(error), message: error?.data?.message || '设置登录信息失败' })
  }
})
