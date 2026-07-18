/**
 * 客户端会话恢复插件 — 在应用挂载前恢复登录状态
 */
export default defineNuxtPlugin(() => {
  const userStore = useUserStore()

  const getCookie = (name: string): string | null => {
    if (import.meta.server) return null
    const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
    return match ? decodeURIComponent(match[1]) : null
  }

  const token = getCookie('aniglow_token')

  if (token && !userStore.isLoggedIn) {
    try {
      const raw = localStorage.getItem('aniglow_session')
      if (raw) {
        const saved = JSON.parse(raw)
        if (saved && saved.id) {
          userStore.user = saved
          console.log('[Session] 会话已恢复:', saved.name || saved.phone)
          return
        }
      }
    } catch {
      localStorage.removeItem('aniglow_session')
    }
    // localStorage 无数据但 cookie 有效：token 仍保留，但用户需重新获取信息
  }
})
