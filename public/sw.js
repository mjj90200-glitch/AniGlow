const CACHE_NAME = 'aniglow-v2'
// 只缓存静态资源，不缓存 HTML 页面 — 确保每次访问都获取最新版本
const STATIC_EXTENSIONS = ['.js', '.css', '.svg', '.png', '.jpg', '.jpeg', '.webp', '.woff2', '.m4a']

self.addEventListener('install', (event) => {
  event.waitUntil(self.skipWaiting())
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  const url = new URL(request.url)

  if (request.method !== 'GET') return
  if (url.pathname.startsWith('/api/')) return

  // 仅缓存静态资源（带 content hash 的文件名确保版本更新自动生效）
  const isStatic = STATIC_EXTENSIONS.some((ext) => url.pathname.endsWith(ext))
  if (!isStatic) {
    // HTML 页面总是走网络，不缓存
    return
  }

  event.respondWith(
    caches.match(request).then((cached) => {
      // 缓存优先：静态资源文件名含 hash，永远是最新版本
      if (cached) return cached
      return fetch(request).then((response) => {
        if (response.ok) {
          const copy = response.clone()
          caches.open(CACHE_NAME).then((cache) => cache.put(request, copy))
        }
        return response
      })
    })
  )
})
