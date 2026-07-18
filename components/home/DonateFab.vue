<template>
  <div class="fixed bottom-6 right-6 z-[70] flex flex-col items-end gap-3">
    <Transition name="donate-popup">
      <div
        v-if="showPopup"
        class="w-72 overflow-hidden rounded-2xl border border-white/70 bg-[#fffaf0]/95 shadow-[0_20px_60px_rgba(31,68,47,0.18)] backdrop-blur-xl"
      >
        <button
          type="button"
          class="absolute right-3 top-3 z-10 flex h-7 w-7 items-center justify-center rounded-full bg-white/70 text-gray-400 transition hover:text-gray-700"
          @click="showPopup = false"
        >
          <X class="h-3.5 w-3.5" />
        </button>
        <div class="p-5 text-center">
          <p class="text-sm font-black text-gray-800">{{ title }}</p>
          <div class="mx-auto mt-4 flex h-44 w-44 items-center justify-center overflow-hidden rounded-2xl border border-white/80 bg-white p-3 shadow-sm">
            <img
              :src="qrUrl"
              alt="支付二维码"
              class="h-full w-full rounded-xl object-cover"
            />
          </div>
          <p class="mt-3 text-xs font-bold text-gray-500 leading-relaxed">
            想变成萤火会员请联系站长<br>
            <span class="text-firefly-600 font-black">¥9.9/月 · 每小时100次AIGC对话</span>
          </p>
        </div>
      </div>
    </Transition>

    <button
      type="button"
      class="donate-fab"
      :class="{ 'donate-fab-active': showPopup }"
      @click="showPopup = !showPopup"
      :aria-label="showPopup ? '关闭' : '打赏支持'"
    >
      <span class="donate-fab-text">賞</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { X } from 'lucide-vue-next'

interface MembershipStatus {
  active: boolean
  planCode: string
  planName: string
  priceText: string
  quotaLimitPerHour: number
  quotaLimitPerDay?: number
  voiceEnabled: boolean
  expiresAt: string | null
  paymentQrUrl: string | null
}

const showPopup = ref(false)
const qrUrl = ref('/images/douyin-qr.png')
const title = ref('喜欢角色的陪伴吗？')

const { request } = useApi()

async function fetchMembership() {
  try {
    const membership = await request<MembershipStatus>('/agent/membership/status', { auth: true })
    qrUrl.value = membership.paymentQrUrl || '/images/douyin-qr.png'
  } catch {}
}

onMounted(() => fetchMembership())
</script>

<style scoped>
.donate-fab {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 999px;
  border: 1.5px solid rgba(255, 255, 255, 0.7);
  background: linear-gradient(135deg, rgba(253, 251, 247, 0.92), rgba(240, 244, 248, 0.9));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 4px 18px rgba(160, 216, 239, 0.22), 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
}
.donate-fab::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(0, 230, 118, 0.25), rgba(103, 232, 249, 0.2));
  opacity: 0;
  transition: opacity 0.25s ease;
}
.donate-fab:hover::before,
.donate-fab-active::before {
  opacity: 1;
}
.donate-fab:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 24px rgba(160, 216, 239, 0.3), 0 2px 8px rgba(0, 230, 118, 0.15);
}
.donate-fab-active {
  transform: scale(1.06);
  border-color: rgba(0, 230, 118, 0.25);
}
.donate-fab-text {
  font-size: 1.2rem;
  font-weight: 900;
  color: #374151;
  letter-spacing: 0.02em;
  user-select: none;
}
.donate-popup-enter-active {
  transition: all 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.donate-popup-leave-active {
  transition: all 0.2s ease-in;
}
.donate-popup-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.95);
}
.donate-popup-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.97);
}
</style>
