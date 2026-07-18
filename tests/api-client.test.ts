import { describe, expect, it } from 'vitest'
import { ApiClientError, resolveApiError, unwrapApiResponse } from '../utils/api-client'

describe('API client helpers', () => {
  it('unwraps successful payloads', () => {
    expect(unwrapApiResponse({ success: true, data: { id: 7 } })).toEqual({ id: 7 })
  })

  it('allows successful empty responses', () => {
    expect(unwrapApiResponse<void>({ success: true })).toBeUndefined()
  })

  it('uses the backend business message', () => {
    expect(() => unwrapApiResponse({ success: false, message: '额度已用完' }))
      .toThrow('额度已用完')
  })

  it('normalizes transport status and trace id', () => {
    const error = resolveApiError({
      statusCode: 503,
      data: { message: '服务暂不可用' },
      response: { headers: { get: () => 'trace-12345678' } },
    })
    expect(error).toMatchObject({ status: 503, traceId: 'trace-12345678', message: '服务暂不可用' })
  })

  it('does not wrap an existing ApiClientError twice', () => {
    const original = new ApiClientError('认证失败', 401)
    expect(resolveApiError(original)).toBe(original)
  })

  it('uses fallback messages for malformed failures', () => {
    expect(() => unwrapApiResponse(null as any, '统一兜底')).toThrow('统一兜底')
    expect(resolveApiError({}, '网络异常')).toMatchObject({ message: '网络异常' })
  })
})
