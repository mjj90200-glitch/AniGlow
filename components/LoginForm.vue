<template>
  <div class="w-full animate-fade-in">
    <!-- ═══ 登录 / 注册 Tab 切换 ═══ -->
    <div class="flex mb-6 p-1 rounded-2xl bg-white/40 backdrop-blur-sm">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="flex-1 py-2.5 rounded-xl text-sm font-bold transition-all duration-300 relative"
        :class="mode === tab.key ? 'text-gray-800' : 'text-gray-400 hover:text-gray-600'"
        @click="switchMode(tab.key)"
      >
        {{ tab.label }}
        <span
          v-if="mode === tab.key"
          class="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-0.5 rounded-full"
          style="background: linear-gradient(90deg, #00E676, #A0D8EF);"
        />
      </button>
    </div>

    <!-- ═══ 手机号输入（两个模式共用） ═══ -->
    <div class="mb-4">
      <label class="block text-xs font-semibold text-gray-400 mb-2 ml-1 tracking-wide">手机号码</label>
      <div class="relative">
        <div class="absolute left-0 top-0 bottom-0 flex items-center pl-4 pr-2 text-sm font-bold text-gray-500 border-r border-cream-200">+86</div>
        <input
          v-model="phone"
          type="tel" maxlength="11" placeholder="请输入手机号"
          class="w-full pl-20 pr-4 py-3.5 text-base font-semibold text-gray-800 placeholder-gray-300
                 outline-none transition-all duration-300 rounded-2xl bg-white/60"
          style="border: 2px solid;"
          :class="phoneFocused ? 'border-firefly shadow-[0_0_0_3px_rgba(0,230,118,0.12)] bg-white' : 'border-cream-300'"
          @focus="phoneFocused = true" @blur="phoneFocused = false"
        />
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════
         登录模式：手机号 + 密码
         ════════════════════════════════════════════════════════ -->
    <template v-if="mode === 'login'">
      <div class="mb-5">
        <label class="block text-xs font-semibold text-gray-400 mb-2 ml-1 tracking-wide">密码</label>
        <div class="relative">
          <input
            v-model="password"
            :type="showPwd ? 'text' : 'password'"
            placeholder="请输入密码"
            class="w-full px-4 py-3.5 text-base font-semibold text-gray-800 placeholder-gray-300
                   outline-none transition-all duration-300 rounded-2xl bg-white/60"
            style="border: 2px solid;"
            :class="pwdFocused ? 'border-firefly shadow-[0_0_0_3px_rgba(0,230,118,0.12)] bg-white' : 'border-cream-300'"
            @focus="pwdFocused = true" @blur="pwdFocused = false"
            @keydown.enter="handleLogin"
          />
          <button
            class="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
            @click="showPwd = !showPwd"
          >
            <Eye v-if="!showPwd" class="w-4.5 h-4.5" />
            <EyeOff v-else class="w-4.5 h-4.5" />
          </button>
        </div>
      </div>

      <!-- 登录错误 -->
      <div v-if="error" class="mb-4 p-3 rounded-2xl text-sm animate-slide-up flex items-start gap-2"
        style="background: rgba(255,192,203,0.12); border: 1px solid rgba(255,154,174,0.25); color: #D0707F;">
        <AlertCircle class="w-4 h-4 mt-px shrink-0" />
        <span>{{ error }}</span>
      </div>

      <button
        class="w-full py-4 rounded-2xl text-base font-extrabold text-white transition-all duration-300
               disabled:opacity-40 disabled:cursor-not-allowed hover:-translate-y-0.5 active:scale-[0.98]"
        :style="{
          background: canLogin ? 'linear-gradient(135deg, #00E676 0%, #00C853 100%)' : '#ccc',
          boxShadow: canLogin ? '0 8px 28px rgba(0,230,118,0.35)' : 'none'
        }"
        :disabled="!canLogin || loading"
        @click="handleLogin"
      >
        <span v-if="loading" class="inline-flex items-center gap-2">
          <span class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          登录中...
        </span>
        <span v-else class="inline-flex items-center gap-2">
          <LogIn class="w-5 h-5" /> 登录
        </span>
      </button>
    </template>

    <!-- ═══════════════════════════════════════════════════════════
         注册模式：验证码 + 密码 + 确认密码
         ════════════════════════════════════════════════════════ -->
    <template v-if="mode === 'register'">
      <!-- 验证码 -->
      <div class="mb-4">
        <label class="block text-xs font-semibold text-gray-400 mb-2 ml-1 tracking-wide">验证码</label>
        <div class="flex gap-2.5">
          <input
            v-model="code"
            type="text" maxlength="6" inputmode="numeric" placeholder="输入验证码"
            class="flex-1 px-4 py-3.5 text-base font-bold text-center tracking-[0.3em] text-gray-800
                   placeholder-gray-300 placeholder:tracking-normal outline-none rounded-2xl bg-white/60
                   transition-all duration-300"
            style="border: 2px solid;"
            :class="codeFocused ? 'border-firefly shadow-[0_0_0_3px_rgba(0,230,118,0.12)] bg-white' : 'border-cream-300'"
            @focus="codeFocused = true" @blur="codeFocused = false"
          />
          <button
            class="shrink-0 px-5 py-3.5 rounded-2xl text-sm font-bold whitespace-nowrap transition-all duration-300
                   disabled:cursor-not-allowed"
            :class="countdown > 0 ? 'bg-gray-100 text-gray-400' : 'bg-firefly/10 text-firefly-600 border border-firefly/20 hover:bg-firefly/20 active:scale-95'"
            :disabled="countdown > 0 || !validPhone"
            @click="handleSendCode"
          >
            {{ sending ? '发送中...' : countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </button>
        </div>
      </div>

      <!-- 密码 -->
      <div class="mb-4">
        <label class="block text-xs font-semibold text-gray-400 mb-2 ml-1 tracking-wide">设置密码</label>
        <input
          v-model="password"
          type="password" placeholder="至少6位密码"
          class="w-full px-4 py-3.5 text-base font-semibold text-gray-800 placeholder-gray-300
                 outline-none transition-all duration-300 rounded-2xl bg-white/60"
          style="border: 2px solid;"
          :class="pwdFocused ? 'border-firefly shadow-[0_0_0_3px_rgba(0,230,118,0.12)] bg-white' : 'border-cream-300'"
          @focus="pwdFocused = true" @blur="pwdFocused = false"
        />
      </div>

      <!-- 确认密码 -->
      <div class="mb-5">
        <label class="block text-xs font-semibold text-gray-400 mb-2 ml-1 tracking-wide">确认密码</label>
        <input
          v-model="confirmPwd"
          type="password" placeholder="再次输入密码"
          class="w-full px-4 py-3.5 text-base font-semibold text-gray-800 placeholder-gray-300
                 outline-none transition-all duration-300 rounded-2xl bg-white/60"
          :style="{ border: `2px solid ${confirmPwd && password !== confirmPwd ? '#FF9AAE' : confirmPwd && password === confirmPwd ? '#00E676' : 'rgb(237,230,214)'}` }"
          @keydown.enter="handleRegister"
        />
        <p v-if="confirmPwd && password !== confirmPwd" class="text-xs mt-1.5 ml-1" style="color: #D0707F;">两次密码不一致</p>
      </div>

      <!-- 注册错误 -->
      <div v-if="error" class="mb-4 p-3 rounded-2xl text-sm animate-slide-up flex items-start gap-2"
        style="background: rgba(255,192,203,0.12); border: 1px solid rgba(255,154,174,0.25); color: #D0707F;">
        <AlertCircle class="w-4 h-4 mt-px shrink-0" />
        <span>{{ error }}</span>
      </div>

      <button
        class="w-full py-4 rounded-2xl text-base font-extrabold text-white transition-all duration-300
               disabled:opacity-40 disabled:cursor-not-allowed hover:-translate-y-0.5 active:scale-[0.98]"
        :style="{
          background: canRegister ? 'linear-gradient(135deg, #00E676 0%, #00C853 100%)' : '#ccc',
          boxShadow: canRegister ? '0 8px 28px rgba(0,230,118,0.35)' : 'none'
        }"
        :disabled="!canRegister || loading"
        @click="handleRegister"
      >
        <span v-if="loading" class="inline-flex items-center gap-2">
          <span class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          注册中...
        </span>
        <span v-else class="inline-flex items-center gap-2">
          <UserPlus class="w-5 h-5" /> 注册
        </span>
      </button>
    </template>

    <!-- ═══ 切换提示 ═══ -->
    <p class="text-center text-xs text-gray-400 mt-5">
      <template v-if="mode === 'login'">
        还没有账号？<button class="font-semibold text-firefly-500 hover:underline ml-0.5" @click="switchMode('register')">立即注册</button>
      </template>
      <template v-else>
        已有账号？<button class="font-semibold text-firefly-500 hover:underline ml-0.5" @click="switchMode('login')">去登录</button>
      </template>
    </p>
  </div>
</template>

<script setup lang="ts">
import { AlertCircle, LogIn, UserPlus, Eye, EyeOff } from 'lucide-vue-next'

const emit = defineEmits<{ login: [user: any, token?: string] }>()

const { sendSmsCode, loginByPassword, registerWithPassword } = useAuthingSDK()
const userStore = useUserStore()

// ─── 模式 ──────────────────────────────────────────────────
const tabs = [
  { key: 'login' as const, label: '登录' },
  { key: 'register' as const, label: '注册' },
]
const mode = ref<'login' | 'register'>('login')

// ─── 输入 ──────────────────────────────────────────────────
const phone = ref('')
const code = ref('')
const password = ref('')
const confirmPwd = ref('')
const error = ref('')
const loading = ref(false)
const sending = ref(false)
const showPwd = ref(false)
const countdown = ref(0)
const phoneFocused = ref(false)
const codeFocused = ref(false)
const pwdFocused = ref(false)

let countdownTimer: ReturnType<typeof setInterval> | undefined

const validPhone = computed(() => /^1\d{10}$/.test(phone.value))
const canLogin = computed(() => validPhone.value && password.value.length >= 6)
const canRegister = computed(() =>
  validPhone.value && code.value.length >= 4 &&
  password.value.length >= 6 && password.value === confirmPwd.value
)

onUnmounted(() => { if (countdownTimer) clearInterval(countdownTimer) })

// ─── 切换模式 ──────────────────────────────────────────────
function switchMode(key: 'login' | 'register') {
  mode.value = key
  error.value = ''
  code.value = ''
  password.value = ''
  confirmPwd.value = ''
}

// ─── 发验证码（仅注册模式） ────────────────────────────────
async function handleSendCode() {
  error.value = ''
  if (!validPhone.value) { error.value = '请输入正确的11位手机号'; return }
  sending.value = true
  const r = await sendSmsCode(phone.value)
  sending.value = false
  if (r.ok) {
    countdown.value = 60
    countdownTimer = setInterval(() => { countdown.value--; if (countdown.value <= 0 && countdownTimer) clearInterval(countdownTimer) }, 1000)
  } else {
    error.value = r.message || '发送失败'
  }
}

// ─── 密码登录 ──────────────────────────────────────────────
async function handleLogin() {
  error.value = ''
  if (!validPhone.value) { error.value = '请输入正确的手机号'; return }
  if (password.value.length < 6) { error.value = '密码至少6位'; return }

  loading.value = true
  const r = await loginByPassword(phone.value, password.value)
  loading.value = false

  if (r.ok && r.user) {
    r.user.backendToken = r.backendToken
    userStore.loginWithAuthing(r.user, r.token)
    emit('login', r.user, r.token)
  } else {
    error.value = r.message || '手机号或密码错误'
  }
}

// ─── 注册 ──────────────────────────────────────────────────
async function handleRegister() {
  error.value = ''
  if (!validPhone.value) { error.value = '请输入正确的手机号'; return }
  if (code.value.length < 4) { error.value = '请输入验证码'; return }
  if (password.value.length < 6) { error.value = '密码至少6位'; return }
  if (password.value !== confirmPwd.value) { error.value = '两次密码不一致'; return }

  loading.value = true
  const r = await registerWithPassword(phone.value, code.value, password.value)
  loading.value = false

  if (r.ok && r.user) {
    r.user.backendToken = r.backendToken
    userStore.loginWithAuthing(r.user, r.token)
    emit('login', r.user, r.token)
  } else {
    error.value = r.message || '注册失败，请重试'
  }
}
</script>
