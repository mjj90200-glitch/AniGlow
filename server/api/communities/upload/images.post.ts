/**
 * 图片上传专用代理 — 转发 multipart/form-data 到 Spring Boot 后端
 * 必须以 Buffer 读取原始二进制数据，并保留 Content-Type 中的 boundary
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const backendUrl = (config.backendUrl || 'http://localhost:8081').replace(/\/$/, '')
  const target = `${backendUrl}${event.path}`

  const contentType = getHeader(event, 'content-type') || ''
  let authorization = accessAuthorization(event) || getHeader(event, 'authorization') || undefined
  const rawBody = await readRawBody(event, false)

  try {
    const send = (auth?: string) => fetch(target, {
      method: 'POST',
      headers: {
        'Content-Type': contentType,
        ...(auth ? { Authorization: auth } : {}),
      },
      body: rawBody as any,
    })
    let response = await send(authorization)
    if (response.status === 401) {
      const refreshed = await refreshAuthSession(event)
      authorization = refreshed?.token ? `Bearer ${refreshed.token}` : undefined
      if (authorization) response = await send(authorization)
    }

    const responseText = await response.text()
    if (!response.ok) {
      setResponseStatus(event, response.status)
    }

    try {
      return JSON.parse(responseText)
    } catch {
      return responseText
    }
  } catch (error: any) {
    if (error.data !== undefined || error.status || error.statusCode) {
      setResponseStatus(event, error.status || error.statusCode || 500)
      return error.data
    }
    console.warn(`[Upload Proxy] 后端不可用: ${error.message}`)
    return {
      success: false,
      message: '图片上传服务暂时不可用，请稍后再试',
      data: null,
    }
  }
})
