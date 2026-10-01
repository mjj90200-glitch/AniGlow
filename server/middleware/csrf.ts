const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

export default defineEventHandler((event) => {
  if (SAFE_METHODS.has(event.method) || !event.path.startsWith('/api/')) return

  const fetchSite = getHeader(event, 'sec-fetch-site')
  if (fetchSite === 'cross-site') {
    throw createError({ statusCode: 403, message: '已拒绝跨站请求' })
  }

  const origin = getHeader(event, 'origin')
  if (!origin) return

  const config = useRuntimeConfig(event)
  const configuredOrigin = safeOrigin(String(config.public.siteUrl || ''))
  const host = getHeader(event, 'x-forwarded-host') || getHeader(event, 'host')
  const protocol = getHeader(event, 'x-forwarded-proto') || 'http'
  const requestOrigin = host ? `${protocol}://${host}` : ''
  if (origin !== configuredOrigin && origin !== requestOrigin) {
    throw createError({ statusCode: 403, message: '请求来源验证失败' })
  }
})

function safeOrigin(value: string) {
  try {
    return new URL(value).origin
  } catch {
    return ''
  }
}
