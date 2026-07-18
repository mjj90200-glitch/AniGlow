<template>
    <Teleport to="body">
      <Transition name="donate-popup">
        <div
          v-if="showDonatePopup"
          class="fixed inset-0 z-[100] flex items-center justify-center bg-gray-900/20 px-4 backdrop-blur-md"
          role="dialog"
          aria-modal="true"
          aria-labelledby="agent-membership-title"
          @click.self="showDonatePopup = false"
        >
          <section class="relative w-full max-w-sm rounded-[2rem] border border-white/75 bg-cream/90 p-6 text-center shadow-[0_28px_80px_rgba(31,68,47,0.2)] backdrop-blur-2xl">
            <button class="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-full bg-white/75 text-gray-400 transition hover:text-gray-700" aria-label="关闭会员窗口" @click="showDonatePopup = false">
              <X class="h-4 w-4" />
            </button>
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-firefly/15 text-firefly-700">
              <Crown class="h-6 w-6" />
            </div>
            <h2 id="agent-membership-title" class="mt-4 text-xl font-black text-gray-800">{{ donateTitle }}</h2>
            <p class="mt-2 text-sm leading-6 text-gray-500">
              当前剩余 {{ quotaStatus.remaining }} / {{ quotaStatus.limit }} 次，{{ quotaResetLabel }}。
            </p>
            <div class="mx-auto mt-4 h-44 w-44 overflow-hidden rounded-2xl border border-white/80 bg-white p-3 shadow-soft">
              <img :src="douyinQrUrl" alt="萤火会员支付二维码" class="h-full w-full rounded-xl object-cover" />
            </div>
            <div class="mt-4 rounded-3xl bg-white/60 p-4 text-left">
              <p class="text-sm font-black text-firefly-700">萤火月卡 · ¥9.9 / 月</p>
              <p class="mt-2 text-xs font-bold leading-6 text-gray-500">每小时 100 次 AIGC 对话，会员期内可使用后续上线的角色语音功能。</p>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>
</template>
<script setup lang="ts">
import { Crown, X } from 'lucide-vue-next'
const props = defineProps<{ chat: Record<string, any> }>()
const { showDonatePopup, donateTitle, quotaStatus, quotaResetLabel, douyinQrUrl } = toRefs(props.chat)
</script>
