export default defineEventHandler(async (event) => {
  const session = await refreshAuthSession(event)
  if (!session) throw createError({ statusCode: 401, message: '登录已过期，请重新登录' })
  return { code: 200, message: '登录状态已刷新', data: publicAuthUser(session) }
})

