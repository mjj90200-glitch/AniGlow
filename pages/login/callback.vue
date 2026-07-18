<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4 pt-24 pb-16">
    <div class="text-center">
      <!-- 加载状态 -->
      <div v-if="processing" class="animate-fade-in">
        <div
          class="w-16 h-16 mx-auto mb-4 rounded-2xl flex items-center justify-center"
          style="background: linear-gradient(135deg, #00E676 0%, #00C853 100%);"
        >
          <div class="w-8 h-8 border-2 border-white/30 border-t-white rounded-full animate-spin" />
        </div>
        <h1 class="text-xl font-extrabold text-gray-800 mb-2">正在登录...</h1>
        <p class="text-gray-500 text-sm">请稍候，正在验证您的身份</p>
      </div>

      <!-- 错误状态 -->
      <div v-if="error" class="animate-slide-up">
        <div
          class="w-16 h-16 mx-auto mb-4 rounded-2xl flex items-center justify-center"
          style="background: linear-gradient(135deg, #FFC0CB 0%, #FF9AAE 100%);"
        >
          <XCircle class="w-8 h-8 text-white" />
        </div>
        <h1 class="text-xl font-extrabold text-gray-800 mb-2">登录失败</h1>
        <p class="text-gray-500 text-sm mb-6">{{ error }}</p>
        <div class="flex items-center justify-center gap-3">
          <NuxtLink
            to="/login"
            class="btn-glow text-sm px-6 py-2.5"
          >
            重新登录
          </NuxtLink>
          <NuxtLink
            to="/"
            class="btn-glass text-sm px-6 py-2.5"
          >
            返回首页
          </NuxtLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { XCircle } from 'lucide-vue-next'

definePageMeta({
  layout: 'default',
})

const userStore = useUserStore()
const processing = ref(true)
const error = ref('')

onMounted(async () => {
  const route = useRoute()
  const code = route.query.code as string
  const state = route.query.state as string

  if (!code) {
    error.value = '缺少授权码，请重新登录'
    processing.value = false
    return
  }

  try {
    // 调用后端交换 token
    const response = await $fetch<{
      token: string
      user: { id: string; name: string; avatar?: string; email?: string; phone?: string }
    }>('/api/auth/callback', {
      method: 'POST',
      body: { code, state },
    })

    if (response.token && response.user) {
      const userWithBackend: any = { ...response.user, backendToken: (response as any).backendToken }
      userStore.loginWithAuthing(userWithBackend, response.token)
      await navigateTo('/')
    } else {
      throw new Error('登录响应无效')
    }
  } catch (e: any) {
    console.error('OIDC callback error:', e)
    error.value = e?.data?.message || e?.message || '登录验证失败，请重试'
    processing.value = false
  }
})
</script>
