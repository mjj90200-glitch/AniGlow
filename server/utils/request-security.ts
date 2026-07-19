import { createError, setResponseHeader, type H3Event } from 'h3'
import { isIP } from 'node:net'

interface WindowCounter {
  count: number
  expiresAt: number
}

export class FixedWindowRateLimiter {
  private readonly counters = new Map<string, WindowCounter>()
  private operations = 0

  consume(key: string, limit: number, windowMs: number, now = Date.now()) {
    if (limit <= 0 || windowMs <= 0) throw new Error('限流参数必须为正数')

    const windowNumber = Math.floor(now / windowMs)
    const windowKey = `${key}:${windowNumber}`
    const expiresAt = (windowNumber + 1) * windowMs
    const counter = this.counters.get(windowKey) ?? { count: 0, expiresAt }
    counter.count += 1
    this.counters.set(windowKey, counter)

    this.operations += 1
    if (this.operations % 256 === 0) {
      for (const [candidate, value] of this.counters) {
        if (value.expiresAt <= now) this.counters.delete(candidate)
      }
    }

    return {
      allowed: counter.count <= limit,
      remaining: Math.max(0, limit - counter.count),
      retryAfter: Math.max(1, Math.ceil((expiresAt - now) / 1000)),
    }
  }
}

const requestRateLimiter = new FixedWindowRateLimiter()

export function resolveClientIp(
  remoteAddress: string | undefined,
  forwardedFor: string | undefined,
  configuredProxies: string,
) {
  const remote = normalizeAddress(remoteAddress)
  const trustedProxies = configuredProxies
    .split(',')
    .map(value => value.trim())
    .filter(Boolean)

  if (!isTrustedProxy(remote, trustedProxies) || !forwardedFor) return remote

  const chain = forwardedFor
    .split(',')
    .map(normalizeAddress)
    .filter(address => isIP(address) !== 0)

  for (let index = chain.length - 1; index >= 0; index -= 1) {
    const candidate = chain[index]!
    if (!isTrustedProxy(candidate, trustedProxies)) return candidate
  }
  return chain[0] ?? remote
}

export function requestClientIp(event: H3Event) {
  const config = useRuntimeConfig(event)
  const configuredProxies = String(config.trustedProxies || '127.0.0.1/32,::1/128')
  return resolveClientIp(
    event.node.req.socket.remoteAddress,
    event.node.req.headers['x-forwarded-for'] as string | undefined,
    configuredProxies,
  )
}

export function enforceRequestRateLimit(
  event: H3Event,
  scope: string,
  limit: number,
  windowSeconds: number,
  subject?: string,
) {
  const identity = subject || requestClientIp(event)
  const result = requestRateLimiter.consume(`${scope}:${identity}`, limit, windowSeconds * 1000)

  setResponseHeader(event, 'X-RateLimit-Limit', String(limit))
  setResponseHeader(event, 'X-RateLimit-Remaining', String(result.remaining))
  if (!result.allowed) {
    setResponseHeader(event, 'Retry-After', result.retryAfter)
    throw createError({
      statusCode: 429,
      statusMessage: 'Too Many Requests',
      message: '请求过于频繁，请稍后再试',
    })
  }
}

function normalizeAddress(value?: string) {
  if (!value) return 'unknown'
  const normalized = value.trim().replace(/^\[|\]$/g, '')
  return normalized.startsWith('::ffff:') ? normalized.slice(7) : normalized
}

function isTrustedProxy(address: string, trustedProxies: string[]) {
  return trustedProxies.some(entry => matchesSubnet(address, entry))
}

function matchesSubnet(address: string, entry: string) {
  const [networkValue, rawPrefix] = entry.split('/', 2)
  const network = normalizeAddress(networkValue)
  const addressVersion = isIP(address)
  const networkVersion = isIP(network)
  if (addressVersion === 0 || addressVersion !== networkVersion) return false

  const maxPrefix = addressVersion === 4 ? 32 : 128
  const prefix = rawPrefix === undefined ? maxPrefix : Number(rawPrefix)
  if (!Number.isInteger(prefix) || prefix < 0 || prefix > maxPrefix) return false

  if (addressVersion === 6) {
    return prefix === 128 && address.toLowerCase() === network.toLowerCase()
  }

  const mask = prefix === 0 ? 0 : (0xFFFFFFFF << (32 - prefix)) >>> 0
  return (ipv4ToNumber(address) & mask) === (ipv4ToNumber(network) & mask)
}

function ipv4ToNumber(address: string) {
  return address.split('.').reduce((result, part) => ((result << 8) | Number(part)) >>> 0, 0)
}
