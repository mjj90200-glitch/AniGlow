/**
 * POST /api/agent/chat/stream
 * SSE 流式代理 — 不做缓冲，直接透传
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const backendUrl = (config.backendUrl || 'http://localhost:8081').replace(/\/$/, '')
  const target = `${backendUrl}/api/agent/chat/stream`

  const body = await readBody(event)
  let authorization = accessAuthorization(event) || getHeader(event, 'authorization') || undefined

  const send = (auth?: string) => fetch(target, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(auth ? { Authorization: auth } : {}),
    },
    body: JSON.stringify(body),
  })

  let response = await send(authorization)
  if (response.status === 401) {
    const refreshed = await refreshAuthSession(event)
    authorization = refreshed?.token ? `Bearer ${refreshed.token}` : undefined
    if (authorization) response = await send(authorization)
  }

  if (!response.ok || !response.body) {
    throw createError({ statusCode: response.status, message: '后端流式服务不可用' })
  }

  setHeader(event, 'Content-Type', 'text/event-stream')
  setHeader(event, 'Cache-Control', 'no-cache')
  setHeader(event, 'Connection', 'keep-alive')
  setHeader(event, 'X-Accel-Buffering', 'no')

  const nodeRes = event.node.res
  const reader = response.body.getReader()

  nodeRes.on('close', () => reader.cancel().catch(() => {}))

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      nodeRes.write(value)
    }
  } catch {
    // 客户端断开连接
  } finally {
    nodeRes.end()
  }
})
