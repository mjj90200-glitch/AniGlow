/**
 * 登录弹窗全局状态 — 任意页面/组件可调用 open() 触发登录
 */
const isOpen = ref(false)

export function useAuthModal() {
  const userStore = useUserStore()

  const open = () => {
    if (userStore.isLoggedIn) return
    isOpen.value = true
  }

  const close = () => {
    isOpen.value = false
  }

  const requireAuth = (action: () => void) => {
    if (userStore.isLoggedIn) {
      action()
    } else {
      open()
    }
  }

  return { isOpen, open, close, requireAuth }
}
