import { describe, expect, it } from 'vitest'
import { FixedWindowRateLimiter, resolveClientIp } from '../server/utils/request-security'

describe('server request security', () => {
  it('blocks requests after the configured fixed-window allowance', () => {
    const limiter = new FixedWindowRateLimiter()
    expect(limiter.consume('login:client', 2, 60_000, 1_000).allowed).toBe(true)
    expect(limiter.consume('login:client', 2, 60_000, 1_000).allowed).toBe(true)
    expect(limiter.consume('login:client', 2, 60_000, 1_000)).toMatchObject({
      allowed: false,
      remaining: 0,
    })
  })

  it('starts a fresh allowance in the next window', () => {
    const limiter = new FixedWindowRateLimiter()
    expect(limiter.consume('sms:client', 1, 60_000, 59_999).allowed).toBe(true)
    expect(limiter.consume('sms:client', 1, 60_000, 60_000).allowed).toBe(true)
  })

  it('uses forwarded addresses only when the direct peer is trusted', () => {
    const proxies = '127.0.0.1/32,10.0.0.0/24'
    expect(resolveClientIp('127.0.0.1', '198.51.100.7, 10.0.0.8', proxies))
      .toBe('198.51.100.7')
    expect(resolveClientIp('203.0.113.9', '198.51.100.7', proxies))
      .toBe('203.0.113.9')
  })

  it('ignores malformed forwarded values', () => {
    expect(resolveClientIp('127.0.0.1', 'spoofed-host, 198.51.100.8', '127.0.0.1/32'))
      .toBe('198.51.100.8')
  })
})
