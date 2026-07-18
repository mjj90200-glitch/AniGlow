/**
 * API 代理 — 将所有 /api/** 请求转发到 Spring Boot 后端 (localhost:8081)
 * 如果后端不可用，返回空数据而非 500 错误，保证前端页面可正常渲染
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const backendUrl = (config.backendUrl || 'http://localhost:8081').replace(/\/$/, '')
  const target = `${backendUrl}${event.path}`

  try {
    // 读取请求体（如果有）
    let body = undefined
    if (event.method !== 'GET' && event.method !== 'HEAD') {
      body = await readBody(event).catch(() => undefined)
    }

    // 读取查询参数
    const query = getQuery(event)

    // 使用 $fetch 转发请求
    const response = await $fetch(target, {
      method: event.method as any,
      headers: {
        'Content-Type': getHeader(event, 'content-type') || 'application/json',
        ...(getHeader(event, 'authorization')
          ? { Authorization: getHeader(event, 'authorization') as string }
          : {}),
        // 从 cookie 读取后端 JWT 并作为 Authorization 转发
        ...(getCookie(event, 'aniglow_backend_token')
          ? { Authorization: `Bearer ${getCookie(event, 'aniglow_backend_token')}` }
          : {}),
      },
      query,
      body,
      // 不吞掉 HTTP 错误状态码，让前端能正确响应 401 等
      ignoreResponseError: false,
      timeout: 300000,
    })

    return response
  } catch (error: any) {
    // 如果后端返回了 HTTP 错误（如 401/403），原样传递给前端
    if (error.data !== undefined || error.status || error.statusCode) {
      setResponseStatus(event, error.status || error.statusCode || 500)
      return error.data
    }

    console.warn(`[API Proxy] ${event.method} ${event.path} → 后端不可用，返回空数据: ${error.message}`)

    // 根据路径返回合适的空数据结构，保证前端渲染不崩溃
    const path = event.path

    if (path.includes('/anime/top-rated') || path.includes('/anime/search') || path.includes('/anime/genre')) {
      return {
        code: 200,
        message: 'OK (offline mode)',
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          pageNumber: 0,
          pageSize: 12,
          last: true,
        },
      }
    }

    if (path.match(/\/anime\/\d+$/)) {
      return {
        code: 200,
        message: 'OK (offline mode)',
        data: null,
      }
    }

    if (path.includes('/anime')) {
      return {
        code: 200,
        message: 'OK (offline mode)',
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          pageNumber: 0,
          pageSize: 20,
          last: true,
        },
      }
    }

    if (path.includes('/ranking')) {
      return {
        code: 200,
        message: 'OK (offline mode)',
        data: [],
      }
    }

    if (path.includes('/agent/roles')) {
      return {
        code: 200,
        message: 'OK (offline mode)',
        data: {
          Makima: '玛奇玛',
          Rem: '蕾姆',
          Onodera: '小野寺',
          Nagisa: '古河渚',
          Frieren: '芙莉莲',
          Kaoruko: '薰子',
        },
      }
    }

    if (path.includes('/agent/characters')) {
      return {
        code: 200,
        message: 'OK (offline mode)',
        data: [
          { code: 'Makima', displayName: '玛奇玛', openingAudioUrl: null },
          { code: 'Rem', displayName: '蕾姆', openingAudioUrl: null },
          { code: 'Onodera', displayName: '小野寺', openingAudioUrl: null },
          { code: 'Nagisa', displayName: '古河渚', openingAudioUrl: null },
          { code: 'Frieren', displayName: '芙莉莲', openingAudioUrl: null },
          { code: 'Kaoruko', displayName: '薰子', openingAudioUrl: null },
        ],
      }
    }

    if (path.includes('/agent')) {
      return {
        code: 200,
        message: 'OK (offline mode)',
        data: null,
      }
    }

    // 默认空响应
    return {
      code: 200,
      message: 'OK (offline mode)',
      data: null,
    }
  }
})
