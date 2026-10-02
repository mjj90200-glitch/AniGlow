<template>
  <div class="min-h-screen pb-20">
    <div v-if="pending" class="max-w-7xl mx-auto px-6 pt-32">
      <div class="glass-card rounded-4xl p-10 animate-pulse">
        <div class="h-8 w-52 rounded-full bg-cream-200 mb-6" />
        <div class="grid md:grid-cols-[220px,1fr] gap-8">
          <div class="aspect-[3/4] rounded-3xl bg-cream-200" />
          <div class="space-y-4">
            <div class="h-10 w-2/3 rounded-full bg-cream-200" />
            <div class="h-4 w-full rounded-full bg-cream-200" />
            <div class="h-4 w-5/6 rounded-full bg-cream-200" />
            <div class="h-12 w-44 rounded-full bg-cream-200 mt-8" />
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="!anime" class="max-w-3xl mx-auto px-6 pt-36 text-center">
      <div class="glass-card-cream rounded-4xl p-10">
        <Sparkles class="w-10 h-10 mx-auto mb-4 text-firefly" />
        <h1 class="text-2xl font-extrabold text-gray-800 mb-3">没有找到这部作品</h1>
        <p class="text-gray-500 mb-6">可能是后端服务暂时不可用，或者这部番剧还没有同步进番舍。</p>
        <NuxtLink to="/anime" class="btn-glow">回到番剧列表</NuxtLink>
      </div>
    </div>

    <template v-else>
      <AnimeHero :anime="anime" :mapped-genres="mappedGenres" :synopsis="synopsis" :score-text="scoreText" />

      <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div class="grid lg:grid-cols-[1fr,340px] gap-6 lg:gap-8">
          <div class="space-y-6">
            <AnimeDetailTabs
              :anime="anime"
              :synopsis="synopsis"
              :aigc-characters="aigcCharacters"
              :related-community-posts="relatedCommunityPosts"
              :related-community-path="relatedCommunityPath"
            />

            <AnimeReviewPanel
              :anime-id="animeId"
              :anime="anime"
              @anime-updated="animeData = $event"
            />
          </div>

          <AnimeSidebar
            :current-votes="currentFireflyVotes"
            :available-tickets="dailyStore.state.tickets"
            :voting="voting"
            :vote-message="voteMessage"
            :vote-error="voteError"
            :info-rows="infoRows"
            :related-anime="relatedAnime"
            @vote="voteForCurrentAnime"
          />
        </div>
      </section>
    </template>

  </div>
</template>

<script setup lang="ts">
import { Sparkles } from 'lucide-vue-next'
import type { CommunityPostDto } from '~/composables/useCommunity'
import {
  displaySynopsis,
  displayTitle,
  communityScore,
  formatNumber,
  mapGenres,
  tType,
} from '~/composables/useAnimeI18n'

interface AigcCharacter {
  code: string
  displayName: string
  sourceTitle?: string | null
  avatarUrl?: string | null
}

const route = useRoute()
const animeId = computed(() => Number(route.params.id))
const {
  fetchAnimeById,
  fetchTopRated,
} = useAnime()
const dailyStore = useDailyStore()
const { fetchPostsByAnime } = useCommunity()
const { request } = useApi()
const { data: animeData, pending } = await useAsyncData(
  `anime-detail-${animeId.value}`,
  () => fetchAnimeById(animeId.value),
  { server: true, watch: [animeId] }
)

const { data: relatedData } = await useAsyncData(
  'anime-related-top',
  () => fetchTopRated(0, 8),
  { server: true }
)

const { data: aigcCharactersData } = await useAsyncData(
  `anime-aigc-characters-${animeId.value}`,
  async () => {
    if (!animeId.value) return []
    try {
      return await request<AigcCharacter[]>(`/agent/characters/anime/${animeId.value}`)
    } catch {
      return []
    }
  },
  { server: true, watch: [animeId] }
)

const { data: relatedCommunityData } = await useAsyncData(
  `anime-community-posts-${animeId.value}`,
  () => fetchPostsByAnime(animeId.value, 0, 4),
  { server: true, watch: [animeId] }
)

const anime = computed(() => animeData.value)
const synopsis = computed(() => anime.value ? displaySynopsis(anime.value) || '这部作品还没有同步到简介，但同好们的讨论已经在发光。' : '')
const mappedGenres = computed(() => anime.value ? mapGenres(anime.value.genres ?? []) : [])
const scoreText = computed(() => anime.value ? formatScore(communityScore(anime.value)) : '0.0')
const relatedAnime = computed(() => (relatedData.value ?? []).filter(item => item.id !== animeId.value).slice(0, 4))
const aigcCharacters = computed(() => aigcCharactersData.value ?? [])
const relatedCommunityPosts = computed<CommunityPostDto[]>(() => relatedCommunityData.value?.items ?? [])
const relatedCommunityPath = computed(() => {
  const slug = relatedCommunityPosts.value[0]?.communitySlug
  return slug ? `/community/${slug}` : '/community/anime'
})
const currentFireflyVotes = computed(() => anime.value?.fireflyVoteCount ?? dailyStore.getAnimeVotes(animeId.value))

const voting = ref(false)
const voteMessage = ref('')
const voteError = ref('')

const infoRows = computed(() => {
  if (!anime.value) return []
  return [
    { label: '首播时间', value: anime.value.airedFrom || anime.value.year || '未知' },
    { label: '集数', value: anime.value.episodes ? `${anime.value.episodes} 集` : '未知' },
    { label: '类型', value: tType(anime.value.type) || '未知' },
    { label: '制作公司', value: anime.value.studio || '未知' },
    { label: '原作来源', value: anime.value.source || '未知' },
    { label: '人气收藏', value: formatNumber(anime.value.favoritesCount) },
  ]
})

const { requireAuth } = useAuthModal()

function voteForCurrentAnime() {
  requireAuth(() => doVoteForCurrentAnime())
}

async function doVoteForCurrentAnime() {
  if (!anime.value || voting.value || dailyStore.state.tickets <= 0) return
  voting.value = true
  voteMessage.value = ''
  voteError.value = ''

  try {
    const voted = await dailyStore.voteForAnime({
      id: anime.value.id,
      title: displayTitle(anime.value),
      cover: anime.value.coverImage,
    })
    if (!voted) return
    const latestAnime = await fetchAnimeById(anime.value.id)
    if (latestAnime) animeData.value = latestAnime
    voteMessage.value = '投票成功，你的萤火已经计入全站排行'
  } catch (e: any) {
    voteError.value = e?.message || '投票暂时没有送达，请稍后再试'
  } finally {
    voting.value = false
  }
}

function formatScore(score?: number) {
  return score ? (Math.round(score * 10) / 10).toFixed(1) : '0.0'
}

useHead(() => ({
  title: anime.value ? `${displayTitle(anime.value)} - 萤火番舍` : '作品详情 - 萤火番舍',
  meta: [
    {
      name: 'description',
      content: synopsis.value.slice(0, 100),
    },
  ],
}))

onMounted(() => {
  dailyStore.hydrate()
  dailyStore.refreshVoteLeaderboard()
})
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
