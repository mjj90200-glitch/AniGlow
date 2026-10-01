<template>
  <div class="w-full animate-fade-in">
    <div class="flex mb-6 p-1 rounded-2xl bg-white/40 backdrop-blur-sm">
      <button
        v-for="tab in tabs" :key="tab.key"
        class="flex-1 py-2.5 rounded-xl text-sm font-bold transition-all duration-300 relative"
        :class="mode === tab.key ? 'text-gray-800' : 'text-gray-400 hover:text-gray-600'"
        type="button" @click="switchMode(tab.key)"
      >
        {{ tab.label }}
        <span v-if="mode === tab.key" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-0.5 rounded-full"
          style="background: linear-gradient(90deg, #00E676, #A0D8EF);" />
      </button>
    </div>

    <form @submit.prevent="submit">
      <div class="mb-4">
        <label class="block text-xs font-semibold text-gray-400 mb-2 ml-1 tracking-wide">用户名</label>
        <input v-model.trim="username" autocomplete="username" maxlength="24" placeholder="3-24位中文、字母、数字或下划线"
          class="auth-input" />
      </div>

      <div v-if="mode === 'register'" class="mb-4">
        <label class="block text-xs font-semibold text-gray-400 mb-2 ml-1 tracking-wide">昵称 <span class="font-normal">（选填）</span></label>
        <input v-model.trim="displayName" autocomplete="nickname" maxlength="80" placeholder="大家会在评论区看到这个名字"
          class="auth-input" />
      </div>

      <div class="mb-4">
        <label class="block text-xs font-semibold text-gray-400 mb-2 ml-1 tracking-wide">密码</label>
        <div class="relative">
          <input v-model="password" :type="showPassword ? 'text' : 'password'"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            placeholder="至少8位密码" class="auth-input pr-12" />
          <button type="button" class="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
            <EyeOff v-if="showPassword" class="w-4.5 h-4.5" />
            <Eye v-else class="w-4.5 h-4.5" />
          </button>
        </div>
      </div>

      <div v-if="mode === 'register'" class="mb-5">
        <label class="block text-xs font-semibold text-gray-400 mb-2 ml-1 tracking-wide">确认密码</label>
        <input v-model="confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入密码"
          class="auth-input" :class="confirmPassword && confirmPassword !== password ? '!border-[#FF9AAE]' : ''" />
        <p v-if="confirmPassword && confirmPassword !== password" class="text-xs mt-1.5 ml-1 text-[#D0707F]">两次密码不一致</p>
      </div>

      <div v-if="error" class="mb-4 p-3 rounded-2xl text-sm animate-slide-up flex items-start gap-2"
        style="background: rgba(255,192,203,0.12); border: 1px solid rgba(255,154,174,0.25); color: #D0707F;">
        <AlertCircle class="w-4 h-4 mt-px shrink-0" /><span>{{ error }}</span>
      </div>

      <button type="submit" class="w-full py-4 rounded-2xl text-base font-extrabold text-white transition-all duration-300 disabled:opacity-40 disabled:cursor-not-allowed hover:-translate-y-0.5 active:scale-[0.98]"
        :style="buttonStyle" :disabled="loading">
        <span v-if="loading" class="inline-flex items-center gap-2"><span class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />处理中...</span>
        <span v-else class="inline-flex items-center gap-2">
          <LogIn v-if="mode === 'login'" class="w-5 h-5" />
          <UserPlus v-else class="w-5 h-5" />{{ mode === 'login' ? '登录' : '创建账号' }}
        </span>
      </button>
    </form>

    <p class="text-center text-xs text-gray-400 mt-5">
      {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
      <button type="button" class="font-semibold text-firefly-500 hover:underline ml-0.5"
        @click="switchMode(mode === 'login' ? 'register' : 'login')">
        {{ mode === 'login' ? '免费注册' : '去登录' }}
      </button>
    </p>
    <p class="text-center text-[11px] text-gray-400 mt-3">当前阶段不收集手机号，也不会产生短信验证费用</p>
  </div>
</template>

<script setup lang="ts">
import { AlertCircle, Eye, EyeOff, LogIn, UserPlus } from 'lucide-vue-next'

const emit = defineEmits<{ login: [user: any] }>()
const userStore = useUserStore()
const tabs = [{ key: 'login' as const, label: '登录' }, { key: 'register' as const, label: '注册' }]
const mode = ref<'login' | 'register'>('login')
const username = ref('')
const displayName = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')

const validUsername = computed(() => /^[\p{Script=Han}A-Za-z0-9_]{3,24}$/u.test(username.value))
const canSubmit = computed(() => validUsername.value && password.value.length >= 8
  && (mode.value === 'login' || password.value === confirmPassword.value))
const buttonStyle = computed(() => ({
  background: 'linear-gradient(135deg, #00E676 0%, #00C853 100%)',
  boxShadow: canSubmit.value ? '0 8px 28px rgba(0,230,118,0.35)' : '0 5px 18px rgba(0,200,83,0.2)',
}))

function switchMode(next: 'login' | 'register') {
  mode.value = next
  password.value = ''
  confirmPassword.value = ''
  error.value = ''
}

function errorMessage(cause: any) {
  return cause?.data?.message || cause?.message || (mode.value === 'login' ? '用户名或密码错误' : '注册失败，请稍后重试')
}

async function submit() {
  error.value = ''
  if (!validUsername.value) { error.value = '用户名需为3-24位中文、字母、数字或下划线'; return }
  if (password.value.length < 8) { error.value = '密码至少8位'; return }
  if (mode.value === 'register' && password.value !== confirmPassword.value) { error.value = '两次密码不一致'; return }
  loading.value = true
  try {
    const loggedInUser = mode.value === 'login'
      ? await userStore.login(username.value, password.value)
      : await userStore.register(username.value, password.value, displayName.value || undefined)
    emit('login', loggedInUser)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-input {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 2px solid rgb(237 230 214);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.6);
  color: rgb(31 41 55);
  font-size: 1rem;
  font-weight: 600;
  outline: none;
  transition: all 0.3s;
}
.auth-input:focus { border-color: #00E676; background: white; box-shadow: 0 0 0 3px rgba(0,230,118,0.12); }
.auth-input::placeholder { color: rgb(209 213 219); font-weight: 500; }
</style>
