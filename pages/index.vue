<template>
  <div class="pb-16">

    <AgentVotePanel :visible="showAgentVote" @close="showAgentVote = false" />

    <!-- ═══════════════════════════════════════════════════════════
         HERO SECTION · 非对称琉璃布局
         参考 TouchGal 的不对称 Hero 设计
         ═══════════════════════════════════════════════════════════ -->

    <section class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-12 lg:pt-36 lg:pb-20">
      <div class="grid lg:grid-cols-12 gap-6 lg:gap-8 items-stretch">

        <!-- ═══ 左卡片 · 主信息 ═══ -->
        <div
          class="lg:col-span-5 animate-slide-in-left"
        >
          <div class="glass-card-cream rounded-4xl p-8 sm:p-10 lg:p-12 h-full flex flex-col justify-center relative overflow-hidden">

            <!-- 卡片内的微光装饰 -->
            <div class="absolute -top-10 -right-10 w-40 h-40 rounded-full pointer-events-none"
                 style="background: rgba(0, 230, 118, 0.06); filter: blur(40px);" />

            <!-- 标签 -->
            <div class="mb-6">
              <span class="tag tag-new inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs">
                <span class="firefly-dot" />
                2026 夏季新番上线
              </span>
            </div>

            <!-- 标题 -->
            <h1 class="hero-title text-4xl sm:text-5xl lg:text-6xl text-gray-800 mb-6">
              萤火番舍
              <br />
              <span class="hero-gradient-text">
                ANI GLOW
              </span>
            </h1>

            <!-- 副标题 -->
            <p class="text-base lg:text-lg text-gray-500 leading-relaxed mb-10 max-w-md">
              以萤火般的微光，照亮每一部值得被看见的好番。
              探索、发现、投票，让好作品被更多人看见。
            </p>

            <!-- 按钮组 -->
            <div class="flex flex-col sm:flex-row gap-3.5 mt-auto">
              <NuxtLink
                to="/anime"
                class="btn-glow text-base px-8 py-4 gap-3 group"
              >
                <Tv class="w-5 h-5 transition-transform group-hover:scale-110 duration-300" />
                <span>浏览番剧</span>
              </NuxtLink>

              <button
                class="btn-sakura text-base px-8 py-4 gap-3 group"
                @click="showAgentVote = true"
              >
                <Bot class="w-5 h-5 transition-transform group-hover:scale-110 duration-300" />
                <span>AIGC 投票</span>
              </button>
            </div>
            <button
              class="mt-4 inline-flex w-fit items-center gap-2 text-xs font-extrabold text-gray-400 transition hover:text-firefly-700"
              @click="randomAnime"
            >
              <Dices class="h-4 w-4" />
              不知道看什么？随机遇见一部
            </button>

            <!-- 底部统计 -->
            <div class="flex items-center gap-6 mt-8 pt-6 border-t border-white/40">
              <div class="text-center">
                <div class="text-xl font-extrabold text-gray-800">{{ formatCompactNumber(homeStats.animeCount) }}</div>
                <div class="text-xs text-gray-400 mt-0.5">收录番剧</div>
              </div>
              <div class="w-px h-8 bg-gray-200" />
              <div class="text-center">
                <div class="text-xl font-extrabold text-gray-800">{{ formatCompactNumber(homeStats.agentCount) }}</div>
                <div class="text-xs text-gray-400 mt-0.5">驻站 AIGC</div>
              </div>
              <div class="w-px h-8 bg-gray-200" />
              <div class="text-center">
                <div class="text-xl font-extrabold text-gray-800">{{ formatCompactNumber(homeStats.memberCount) }}</div>
                <div class="text-xs text-gray-400 mt-0.5">社区成员</div>
              </div>
            </div>
          </div>
        </div>

        <!-- ═══ 右卡片 · 轮播海报 ═══ -->
        <div
          class="lg:col-span-7 animate-slide-in-right"
          style="animation-delay: 150ms"
        >
          <div class="glass-card rounded-4xl overflow-hidden h-full min-h-[420px] lg:min-h-full relative">

            <!-- 加载中 -->
            <div
              v-if="gridLoading && !carouselReady"
              class="absolute inset-0 flex items-center justify-center"
            >
              <div class="flex flex-col items-center gap-3">
                <div class="w-8 h-8 border-2 border-firefly/30 border-t-firefly rounded-full animate-spin" />
                <span class="text-sm text-gray-400">加载中...</span>
              </div>
            </div>

            <!-- 轮播容器 -->
            <div v-if="carouselReady" class="absolute inset-0">
              <Transition
                :name="transitionName"
                mode="out-in"
              >
                <div
                  :key="carousel[activeSlide].id"
                  class="absolute inset-0 cursor-pointer"
                  role="link"
                  tabindex="0"
                  :aria-label="`查看 ${carousel[activeSlide].title} 详情`"
                  @click="openActiveSlide"
                  @keydown.enter.prevent="openActiveSlide"
                >
                  <!-- 海报图片 -->
                  <img
                    :src="carousel[activeSlide].poster"
                    :alt="carousel[activeSlide].title"
                    class="w-full h-full object-cover"
                    @error="($event.target as HTMLImageElement).style.display='none'"
                  />

                  <!-- 渐变遮罩 · 底部信息 -->
                  <div class="absolute inset-0"
                       style="background: linear-gradient(180deg,
                         transparent 40%,
                         rgba(0,0,0,0.15) 60%,
                         rgba(0,0,0,0.55) 85%,
                         rgba(0,0,0,0.75) 100%);" />

                  <!-- 海报上的信息 -->
                  <div class="absolute bottom-0 left-0 right-0 p-8">
                    <div class="flex items-center gap-3 mb-2">
                      <span class="rating-badge text-sm">
                        <Star class="w-3.5 h-3.5" style="fill: currentColor;" />
                        {{ carousel[activeSlide].rating }}
                      </span>
                      <span class="text-white/80 text-xs font-medium">
                        {{ carousel[activeSlide].episodes }}集
                      </span>
                      <span
                        v-if="carousel[activeSlide].isNew"
                        class="tag tag-new text-[11px] px-2 py-0.5"
                      >
                        NEW
                      </span>
                    </div>
                    <h2 class="text-2xl font-extrabold text-white mb-2">
                      {{ carousel[activeSlide].title }}
                    </h2>
                    <p class="text-white/70 text-sm line-clamp-2 max-w-lg">
                      {{ carousel[activeSlide].description }}
                    </p>
                  </div>
                </div>
              </Transition>
            </div>

            <!-- 轮播指示器 -->
            <div class="absolute bottom-6 right-8 flex items-center gap-2 z-20">
              <button
                v-for="(slide, i) in carousel"
                :key="i"
                @click="activeSlide = i"
                class="transition-all duration-300 rounded-full"
                :class="[
                  activeSlide === i
                    ? 'w-8 h-2.5 bg-white'
                    : 'w-2.5 h-2.5 bg-white/40 hover:bg-white/70'
                ]"
                :aria-label="`跳转到第 ${i + 1} 张海报`"
              />
            </div>

            <!-- 左右箭头 -->
            <button
              class="absolute left-4 top-1/2 -translate-y-1/2 z-20 w-10 h-10 rounded-full
                     bg-white/20 backdrop-blur-md hover:bg-white/40
                     flex items-center justify-center transition-all duration-300"
              @click="prevSlide"
              aria-label="上一张"
            >
              <ArrowLeft class="w-5 h-5 text-white" />
            </button>
            <button
              class="absolute right-4 top-1/2 -translate-y-1/2 z-20 w-10 h-10 rounded-full
                     bg-white/20 backdrop-blur-md hover:bg-white/40
                     flex items-center justify-center transition-all duration-300"
              @click="nextSlide"
              aria-label="下一张"
            >
              <ArrowRight class="w-5 h-5 text-white" />
            </button>
          </div>
        </div>

      </div>

      <!-- 向下滚动提示 -->
      <div class="flex justify-center mt-10 lg:mt-14">
        <div class="flex flex-col items-center gap-2 animate-float">
          <span class="text-xs text-gray-400 font-medium tracking-wide">向下探索</span>
          <ChevronDown class="w-5 h-5 text-gray-400" />
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════════════════════
         ANIME GRID · 本周热门番剧网格
         TouchGal 风格卡片设计
         ═══════════════════════════════════════════════════════════ -->

    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 pb-20">
      <!-- Section Header -->
      <div class="flex items-end justify-between mb-10">
        <div>
          <p class="text-xs font-semibold tracking-widest uppercase mb-2"
             style="color: #00AA44;">
             Trending This Week
          </p>
          <h2 class="text-3xl font-extrabold text-gray-800">
            本周热门
          </h2>
        </div>
        <NuxtLink
          to="/ranking"
          class="btn-glass text-sm"
        >
          查看全部排行
          <ArrowRight class="w-4 h-4" />
        </NuxtLink>
      </div>

      <HomeAnimeGrid :loading="gridLoading" :anime-list="animeList" />
    </section>

    <!-- ═══════════════════════════════════════════════════════════
         FEATURES · 特色板块
         ═══════════════════════════════════════════════════════════ -->

    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
      <div class="text-center mb-8 sm:mb-14">
        <p class="text-xs font-semibold tracking-widest uppercase mb-2"
           style="color: #00AA44;">
          Discover More
        </p>
        <h2 class="text-3xl font-extrabold text-gray-800 mb-3">
          探索更多精彩
        </h2>
        <p class="text-gray-500 max-w-xl mx-auto text-sm leading-relaxed">
          无论你喜欢热血冒险、温馨日常，还是治愈系作品，这里都有属于你的那一部
        </p>
      </div>

      <div class="grid grid-cols-3 gap-3 sm:gap-5 lg:gap-6">
        <div
          v-for="(feature, i) in features"
          :key="i"
          class="glass-card rounded-2xl sm:rounded-3xl p-3 sm:p-8 text-center group cursor-default
                 transition-all duration-500 hover:-translate-y-2"
        >
          <!-- 图标 -->
          <div
            class="w-10 h-10 sm:w-16 sm:h-16 mx-auto mb-2 sm:mb-6 rounded-xl sm:rounded-2xl flex items-center justify-center
                   transition-all duration-300 group-hover:scale-110 group-hover:shadow-glow"
            :style="{ background: feature.bgGradient }"
          >
            <component :is="feature.icon" class="w-4 h-4 sm:w-7 sm:h-7" :style="{ color: feature.iconColor }" />
          </div>

          <h3 class="text-xs sm:text-lg font-extrabold text-gray-800 mb-0.5 sm:mb-3">{{ feature.title }}</h3>
          <p class="text-gray-500 text-xs sm:text-sm leading-relaxed hidden sm:block">{{ feature.desc }}</p>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════════════════════
         COMMUNITY · 社区盒子预览
         ═══════════════════════════════════════════════════════════ -->

    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
      <div class="flex items-end justify-between mb-10">
        <div>
          <p class="text-xs font-semibold tracking-widest uppercase mb-2"
             style="color: #00AA44;">
            Community Boxes
          </p>
          <h2 class="text-3xl font-extrabold text-gray-800">
            社区盒子
          </h2>
        </div>
        <NuxtLink
          to="/community"
          class="btn-glass text-sm"
        >
          查看全部社区
          <ArrowRight class="w-4 h-4" />
        </NuxtLink>
      </div>

      <HomeCommunityBoxes :loading="communitiesPending" :communities="communityCards" />
    </section>

  </div>
</template>

<!-- ═══════════════════════════════════════════════════════════════
     SCRIPT
     ═══════════════════════════════════════════════════════════════ -->

<script setup lang="ts">
import {
  Dices,
  Bot,
  Star,
  ChevronDown,
  ArrowLeft,
  ArrowRight,
  Flame,
  Heart,
  Sparkles,
  Users,
  MessageCircle,
  Tv,
} from 'lucide-vue-next'
import type { AnimeDto } from '~/types/anime'
import { mapGenres, tagClass, displayTitle, displaySynopsis, communityScore } from '~/composables/useAnimeI18n'
import { useCommunity } from '~/composables/useCommunity'
import AgentVotePanel from '~/components/home/AgentVotePanel.vue'

const showAgentVote = ref(false)

// ═══════════════════════════════════════════════════════════════
// 数据获取 · 从后端 API 拉取真实动漫数据
// ═══════════════════════════════════════════════════════════════

const { fetchTopRated } = useAnime()
const { fetchCommunities } = useCommunity()
const { request } = useApi()

interface HomeStats {
  animeCount: number
  agentCount: number
  memberCount: number
}

const { data: homeStatsData } = await useAsyncData(
  'home-public-stats',
  async (): Promise<HomeStats> => {
    try {
      return await request<HomeStats>('/stats/public')
    } catch {
      return { animeCount: 0, agentCount: 0, memberCount: 0 }
    }
  },
  { server: true }
)

const homeStats = computed(() => homeStatsData.value ?? { animeCount: 0, agentCount: 0, memberCount: 0 })

// 获取 Top 12 高分动漫（网格数据）
const { data: topAnimeData, pending: gridLoading } = await useAsyncData(
  'home-top-rated',
  () => fetchTopRated(0, 12),
  { server: true }
)

// 获取 Top 3 作为轮播数据
const { data: carouselData } = await useAsyncData(
  'home-carousel',
  () => fetchTopRated(0, 3),
  { server: true }
)

// 获取社区盒子列表（最多 4 个）
const { data: communitiesData, pending: communitiesPending } = await useAsyncData(
  'home-communities',
  () => fetchCommunities(),
  { server: true }
)

interface CommunityCard {
  slug: string
  name: string
  description: string
  coverImage: string
  postCount: number
  memberCount: number
}

const communityCards = computed<CommunityCard[]>(() => {
  return (communitiesData.value ?? []).slice(0, 4).map(c => ({
    slug: c.slug,
    name: c.name,
    description: c.description || '与同好们一起交流讨论',
    coverImage: c.coverImage || '',
    postCount: c.postCount || 0,
    memberCount: c.memberCount || 0,
  }))
})

// ═══════════════════════════════════════════════════════════════
// Genre 映射 · MAL 英文 → 中文标签 + 糖果色类型
// ═══════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════
// 轮播数据 · 从 API 数据构建
// ═══════════════════════════════════════════════════════════════

interface CarouselSlide {
  id: number
  title: string
  poster: string
  rating: number
  episodes: number
  isNew: boolean
  description: string
}

const carousel = computed<CarouselSlide[]>(() => {
  const items = carouselData.value ?? []
  return items.map(a => ({
    id: a.id,
    title: displayTitle(a),
    poster: a.coverImage,
    rating: communityScore(a),
    episodes: a.episodes ?? 0,
    isNew: a.status === 'Currently Airing',
    description: displaySynopsis(a).slice(0, 120),
  }))
})

// 轮播为空时的占位
const carouselReady = computed(() => carousel.value.length > 0)

const activeSlide = ref(0)
const transitionName = ref('carousel-forward')
let autoTimer: ReturnType<typeof setInterval> | null = null

const nextSlide = () => {
  if (!carouselReady.value) return
  transitionName.value = 'carousel-forward'
  activeSlide.value = (activeSlide.value + 1) % carousel.value.length
  resetAutoPlay()
}

const prevSlide = () => {
  if (!carouselReady.value) return
  transitionName.value = 'carousel-backward'
  activeSlide.value = (activeSlide.value - 1 + carousel.value.length) % carousel.value.length
  resetAutoPlay()
}

const resetAutoPlay = () => {
  if (autoTimer) clearInterval(autoTimer)
  if (carouselReady.value) {
    autoTimer = setInterval(nextSlide, 5000)
  }
}

const openActiveSlide = () => {
  if (!carouselReady.value) return
  navigateTo(`/anime/${carousel.value[activeSlide.value].id}`)
}

onMounted(async () => {
  // 生产环境里如果首屏 SSR 恰好碰上后端启动慢，客户端再补拉一次，避免首页模块空白。
  const needsHydrationRefresh =
    (topAnimeData.value?.length ?? 0) === 0 ||
    (carouselData.value?.length ?? 0) === 0 ||
    (homeStatsData.value?.animeCount ?? 0) === 0 ||
    (communitiesData.value?.length ?? 0) === 0

  if (needsHydrationRefresh) {
    await Promise.allSettled([
      refreshNuxtData('home-public-stats'),
      refreshNuxtData('home-top-rated'),
      refreshNuxtData('home-carousel'),
      refreshNuxtData('home-communities'),
    ])
  }

  resetAutoPlay()
})

onUnmounted(() => {
  if (autoTimer) clearInterval(autoTimer)
})

// ═══════════════════════════════════════════════════════════════
// 动漫网格 · 从 API 数据构建
// ═══════════════════════════════════════════════════════════════

interface AnimeCard {
  id: number
  title: string
  cover: string
  rating: number
  tags: { label: string; type: string }[]
  hasAgent: boolean
}

const animeList = computed<AnimeCard[]>(() => {
  const items = topAnimeData.value ?? []
  return items.map(a => ({
    id: a.id,
    title: displayTitle(a),
    cover: a.coverImage,
    rating: communityScore(a),
    tags: mapGenres(a.genres ?? []),
    hasAgent: !!a.hasAgent,
  }))
})

// ═══════════════════════════════════════════════════════════════
// 随机一部番
// ═══════════════════════════════════════════════════════════════

const randomAnime = () => {
  const list = animeList.value
  if (list.length === 0) return
  const randomId = list[Math.floor(Math.random() * list.length)].id
  navigateTo(`/anime/${randomId}`)
}

const formatCompactNumber = (value?: number) => {
  const n = value ?? 0
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`
  return String(n)
}

// ═══════════════════════════════════════════════════════════════
// 特色板块
// ═══════════════════════════════════════════════════════════════

const features = [
  {
    title: '热门推荐',
    desc: '精心挑选的当季热门番剧，紧跟动漫潮流，不错过任何一部口碑佳作',
    icon: Flame,
    bgGradient: 'linear-gradient(135deg, rgba(255, 179, 126, 0.2), rgba(255, 213, 79, 0.15))',
    iconColor: '#E07B3E',
  },
  {
    title: '治愈系',
    desc: '温暖人心的治愈作品，在繁忙生活中找到片刻宁静与感动',
    icon: Heart,
    bgGradient: 'linear-gradient(135deg, rgba(255, 192, 203, 0.2), rgba(255, 154, 174, 0.15))',
    iconColor: '#D0707F',
  },
  {
    title: '经典必看',
    desc: '历经时间考验的经典之作，每一部都值得反复品味与珍藏',
    icon: Sparkles,
    bgGradient: 'linear-gradient(135deg, rgba(0, 230, 118, 0.15), rgba(0, 200, 83, 0.1))',
    iconColor: '#00AA44',
  },
]
</script>

<!-- ═══════════════════════════════════════════════════════════════
     STYLE · 轮播过渡动画
     ═══════════════════════════════════════════════════════════════ -->

<style scoped>
/* 轮播前进动画 */
.carousel-forward-enter-active {
  animation: carouselInRight 0.55s cubic-bezier(0.16, 1, 0.3, 1);
}
.carousel-forward-leave-active {
  animation: carouselOutLeft 0.4s cubic-bezier(0.5, 0, 0.75, 0);
}

/* 轮播后退动画 */
.carousel-backward-enter-active {
  animation: carouselInLeft 0.55s cubic-bezier(0.16, 1, 0.3, 1);
}
.carousel-backward-leave-active {
  animation: carouselOutRight 0.4s cubic-bezier(0.5, 0, 0.75, 0);
}

@keyframes carouselInRight {
  from {
    opacity: 0;
    transform: translateX(30px) scale(1.03);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes carouselOutLeft {
  from {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateX(-30px) scale(0.97);
  }
}

@keyframes carouselInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px) scale(1.03);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes carouselOutRight {
  from {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateX(30px) scale(0.97);
  }
}

/* 行限制 */
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
