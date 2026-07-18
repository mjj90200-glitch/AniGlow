<template>
  <aside class="space-y-6">
    <div class="glass-card overflow-hidden rounded-4xl p-6">
      <div class="flex items-center justify-between gap-4">
        <div>
          <p class="text-[11px] font-black uppercase tracking-[0.2em] text-firefly-600">Firefly Vote</p>
          <h3 class="mt-1 text-lg font-extrabold text-gray-800">为这部番投一票</h3>
        </div>
        <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-firefly/10 text-firefly-700">
          <TicketCheck class="h-5 w-5" />
        </div>
      </div>

      <div class="my-5 grid grid-cols-2 gap-3">
        <div class="rounded-3xl bg-white/60 p-3">
          <p class="text-[11px] font-bold text-gray-400">当前萤火票</p>
          <p class="mt-1 text-2xl font-black text-gray-800">{{ currentVotes }}</p>
        </div>
        <div class="rounded-3xl bg-white/60 p-3">
          <p class="text-[11px] font-bold text-gray-400">我的可用票</p>
          <p class="mt-1 text-2xl font-black text-firefly-700">{{ availableTickets }}</p>
        </div>
      </div>

      <button
        v-if="availableTickets > 0"
        type="button"
        class="btn-glow w-full justify-center px-5 py-3"
        :disabled="voting"
        :class="voting ? 'pointer-events-none opacity-50' : ''"
        @click="$emit('vote')"
      >
        {{ voting ? '萤火飞行中...' : '投出 1 张萤火票' }}
      </button>
      <NuxtLink v-else to="/daily" class="btn-glow flex w-full justify-center px-5 py-3">
        去做每日任务领票
      </NuxtLink>

      <p v-if="voteMessage" class="mt-3 text-center text-xs font-extrabold text-firefly-700">{{ voteMessage }}</p>
      <p v-if="voteError" class="mt-3 text-center text-xs font-bold text-sakura-dark">{{ voteError }}</p>
    </div>

    <div class="glass-card rounded-4xl p-6">
      <h3 class="mb-4 text-lg font-extrabold text-gray-800">基本信息</h3>
      <div class="space-y-3 text-sm">
        <div v-for="item in infoRows" :key="item.label" class="flex justify-between gap-4">
          <span class="text-gray-400">{{ item.label }}</span>
          <span class="text-right font-bold text-gray-700">{{ item.value }}</span>
        </div>
      </div>
    </div>

    <div class="glass-card rounded-4xl p-6">
      <h3 class="mb-4 text-lg font-extrabold text-gray-800">同好也在看</h3>
      <div class="space-y-4">
        <NuxtLink v-for="item in relatedAnime" :key="item.id" :to="`/anime/${item.id}`" class="group flex gap-3">
          <div class="h-20 w-16 shrink-0 overflow-hidden rounded-2xl shadow-soft">
            <img
              :src="item.coverImage"
              :alt="displayTitle(item)"
              class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
            />
          </div>
          <div class="min-w-0 flex-1">
            <h4 class="line-clamp-2 text-sm font-extrabold text-gray-800 group-hover:text-firefly-700">
              {{ displayTitle(item) }}
            </h4>
            <p class="mt-1 text-xs text-gray-400">{{ tType(item.type) || item.year || '番剧' }}</p>
            <div class="mt-1 flex items-center gap-1">
              <Star class="h-3.5 w-3.5 fill-[#FFD54F] text-[#FFD54F]" />
              <span class="text-xs font-bold text-gray-600">{{ formatScore(communityScore(item)) }}</span>
            </div>
          </div>
        </NuxtLink>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { Star, TicketCheck } from 'lucide-vue-next'
import type { AnimeDto } from '~/types/anime'
import { communityScore, displayTitle, tType } from '~/composables/useAnimeI18n'

defineProps<{
  currentVotes: number
  availableTickets: number
  voting: boolean
  voteMessage: string
  voteError: string
  infoRows: Array<{ label: string; value: string | number }>
  relatedAnime: AnimeDto[]
}>()

defineEmits<{ vote: [] }>()

function formatScore(score?: number) {
  return score ? (Math.round(score * 10) / 10).toFixed(1) : '0.0'
}
</script>
