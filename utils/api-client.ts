export interface ApiEnvelope<T> {
  success: boolean
  message?: string
  data?: T
  timestamp?: string
}

export class ApiClientError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly traceId?: string,
  ) {
    super(message)
    this.name = 'ApiClientError'
  }
}

export function unwrapApiResponse<T>(response: ApiEnvelope<T>, fallback = '请求失败，请稍后重试'): T {
  if (!response || response.success === false) {
    throw new ApiClientError(response?.message || fallback)
  }
  return response.data as T
}

export function resolveApiError(error: any, fallback = '请求失败，请稍后重试'): ApiClientError {
  if (error instanceof ApiClientError) return error
  const message = error?.data?.message || error?.message || fallback
  const status = error?.statusCode || error?.status
  const traceId = error?.response?.headers?.get?.('x-trace-id')
  return new ApiClientError(message, status, traceId || undefined)
}
