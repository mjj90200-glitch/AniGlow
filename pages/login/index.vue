<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4 pt-24 pb-16">
    <div class="w-full max-w-md">
      <!-- 标题 -->
      <div class="text-center mb-8 animate-fade-in">
        <div
          class="w-16 h-16 mx-auto mb-4 rounded-2xl flex items-center justify-center"
          style="background: linear-gradient(135deg, #00E676 0%, #00C853 100%);"
        >
          <Sparkles class="w-8 h-8 text-white" />
        </div>
        <h1 class="text-2xl font-extrabold text-gray-800 mb-2">欢迎回来</h1>
        <p class="text-gray-500 text-sm">登录萤火番舍，发现更多精彩番剧</p>
      </div>

      <!-- 用户名密码登录容器 -->
      <div class="glass-card-cream rounded-4xl p-1 overflow-hidden animate-slide-up">
        <div class="relative bg-white/40 rounded-3xl p-6 min-h-[420px]">
          <LoginForm @login="handleLoginSuccess" />

          <!-- 底部提示 -->
          <div class="mt-6 pt-4 border-t border-white/40 text-center">
            <p class="text-xs text-gray-400">
              登录即表示同意
              <a href="#" class="text-sky-dark hover:underline">服务条款</a>
              和
              <a href="#" class="text-sky-dark hover:underline">隐私政策</a>
            </p>
          </div>
        </div>
      </div>

      <!-- 返回首页 -->
      <div class="text-center mt-6">
        <NuxtLink
          to="/"
          class="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-gray-600 transition-colors"
        >
          <ArrowLeft class="w-4 h-4" />
          返回首页
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Sparkles, ArrowLeft } from 'lucide-vue-next'

definePageMeta({
  layout: 'default',
})

const userStore = useUserStore()

// 如果已登录，跳转首页
if (userStore.isLoggedIn) {
  await navigateTo('/')
}

function handleLoginSuccess() {
  navigateTo('/')
}
</script>
