/**
 * GET /api/auth/login
 * 重定向到 Authing OIDC 授权页面
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const appId = config.public.authingAppId
  const host = config.public.authingHost || 'https://core.authing.cn'
  const siteUrl = config.public.siteUrl || 'http://localhost:3001'

  const redirectUri = `${siteUrl}/login/callback`

  // Authing OIDC 授权端点
  const authUrl = new URL('https://core.authing.cn/oidc/auth')
  authUrl.searchParams.set('client_id', appId)
  authUrl.searchParams.set('redirect_uri', redirectUri)
  authUrl.searchParams.set('response_type', 'code')
  authUrl.searchParams.set('scope', 'openid profile email phone')
  authUrl.searchParams.set('state', Math.random().toString(36).substring(2, 15))

  await sendRedirect(event, authUrl.toString())
})
