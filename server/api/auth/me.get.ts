/**
 * GET /api/auth/me
 * 获取当前登录用户信息
 */
export default defineEventHandler(async (event) => {
  // 从请求头获取 token
  const authHeader = getHeader(event, 'authorization')
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Unauthorized',
      message: '未登录',
    })
  }

  const token = authHeader.replace('Bearer ', '')

  try {
    // 调用 Authing userinfo 端点验证 token
    const userInfo = await $fetch<{
      sub: string
      name: string
      nickname: string
      picture: string
      email: string
      phone_number: string
    }>('https://core.authing.cn/oidc/me', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    return {
      id: userInfo.sub,
      name: userInfo.nickname || userInfo.name || '番舍同好',
      avatar: userInfo.picture,
      email: userInfo.email,
      phone: userInfo.phone_number,
    }
  } catch (e: any) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Unauthorized',
      message: '登录已过期，请重新登录',
    })
  }
})
