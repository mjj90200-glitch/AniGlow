<template>
  <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-32 pb-20">
    <!-- 页面标题 -->
    <div class="mb-10">
      <p class="text-xs font-semibold tracking-widest uppercase mb-2" style="color: #00AA44;">
        Rankings
      </p>
      <h1 class="text-3xl font-extrabold text-gray-800 flex items-center gap-2">
        番剧排行榜
        <Crown class="w-7 h-7" style="color: #FFB300;" />
      </h1>
      <p class="text-gray-500 mt-2 text-sm">站内评分与萤火投票共同组成真实用户排行</p>
      <div class="mt-4 inline-flex items-center gap-2 rounded-full bg-white/65 backdrop-blur-md px-4 py-2 shadow-soft">
        <TicketCheck class="w-4 h-4 text-firefly" />
        <span class="text-sm font-bold text-gray-700">
          今日可投 {{ dailyStore.state.tickets }} 票
        </span>
        <NuxtLink to="/daily" class="text-xs font-bold text-firefly-700 hover:text-firefly-600">
          做任务获取
        </NuxtLink>
      </div>
      <div class="mt-5 flex flex-wrap gap-2">
        <button
          v-for="tab in rankingTabs"
          :key="tab.key"
          class="rounded-full px-4 py-2 text-xs font-extrabold transition-all"
          :class="activeRanking === tab.key
            ? 'bg-firefly text-white shadow-glow'
            : 'bg-white/65 text-gray-500 hover:text-firefly-700'"
          @click="activeRanking = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="pending" class="space-y-3">
      <div
        v-for="n in 10"
        :key="'skel-' + n"
        class="rounded-3xl bg-white/40 backdrop-blur-sm animate-pulse h-20"
      />
    </div>

    <!-- 排行列表 -->
    <div v-else class="space-y-3">
      <div
        v-for="(anime, index) in rankedList"
        :key="anime.id"
        class="glass-card rounded-3xl p-4 flex items-center gap-4 group
               cursor-pointer transition-all duration-300 hover:-translate-y-1"
      >
        <NuxtLink :to="`/anime/${anime.id}`" class="flex items-center gap-4 flex-1 min-w-0">

          <!-- 排名 -->
          <div
            class="w-10 h-10 rounded-full flex items-center justify-center text-sm font-extrabold shrink-0"
            :class="[
              index === 0 ? 'bg-amber shadow-glow text-amber-700' :
              index === 1 ? 'bg-gray-200 text-gray-600' :
              index === 2 ? 'bg-amber-100 text-amber-700' :
              'bg-cream-200 text-gray-500'
            ]"
            :style="index < 3 ? { background: index === 0 ? 'linear-gradient(135deg, #FFD54F, #FFB300)' : index === 1 ? 'linear-gradient(135deg, #C0C0C0, #A0A0A0)' : 'linear-gradient(135deg, #FFCC80, #FF9800)' } : {}"
          >
            {{ index + 1 }}
          </div>

          <!-- 封面 -->
          <img
            :src="anime.cover"
            :alt="anime.title"
            class="w-12 h-16 rounded-xl object-cover shrink-0 shadow-soft"
            loading="lazy"
          />

          <!-- 信息 -->
          <div class="flex-1 min-w-0">
            <h3 class="font-extrabold text-gray-800 text-sm line-clamp-1
                       group-hover:text-firefly-600 transition-colors">
              {{ anime.title }}
            </h3>
            <div class="flex items-center gap-3 mt-1">
              <span class="text-xs text-gray-400">{{ anime.type }}</span>
              <span class="text-xs text-gray-400">{{ anime.episodes }}集</span>
              <span v-if="anime.studio" class="text-xs text-gray-400">{{ anime.studio }}</span>
            </div>
          </div>

          <!-- 评分 -->
          <div class="text-right shrink-0">
            <div class="text-lg font-extrabold" style="color: #FFB300;">
              {{ anime.rating }}
            </div>
            <div class="text-[10px] text-gray-400">
              {{ anime.metricLabel }}
            </div>
            <div class="text-[10px] font-bold text-firefly-700 mt-1">
              {{ anime.fireflyVoteCount }} 票萤火
            </div>
          </div>
        </NuxtLink>

        <button
          class="inline-flex items-center justify-center rounded-full px-3 sm:px-4 py-2 text-xs font-extrabold
                 bg-firefly/10 text-firefly-700 border border-firefly/20 transition-all hover:bg-firefly/20 active:scale-95"
          :disabled="dailyStore.state.tickets <= 0"
          :class="dailyStore.state.tickets <= 0 ? 'opacity-40 pointer-events-none' : ''"
          @click="vote(anime)"
        >
          投票
        </button>
      </div>
    </div>

    <!-- 空状态 -->
    <div
      v-if="!pending && rankedList.length === 0"
      class="text-center py-20"
    >
      <p class="text-gray-400">暂无排行数据</p>
    </div>

    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 translate-y-3"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 translate-y-3"
    >
      <div
        v-if="toast"
        class="fixed bottom-8 left-1/2 -translate-x-1/2 z-[90] rounded-full bg-white/85 backdrop-blur-2xl
               px-5 py-3 shadow-glaze-lg border border-white/60 text-sm font-bold text-firefly-700"
      >
        {{ toast }}
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { Crown, TicketCheck } from 'lucide-vue-next'
import type { AnimeDto } from '~/types/anime'
import { displayTitle, tType, communityScore } from '~/composables/useAnimeI18n'

type RankingType = 'community' | 'firefly'

const { fetchCommunityRated, fetchFireflyRanking } = useAnime()
const dailyStore = useDailyStore()
const toast = ref('')
let toastTimer: ReturnType<typeof setTimeout> | undefined
const activeRanking = ref<RankingType>('community')
const liveVoteCounts = ref<Record<number, number>>({})

const rankingTabs: { key: RankingType; label: string }[] = [
  { key: 'community', label: '中国用户评分' },
  { key: 'firefly', label: '萤火投票榜' },
]

const { data, pending } = await useAsyncData(
  'ranking-list',
  () => {
    if (activeRanking.value === 'firefly') return fetchFireflyRanking(0, 30)
    return fetchCommunityRated(0, 30)
  },
  { server: true, watch: [activeRanking] }
)

const rankedList = computed(() => {
  const items = data.value ?? []
  const mapped = items.map((a: AnimeDto) => {
    const liveVotes = liveVoteCounts.value[a.id]
      ?? Math.max(a.fireflyVoteCount ?? 0, dailyStore.getAnimeVotes(a.id))

    return {
      id: a.id,
      title: displayTitle(a),
      cover: a.coverImage,
      rating: formatScore(communityScore(a)),
      metricLabel: activeRanking.value === 'community'
          ? `${a.communityRatingCount ?? 0} 位站内评分`
          : `${liveVotes} 票`,
      fireflyVoteCount: liveVotes,
      type: tType(a.type),
      episodes: a.episodes ?? 0,
      studio: a.studio ?? '',
      coverImage: a.coverImage,
    }
  })

  return activeRanking.value === 'firefly'
    ? mapped.sort((a, b) => b.fireflyVoteCount - a.fireflyVoteCount)
    : mapped
})

function formatScore(score?: number) {
  return score ? (Math.round(score * 10) / 10).toFixed(1) : '0.0'
}

onMounted(() => {
  dailyStore.hydrate()
  dailyStore.refreshVoteLeaderboard()
})

onBeforeUnmount(() => {
  if (toastTimer) clearTimeout(toastTimer)
})

const { requireAuth } = useAuthModal()

function vote(anime: { id: number; title: string; cover: string }) {
  requireAuth(async () => {
    try {
      const ok = await dailyStore.voteForAnime(anime)
      if (ok) {
        liveVoteCounts.value = {
          ...liveVoteCounts.value,
          [anime.id]: Math.max(dailyStore.getAnimeVotes(anime.id), (liveVoteCounts.value[anime.id] ?? 0) + 1),
        }
        showToast(`已为 ${anime.title} 投出 1 票萤火`)
        refreshNuxtData('ranking-list')
        return
      }
      showToast('先去每日任务攒一张投票萤火吧')
    } catch {
      showToast('投票暂时没有送达，再试一次吧')
    }
  })
}

function showToast(message: string) {
  toast.value = message
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value = ''
  }, 2200)
}
</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
