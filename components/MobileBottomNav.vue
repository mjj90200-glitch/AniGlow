<template>
  <nav
    class="fixed inset-x-0 bottom-0 z-[70] lg:hidden px-3 pb-[calc(env(safe-area-inset-bottom)+0.55rem)] pt-2
           bg-cream/75 backdrop-blur-2xl border-t border-white/60 shadow-[0_-10px_32px_rgba(160,216,239,0.18)]"
    aria-label="移动端主导航"
  >
    <div class="grid grid-cols-6 gap-0.5 rounded-[1.75rem] bg-white/55 p-1.5 shadow-soft">
      <NuxtLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="relative flex min-h-[3.35rem] flex-col items-center justify-center gap-0.5 rounded-[1.25rem]
               text-[11px] font-extrabold transition-all active:scale-95"
        :class="isActive(item.path)
          ? 'text-firefly-700 bg-firefly/12 shadow-glow-sm'
          : 'text-gray-400 hover:text-firefly-700'"
      >
        <span
          v-if="isActive(item.path)"
          class="absolute -top-1 h-1 w-7 rounded-full bg-firefly shadow-[0_0_14px_rgba(0,230,118,0.55)]"
        />
        <component :is="item.icon" class="h-5 w-5" />
        <span>{{ item.label }}</span>
      </NuxtLink>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { Bot, CalendarCheck2, Home, Trophy, Tv, Users } from 'lucide-vue-next'

const route = useRoute()
const userStore = useUserStore()

const navItems = [
  { label: '首页', path: '/', icon: Home },
  { label: '番剧', path: '/anime', icon: Tv },
  { label: '排行', path: '/ranking', icon: Trophy },
  { label: '每日', path: '/daily', icon: CalendarCheck2 },
  { label: 'AIGC', path: '/agent', icon: Bot },
  { label: '社区', path: '/community', icon: Users },
]

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>
