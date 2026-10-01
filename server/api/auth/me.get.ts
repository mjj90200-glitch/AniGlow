import type { H3Event } from 'h3'

async function fetchCurrentUser(event: H3Event, authorization: string): Promise<any> {
  const backendUrl = String(useRuntimeConfig(event).backendUrl || 'http://localhost:8081').replace(/\/$/, '')
  return $fetch<any>(`${backendUrl}/api/auth/me`, {
    headers: { Authorization: authorization },
    timeout: 10000,
  })
}

export default defineEventHandler(async (event) => {
  let authorization = accessAuthorization(event)
  if (!authorization) {
    const refreshed = await refreshAuthSession(event)
    if (!refreshed?.token) throw createError({ statusCode: 401, message: '未登录' })
    authorization = `Bearer ${refreshed.token}`
  }

  try {
    const response: any = await fetchCurrentUser(event, authorization)
    return { ...response, data: publicAuthUser(response.data) }
  } catch (error: any) {
    if (backendStatus(error) === 401) {
      const refreshed = await refreshAuthSession(event)
      if (refreshed?.token) {
        const response: any = await fetchCurrentUser(event, `Bearer ${refreshed.token}`)
        return { ...response, data: publicAuthUser(response.data) }
      }
    }
    throw createError({ statusCode: backendStatus(error), message: error?.data?.message || '登录已过期' })
  }
})
