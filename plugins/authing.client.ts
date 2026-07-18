/**
 * Authing 插件 — 提供自定义登录表单所需的环境配置
 * 登录 UI 已改用 components/LoginForm.vue，不再依赖 @authing/native-js-ui-components Guard
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const appId = config.public.authingAppId as string

  if (!appId) {
    console.warn('[Authing] NUXT_PUBLIC_AUTHING_APP_ID 未配置，请在 .env 中设置。')
  } else {
    console.log('[Authing] 插件就绪，使用自定义登录表单, appId:', appId)
  }

  return {
    provide: {
      authingReady: !!appId,
    },
  }
})
