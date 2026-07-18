import type { ApiEnvelope } from '~/utils/api-client'
import { resolveApiError, unwrapApiResponse } from '~/utils/api-client'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface ApiRequestOptions {
  method?: HttpMethod
  params?: Record<string, unknown>
  body?: unknown
  headers?: Record<string, string>
  auth?: boolean
}

export function useApi() {
  const config = useRuntimeConfig()
  const userStore = useUserStore()
  const base = String(config.public.apiBase || '/api').replace(/\/$/, '')

  const request = async <T>(path: string, options: ApiRequestOptions = {}): Promise<T> => {
    if (options.auth) {
      const ready = await userStore.ensureBackendToken()
      if (!ready) throw resolveApiError(null, '登录状态需要刷新，请重新登录后再操作')
    }

    const headers = { ...options.headers }
    if (userStore.backendToken) {
      headers.Authorization = `Bearer ${userStore.backendToken}`
    }

    try {
      const response = await $fetch<ApiEnvelope<T>>(`${base}${path}`, {
        method: options.method,
        params: options.params,
        body: options.body as any,
        headers,
      })
      return unwrapApiResponse(response)
    } catch (error) {
      throw resolveApiError(error)
    }
  }

  const stream = async (path: string, options: ApiRequestOptions = {}): Promise<Response> => {
    if (options.auth) {
      const ready = await userStore.ensureBackendToken()
      if (!ready) throw resolveApiError(null, '登录状态需要刷新，请重新登录后再操作')
    }
    const headers: Record<string, string> = { 'Content-Type': 'application/json', ...options.headers }
    if (userStore.backendToken) headers.Authorization = `Bearer ${userStore.backendToken}`
    const response = await fetch(`${base}${path}`, {
      method: options.method || 'POST',
      headers,
      body: options.body == null ? undefined : JSON.stringify(options.body),
    })
    if (!response.ok) {
      let message = '请求失败，请稍后重试'
      try { message = (await response.json())?.message || message } catch { /* 非 JSON 错误响应 */ }
      throw resolveApiError({ message, status: response.status, response: { headers: response.headers } })
    }
    return response
  }

  return { request, stream }
}
