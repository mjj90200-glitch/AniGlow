<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-32 pb-20">
    <section class="grid lg:grid-cols-[1.05fr,0.95fr] gap-6 lg:gap-8 items-stretch mb-10">
      <div class="glass-card-cream rounded-4xl p-7 sm:p-9 relative overflow-hidden animate-slide-in-left">
        <div
          class="absolute -right-20 -top-24 w-72 h-72 rounded-full pointer-events-none"
          style="background: rgba(0, 230, 118, 0.08); filter: blur(50px);"
        />
        <div class="relative">
          <span class="tag tag-new mb-5">
            <span class="firefly-dot mr-2" />
            Daily Firefly
          </span>
          <h1 class="text-4xl sm:text-5xl font-extrabold text-gray-800 leading-tight mb-4">
            今日番舍小任务
          </h1>
          <p class="text-gray-500 leading-relaxed max-w-2xl">
            每天做一点轻轻的互动：看看评论、写下一句理解、在番舍里慢慢逛一会儿。
            完成任务就能获得投票萤火，把今天的喜欢投给最想推上榜的番剧。
          </p>

          <div class="grid grid-cols-3 gap-3 mt-8">
            <div class="rounded-3xl bg-white/60 backdrop-blur-md p-4">
              <p class="text-xs text-gray-400">今日完成</p>
              <p class="text-2xl font-extrabold text-gray-800">
                {{ dailyStore.completedCount }}<span class="text-sm text-gray-400"> / {{ dailyStore.tasks.length }}</span>
              </p>
            </div>
            <div class="rounded-3xl bg-white/60 backdrop-blur-md p-4">
              <p class="text-xs text-gray-400">可用投票</p>
              <p class="text-2xl font-extrabold text-firefly-700">{{ dailyStore.state.tickets }}</p>
            </div>
            <div class="rounded-3xl bg-white/60 backdrop-blur-md p-4">
              <p class="text-xs text-gray-400">今日可得</p>
              <p class="text-2xl font-extrabold text-gray-800">{{ dailyStore.totalReward }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="glass-card rounded-4xl p-7 sm:p-8 animate-slide-in-right">
        <div class="flex items-center justify-between mb-5">
          <div>
            <p class="text-xs font-semibold tracking-widest uppercase mb-1" style="color: #00AA44;">
              Vote Tickets
            </p>
            <h2 class="text-2xl font-extrabold text-gray-800">投票萤火袋</h2>
          </div>
          <TicketCheck class="w-8 h-8 text-firefly" />
        </div>

        <div class="rounded-4xl p-5 mb-5"
             style="background: linear-gradient(135deg, rgba(0,230,118,0.14), rgba(160,216,239,0.16));">
          <div class="flex items-end justify-between gap-4">
            <div>
              <p class="text-sm text-gray-500 mb-1">当前可投</p>
              <p class="text-5xl font-black text-gray-800">{{ dailyStore.state.tickets }}</p>
            </div>
            <p class="text-sm font-bold text-firefly-700 mb-2">张番剧投票</p>
          </div>
        </div>

        <div class="space-y-3">
          <div
            v-for="task in dailyStore.tasks"
            :key="task.id"
            class="rounded-3xl bg-white/55 backdrop-blur-md p-4 border border-white/50"
          >
            <div class="flex items-start gap-3">
              <div
                class="w-10 h-10 rounded-2xl flex items-center justify-center shrink-0"
                :class="taskIconClass(task.accent)"
              >
                <CheckCircle2 v-if="dailyStore.isTaskCompleted(task.id)" class="w-5 h-5" />
                <component v-else :is="taskIcon(task.id)" class="w-5 h-5" />
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center justify-between gap-2">
                  <h3 class="font-extrabold text-gray-800">{{ task.title }}</h3>
                  <span
                    class="text-xs font-bold rounded-full px-2.5 py-1"
                    :class="dailyStore.isTaskCompleted(task.id) ? 'bg-firefly/10 text-firefly-700' : 'bg-white/70 text-gray-400'"
                  >
                    +{{ task.reward }} 投票
                  </span>
                </div>
                <p class="text-sm text-gray-500 leading-6 mt-1">{{ task.description }}</p>

                <div v-if="task.id === 'browse-five-minutes'" class="mt-3">
                  <div class="h-2 rounded-full bg-white/70 overflow-hidden">
                    <div
                      class="h-full rounded-full transition-all duration-500"
                      style="background: linear-gradient(90deg, #A0D8EF, #00E676);"
                      :style="{ width: `${Math.round(dailyStore.browseProgress * 100)}%` }"
                    />
                  </div>
                  <p class="text-[11px] text-gray-400 mt-1">
                    已浏览 {{ Math.floor(dailyStore.state.browseSeconds / 60) }} 分 {{ dailyStore.state.browseSeconds % 60 }} 秒
                  </p>
                </div>

                <NuxtLink
                  v-if="task.id === 'view-comments' && !dailyStore.isTaskCompleted(task.id)"
                  :to="firstAnime ? `/anime/${firstAnime.id}` : '/anime'"
                  class="inline-flex mt-3 text-xs font-bold text-firefly-700 hover:text-firefly-600"
                >
                  去看看评论区 →
                </NuxtLink>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="grid lg:grid-cols-[1fr,360px] gap-6 lg:gap-8">
      <div class="glass-card rounded-4xl p-6 sm:p-8">
        <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 mb-6">
          <div>
            <p class="text-xs font-semibold tracking-widest uppercase mb-1" style="color: #00AA44;">
              Vote For Love
            </p>
            <h2 class="text-2xl font-extrabold text-gray-800">把今天的喜欢投出去</h2>
          </div>
          <p class="text-sm text-gray-400">每张投票萤火可投 1 票</p>
        </div>

        <div v-if="pending" class="grid sm:grid-cols-2 xl:grid-cols-3 gap-4">
          <div v-for="n in 6" :key="n" class="h-56 rounded-3xl bg-white/40 animate-pulse" />
        </div>

        <div v-else class="grid sm:grid-cols-2 xl:grid-cols-3 gap-4">
          <article
            v-for="anime in voteCandidates"
            :key="anime.id"
            class="rounded-3xl bg-white/60 backdrop-blur-md border border-white/50 overflow-hidden group"
          >
            <NuxtLink :to="`/anime/${anime.id}`" class="block">
              <div class="relative aspect-[16/10] overflow-hidden">
                <img
                  :src="anime.cover"
                  :alt="anime.title"
                  class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                  loading="lazy"
                />
                <div class="absolute inset-0 bg-gradient-to-t from-black/55 via-black/5 to-transparent" />
                <div class="absolute left-4 bottom-4 right-4">
                  <h3 class="text-white font-extrabold line-clamp-1">{{ anime.title }}</h3>
                  <p class="text-white/75 text-xs mt-1">{{ anime.genreText }}</p>
                </div>
              </div>
            </NuxtLink>
            <div class="p-4 flex items-center justify-between gap-3">
              <div>
                <p class="text-xs text-gray-400">当前萤火票</p>
                <p class="text-xl font-extrabold text-gray-800">{{ dailyStore.getAnimeVotes(anime.id) }}</p>
              </div>
              <button
                class="btn-glow px-4 py-2.5"
                :disabled="dailyStore.state.tickets <= 0"
                :class="dailyStore.state.tickets <= 0 ? 'opacity-40 pointer-events-none' : ''"
                @click="vote(anime)"
              >
                投 1 票
              </button>
            </div>
          </article>
        </div>
      </div>

      <aside class="glass-card-cream rounded-4xl p-6 sm:p-7 h-fit">
        <div class="flex items-center gap-2 mb-5">
          <Crown class="w-6 h-6" style="color: #FFB300;" />
          <h2 class="text-xl font-extrabold text-gray-800">萤火投票榜</h2>
        </div>

        <div v-if="dailyStore.voteLeaderboard.length === 0" class="rounded-3xl bg-white/55 p-5 text-center">
          <Sparkles class="w-7 h-7 mx-auto mb-2 text-firefly" />
          <p class="text-sm text-gray-500">今天还没有人投票。第一簇萤火，等你点亮。</p>
        </div>

        <div v-else class="space-y-3">
          <NuxtLink
            v-for="(entry, index) in dailyStore.voteLeaderboard.slice(0, 6)"
            :key="entry.id"
            :to="`/anime/${entry.id}`"
            class="flex items-center gap-3 rounded-3xl bg-white/55 p-3 group"
          >
            <div class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-black shrink-0"
                 :style="{ background: index === 0 ? 'linear-gradient(135deg, #FFD54F, #FFB300)' : 'rgba(255,255,255,0.75)' }">
              {{ index + 1 }}
            </div>
            <img :src="entry.cover" :alt="entry.title" class="w-11 h-14 rounded-xl object-cover shrink-0" />
            <div class="min-w-0 flex-1">
              <p class="text-sm font-extrabold text-gray-800 line-clamp-1 group-hover:text-firefly-700">
                {{ entry.title }}
              </p>
              <p class="text-xs text-gray-400">{{ entry.votes }} 票萤火</p>
            </div>
          </NuxtLink>
        </div>
      </aside>
    </section>

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
import {
  CheckCircle2,
  Clock3,
  Crown,
  Eye,
  MessageCircle,
  Sparkles,
  TicketCheck,
} from 'lucide-vue-next'
import type { AnimeDto } from '~/types/anime'
import { displayTitle, mapGenres } from '~/composables/useAnimeI18n'
import type { DailyTaskId } from '~/stores/daily'

const dailyStore = useDailyStore()
const { fetchTopRated } = useAnime()
const toast = ref('')
let toastTimer: ReturnType<typeof setTimeout> | undefined

const { data, pending } = await useAsyncData(
  'daily-vote-candidates',
  () => fetchTopRated(0, 9),
  { server: true }
)

const voteCandidates = computed(() => (data.value ?? []).map((anime: AnimeDto) => ({
  id: anime.id,
  title: displayTitle(anime),
  cover: anime.coverImage,
  genreText: mapGenres(anime.genres ?? []).map(tag => tag.label).slice(0, 3).join(' · ') || '值得被更多人看见',
})))

const firstAnime = computed(() => voteCandidates.value[0])

onMounted(() => {
  dailyStore.hydrate()
  dailyStore.refreshVoteLeaderboard()
})

onBeforeUnmount(() => {
  if (toastTimer) clearTimeout(toastTimer)
})

function taskIcon(id: DailyTaskId) {
  return {
    'view-comments': Eye,
    'publish-comment': MessageCircle,
    'browse-five-minutes': Clock3,
  }[id]
}

function taskIconClass(accent: 'firefly' | 'sakura' | 'sky') {
  return {
    firefly: 'bg-firefly/15 text-firefly-700',
    sakura: 'bg-sakura/20 text-sakura-dark',
    sky: 'bg-sky/20 text-sky-dark',
  }[accent]
}

const { requireAuth } = useAuthModal()

function vote(anime: { id: number; title: string; cover: string }) {
  requireAuth(async () => {
    try {
      const ok = await dailyStore.voteForAnime(anime)
      showToast(ok ? `已把一簇萤火投给 ${anime.title}` : '先完成每日任务，攒一张投票萤火吧')
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

useHead({
  title: '每日任务 - 萤火番舍',
  meta: [
    {
      name: 'description',
      content: '完成每日番剧小任务，获得投票萤火，为喜欢的番剧投票排行。',
    },
  ],
})
</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
