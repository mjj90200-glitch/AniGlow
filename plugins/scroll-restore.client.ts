export default defineNuxtPlugin({
  name: 'scroll-restore',
  enforce: 'post',
  setup(nuxtApp) {
    if (import.meta.server) return

    const KEY = 'aniglow_anime_list_scroll_y'
    const router = useRouter()

    router.afterEach((to) => {
      if (to.path !== '/anime') return

      const savedY = sessionStorage.getItem(KEY)
      if (savedY === null) return

      sessionStorage.removeItem(KEY)
      const top = Number(savedY)
      if (isNaN(top) || top <= 0) return

      // 等待页面渲染完成后再恢复滚动位置
      nuxtApp.hooks.hookOnce('page:loading:end', () => {
        nextTick(() => {
          requestAnimationFrame(() => {
            window.scrollTo({ top, behavior: 'auto' })
          })
        })
      })
    })
  },
})
