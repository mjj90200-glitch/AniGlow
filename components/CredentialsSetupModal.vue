<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-[95] flex items-center justify-center px-4"
      style="background: rgba(24, 42, 45, 0.2); backdrop-filter: blur(12px);">
      <section class="w-full max-w-md rounded-[2rem] border border-white/80 bg-white/70 p-6 shadow-2xl backdrop-blur-xl">
        <div class="mb-5 flex items-start gap-3">
          <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-firefly text-white shadow-lg shadow-firefly/20">
            <KeyRound class="h-6 w-6" />
          </div>
          <div>
            <h2 class="text-lg font-extrabold text-gray-800">保存你的本地登录方式</h2>
            <p class="mt-1 text-xs leading-5 text-gray-500">旧账号只需设置一次。以后无需短信验证码，直接使用用户名和密码登录。</p>
          </div>
        </div>

        <form class="space-y-4" @submit.prevent="submit">
          <label class="block text-xs font-bold text-gray-500">用户名
            <input v-model.trim="username" maxlength="24" autocomplete="username" class="credential-input mt-2" placeholder="3-24位中文、字母、数字或下划线" />
          </label>
          <label class="block text-xs font-bold text-gray-500">新密码
            <input v-model="password" type="password" autocomplete="new-password" class="credential-input mt-2" placeholder="至少8位" />
          </label>
          <label class="block text-xs font-bold text-gray-500">确认密码
            <input v-model="confirmation" type="password" autocomplete="new-password" class="credential-input mt-2" placeholder="再次输入密码" />
          </label>
          <p v-if="error" class="rounded-xl bg-pink/10 px-3 py-2 text-xs text-[#C85D70]">{{ error }}</p>
          <button :disabled="loading" class="w-full rounded-full bg-firefly py-3.5 text-sm font-extrabold text-white shadow-lg shadow-firefly/20 transition hover:-translate-y-0.5 disabled:opacity-50">
            {{ loading ? '正在保存...' : '保存并继续' }}
          </button>
          <button type="button" class="w-full text-xs font-semibold text-gray-400 hover:text-gray-600" @click="$emit('logout')">暂不设置，退出账号</button>
        </form>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { KeyRound } from 'lucide-vue-next'

defineProps<{ visible: boolean }>()
const emit = defineEmits<{ complete: []; logout: [] }>()
const userStore = useUserStore()
const username = ref('')
const password = ref('')
const confirmation = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!/^[\p{Script=Han}A-Za-z0-9_]{3,24}$/u.test(username.value)) { error.value = '用户名格式不正确'; return }
  if (password.value.length < 8) { error.value = '密码至少8位'; return }
  if (password.value !== confirmation.value) { error.value = '两次密码不一致'; return }
  loading.value = true
  try {
    await userStore.setCredentials(username.value, password.value)
    emit('complete')
  } catch (cause: any) {
    error.value = cause?.data?.message || cause?.message || '保存失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.credential-input { width: 100%; border: 2px solid rgba(218, 228, 219, .9); border-radius: 1rem; background: rgba(255,255,255,.7); padding: .8rem 1rem; color: #263238; outline: none; transition: .2s; }
.credential-input:focus { border-color: #00E676; box-shadow: 0 0 0 3px rgba(0,230,118,.1); }
</style>
