<template>
  <Transition
    enter-active-class="transition-all duration-300 ease-out"
    enter-from-class="opacity-0 translate-y-4"
    enter-to-class="opacity-100 translate-y-0"
    leave-active-class="transition-all duration-200 ease-in"
    leave-from-class="opacity-100 translate-y-0"
    leave-to-class="opacity-0 translate-y-4"
  >
    <aside
      v-if="visible"
      class="fixed inset-x-4 bottom-[6.2rem] z-[72] lg:hidden rounded-[1.75rem] border border-white/65
             bg-white/82 p-4 shadow-glaze-lg backdrop-blur-2xl"
      role="dialog"
      aria-label="添加萤火番舍到主屏幕"
    >
      <button
        class="absolute right-3 top-3 flex h-7 w-7 items-center justify-center rounded-full bg-cream-100 text-gray-400"
        aria-label="关闭添加提示"
        @click="dismiss"
      >
        <X class="h-3.5 w-3.5" />
      </button>

      <div class="flex gap-3 pr-7">
        <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-firefly shadow-glow">
          <Sparkles class="h-6 w-6 text-white" />
        </div>
        <div class="min-w-0">
          <p class="text-sm font-black text-gray-800">把萤火番舍放到手机桌面</p>
          <p class="mt-1 text-xs font-semibold leading-5 text-gray-500">
            像 App 一样打开，底部导航、任务、AIGC 都会更顺手。
          </p>
        </div>
      </div>

      <button
        v-if="canInstall"
        class="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-full bg-firefly px-4 py-3
               text-sm font-black text-white shadow-glow active:scale-95"
        @click="install"
      >
        <Download class="h-4 w-4" />
        添加到主屏幕
      </button>

      <div v-else class="mt-3 rounded-2xl bg-cream-100/70 px-3 py-2 text-xs font-bold leading-5 text-gray-500">
        iPhone：点击浏览器底部分享按钮，选择“添加到主屏幕”。
      </div>
    </aside>
  </Transition>
</template>

<script setup lang="ts">
import { Download, Sparkles, X } from 'lucide-vue-next'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
}

const DISMISS_KEY = 'aniglow_pwa_install_dismissed'
const deferredPrompt = ref<BeforeInstallPromptEvent | null>(null)
const dismissed = ref(false)
const isStandalone = ref(false)
const isMobile = ref(false)

const canInstall = computed(() => Boolean(deferredPrompt.value))
const visible = computed(() => isMobile.value && !dismissed.value && !isStandalone.value && (canInstall.value || isIosSafari()))

onMounted(() => {
  dismissed.value = localStorage.getItem(DISMISS_KEY) === '1'
  isStandalone.value = window.matchMedia('(display-mode: standalone)').matches
    || Boolean((window.navigator as any).standalone)
  isMobile.value = window.matchMedia('(max-width: 1023px)').matches

  window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt as EventListener)
  window.addEventListener('appinstalled', handleInstalled)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt as EventListener)
  window.removeEventListener('appinstalled', handleInstalled)
})

function handleBeforeInstallPrompt(event: BeforeInstallPromptEvent) {
  event.preventDefault()
  deferredPrompt.value = event
}

function handleInstalled() {
  dismissed.value = true
  localStorage.setItem(DISMISS_KEY, '1')
}

async function install() {
  if (!deferredPrompt.value) return
  await deferredPrompt.value.prompt()
  const choice = await deferredPrompt.value.userChoice
  if (choice.outcome === 'accepted') dismiss()
  deferredPrompt.value = null
}

function dismiss() {
  dismissed.value = true
  localStorage.setItem(DISMISS_KEY, '1')
}

function isIosSafari() {
  if (import.meta.server) return false
  const ua = window.navigator.userAgent
  const isIos = /iphone|ipad|ipod/i.test(ua)
  const isWebkit = /safari/i.test(ua) && !/crios|fxios|edgios/i.test(ua)
  return isIos && isWebkit
}
</script>
