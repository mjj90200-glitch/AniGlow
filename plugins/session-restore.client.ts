export default defineNuxtPlugin(async () => {
  const userStore = useUserStore()
  if (await userStore.restoreSession()) return

  // 旧 Authing Cookie 仅用于一次性无短信迁移，成功后服务端会清除旧 Cookie。
  try {
    await $fetch('/api/auth/backend-token', { method: 'POST' })
    await userStore.restoreSession()
  } catch {
    // 新访客没有旧会话时会走到这里，无需打扰用户。
  }
})
