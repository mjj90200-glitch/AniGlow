<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-32 pb-20">
    <!-- 页面标题 -->
    <div class="mb-10">
      <p class="text-xs font-semibold tracking-widest uppercase mb-2" style="color: #00AA44;">
        Browse All
      </p>
      <h1 class="text-3xl font-extrabold text-gray-800">全部番剧</h1>
      <p class="mt-3 max-w-2xl text-sm text-gray-500 leading-7">
        接入 AniList 数据源，{{ totalResults > 0 ? totalResults : '6000+' }} 部番剧持续更新，按题材、地区、年份挑一部今天想看的作品吧。
      </p>
    </div>

    <!-- 搜索框 -->
    <section class="mb-6">
      <div class="relative">
        <Search class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
        <input
          ref="searchInputRef"
          v-model="searchInput"
          type="text"
          placeholder="搜索你喜欢的番剧..."
          class="w-full pl-12 pr-12 py-3.5 rounded-2xl border border-white/70 bg-white/60 backdrop-blur-xl
                 text-sm text-gray-700 placeholder-gray-400 outline-none
                 transition-all duration-300
                 focus:border-firefly/40 focus:ring-4 focus:ring-firefly/10 focus:bg-white/80
                 shadow-[0_10px_30px_rgba(31,68,47,0.06)]"
        />
        <button
          v-if="searchInput"
          @click="searchInput = ''"
          class="absolute right-4 top-1/2 -translate-y-1/2 p-1 rounded-full
                 text-gray-400 hover:text-gray-600 hover:bg-gray-100/50 transition-colors"
          aria-label="清除搜索"
        >
          <X class="w-4 h-4" />
        </button>
      </div>
    </section>

    <!-- 综合筛选区（题材 / 地区 / 年份 / 格式） -->
    <section class="mb-8 rounded-[2rem] border border-white/70 bg-white/60 backdrop-blur-xl p-4 sm:p-5 shadow-[0_18px_50px_rgba(31,68,47,0.08)]">
      <div class="flex items-center justify-between gap-3 mb-4">
        <div>
          <p class="text-xs font-bold tracking-[0.22em] uppercase text-firefly-600">Filter Garden</p>
          <h2 class="text-lg font-extrabold text-gray-800 mt-1">筛选</h2>
        </div>
        <div class="flex items-center gap-2">
          <span class="hidden sm:inline-flex items-center rounded-full bg-firefly-50 px-3 py-1 text-xs font-bold text-firefly-700">
            {{ activeCategory.label }}
          </span>
          <button
            v-if="hasActiveFilter"
            type="button"
            class="rounded-full border border-white/70 bg-white/60 px-3 py-1 text-xs font-bold
                   text-gray-500 hover:text-firefly-600 hover:border-firefly/30 transition-colors"
            @click="resetFilters"
          >
            重置筛选
          </button>
        </div>
      </div>

      <!-- 题材 -->
      <div class="flex items-start gap-3 mb-3">
        <span class="filter-row-label">题材</span>
        <div class="flex flex-1 flex-wrap gap-2">
          <button
            v-for="category in categories"
            :key="category.genre || 'all'"
            type="button"
            class="category-chip"
            :class="{ 'category-chip-active': activeGenre === category.genre }"
            @click="activeGenre = category.genre"
          >
            <span>{{ category.icon }}</span>
            {{ category.label }}
          </button>
        </div>
      </div>

      <!-- 地区 -->
      <div class="flex items-start gap-3 mb-3">
        <span class="filter-row-label">地区</span>
        <div class="flex flex-1 flex-wrap gap-2">
          <button
            v-for="option in countryOptions"
            :key="'c-' + (option.value || 'all')"
            type="button"
            class="filter-chip"
            :class="{ 'category-chip-active': activeCountry === option.value }"
            @click="activeCountry = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <!-- 格式 -->
      <div class="flex items-start gap-3 mb-3">
        <span class="filter-row-label">格式</span>
        <div class="flex flex-1 flex-wrap gap-2">
          <button
            v-for="option in typeOptions"
            :key="'t-' + (option.value || 'all')"
            type="button"
            class="filter-chip"
            :class="{ 'category-chip-active': activeType === option.value }"
            @click="activeType = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <!-- 年份 -->
      <div class="flex items-start gap-3">
        <span class="filter-row-label">年份</span>
        <div class="flex flex-1 flex-wrap gap-2">
          <button
            v-for="option in yearChips"
            :key="'y-' + option.label"
            type="button"
            class="filter-chip"
            :class="{ 'category-chip-active': activeYearFrom === option.from && activeYearTo === option.to }"
            @click="activeYearFrom = option.from; activeYearTo = option.to"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </section>

    <!-- 搜索/筛选结果提示 -->
    <div
      v-if="debouncedSearch || hasActiveFilter"
      class="mb-6 flex items-center justify-between"
    >
      <p class="text-sm text-gray-500">
        <template v-if="debouncedSearch">
          搜索 "<span class="font-bold text-gray-700">{{ debouncedSearch }}</span>"
        </template>
        <template v-else>当前筛选</template>
        共找到 <span class="font-bold text-firefly-700">{{ totalResults }}</span> 部番剧
      </p>
      <button
        @click="resetFilters"
        class="text-xs font-bold text-gray-400 hover:text-firefly-600 transition-colors"
      >
        清除全部条件
      </button>
    </div>

    <!-- 加载中 -->
    <div
      v-if="pending"
      class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5 lg:gap-6"
    >
      <div
        v-for="n in 12"
        :key="'skel-' + n"
        class="rounded-3xl bg-white/40 backdrop-blur-sm animate-pulse"
      >
        <div class="aspect-[3/4] bg-cream-200 rounded-t-3xl" />
        <div class="p-4 space-y-2">
          <div class="h-4 bg-cream-200 rounded-full w-3/4" />
          <div class="flex gap-1.5">
            <div class="h-5 bg-cream-200 rounded-full w-12" />
            <div class="h-5 bg-cream-200 rounded-full w-14" />
          </div>
        </div>
      </div>
    </div>

    <!-- 动漫网格 -->
    <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5 lg:gap-6">
      <article
        v-for="(anime, index) in animeCards"
        :key="anime.id"
        class="anime-card animate-scale-in group"
        :style="{ animationDelay: `${(index % 20) * 60}ms` }"
      >
        <NuxtLink :to="`/anime/${anime.id}`" class="block">
          <div class="relative aspect-[3/4] overflow-hidden rounded-t-3xl bg-cream-200">
            <img
              :src="anime.cover"
              :alt="anime.title"
              class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
              loading="lazy"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <div class="absolute top-3 right-3 z-10">
              <div class="rating-badge text-xs px-2.5 py-1">
                <Star class="w-3 h-3" style="fill: #5D4037;" />
                {{ anime.rating }}
              </div>
            </div>
            <div
              v-if="anime.hasAgent"
              class="absolute top-3 left-3 z-10 flex items-center gap-1.5 rounded-full
                     border border-white/60 bg-white/80 px-2.5 py-1 text-[11px] font-bold
                     text-firefly-700 backdrop-blur-md"
            >
              <span class="firefly-dot" />
              AIGC
            </div>
          </div>
          <div class="p-4">
            <h3 class="font-extrabold text-gray-800 text-sm mb-2.5 line-clamp-1
                       group-hover:text-firefly-600 transition-colors duration-300">
              {{ anime.title }}
            </h3>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="tag in anime.tags"
                :key="tag.label"
                class="tag"
                :class="tagClass(tag.type)"
              >
                {{ tag.label }}
              </span>
            </div>
          </div>
        </NuxtLink>
      </article>
    </div>

    <!-- 空状态 -->
    <div
      v-if="!pending && animeCards.length === 0"
      class="text-center py-20"
    >
      <p v-if="debouncedSearch" class="text-gray-400">
        没有找到与 "{{ debouncedSearch }}" 相关的番剧，试试其他关键词吧
      </p>
      <p v-else class="text-gray-400">这个筛选组合暂时没有数据，换个条件试试看吧</p>
    </div>

    <!-- 分页 -->
    <nav
      v-if="totalPages > 1"
      class="flex items-center justify-center gap-2 mt-12"
      aria-label="分页导航"
    >
      <button
        :disabled="currentPage === 0"
        class="pagination-btn group"
        @click="goToPage(currentPage - 1)"
        aria-label="上一页"
      >
        <ChevronLeft class="w-5 h-5 group-disabled:opacity-30" />
      </button>

      <template v-for="page in visiblePages" :key="page">
        <span
          v-if="page === '...'"
          class="w-10 h-10 flex items-center justify-center text-gray-400 text-sm font-bold"
        >...</span>
        <button
          v-else
          class="pagination-btn"
          :class="{ 'pagination-btn-active': page === currentPage }"
          @click="goToPage(page as number)"
          :aria-label="`第 ${(page as number) + 1} 页`"
          :aria-current="page === currentPage ? 'page' : undefined"
        >
          {{ (page as number) + 1 }}
        </button>
      </template>

      <button
        :disabled="currentPage >= totalPages - 1"
        class="pagination-btn group"
        @click="goToPage(currentPage + 1)"
        aria-label="下一页"
      >
        <ChevronRight class="w-5 h-5 group-disabled:opacity-30" />
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ChevronLeft, ChevronRight, Search, Star, X } from 'lucide-vue-next'

definePageMeta({ keepalive: true })
import type { AnimeDto, AnimeListResponse } from '~/types/anime'
import { mapGenres, tagClass, displayTitle, communityScore } from '~/composables/useAnimeI18n'

const { fetchFilterOptions, fetchFilteredAnime } = useAnime()
const route = useRoute()

const PAGE_SIZE = 60

// ── 题材分类（AniList/MAL 通用题材名，确保筛选结果准确） ──
const categories = [
  { label: '全部', genre: '', icon: '✦' },
  { label: '热血', genre: 'Action', icon: '⚡' },
  { label: '冒险', genre: 'Adventure', icon: '✈' },
  { label: '搞笑', genre: 'Comedy', icon: '☺' },
  { label: '恋爱', genre: 'Romance', icon: '♡' },
  { label: '奇幻', genre: 'Fantasy', icon: '✧' },
  { label: '科幻', genre: 'Sci-Fi', icon: '⌁' },
  { label: '日常', genre: 'Slice of Life', icon: '☁' },
  { label: '校园', genre: 'School', icon: '♧' },
  { label: '治愈', genre: 'Iyashikei', icon: '蛍' },
  { label: '异世界', genre: 'Isekai', icon: '◇' },
  { label: '音乐', genre: 'Music', icon: '♪' },
  { label: '运动', genre: 'Sports', icon: '★' },
  { label: '悬疑', genre: 'Mystery', icon: '?' },
  { label: '心理', genre: 'Psychological', icon: '◈' },
  { label: '超自然', genre: 'Supernatural', icon: '✜' },
  { label: '恐怖', genre: 'Horror', icon: '☾' },
  { label: '惊悚', genre: 'Thriller', icon: '⌖' },
  { label: '剧情', genre: 'Drama', icon: '❖' },
  { label: '机甲', genre: 'Mecha', icon: '⚙' },
  { label: '魔法少女', genre: 'Mahou Shoujo', icon: '☆' },
]

// ── 格式标签映射 ──
const TYPE_LABELS: Record<string, string> = {
  TV: 'TV动画',
  Movie: '剧场版',
  ONA: 'ONA',
  OVA: 'OVA',
  Special: '特别篇',
  Music: '音乐',
}

// ── 筛选状态（初始化自 URL，修改时同步回 URL，保证从详情页返回时恢复原状态） ──
const activeGenre = ref(typeof route.query.genre === 'string' ? route.query.genre : '')
const activeType = ref(typeof route.query.type === 'string' ? route.query.type : '')
const activeCountry = ref(typeof route.query.country === 'string' ? route.query.country : '')
const activeYearFrom = ref<number | null>(typeof route.query.yearFrom === 'string' && route.query.yearFrom ? Number(route.query.yearFrom) : null)
const activeYearTo = ref<number | null>(typeof route.query.yearTo === 'string' && route.query.yearTo ? Number(route.query.yearTo) : null)
const currentPage = ref(pageFromQuery())
const searchInputRef = ref<HTMLInputElement | null>(null)
const searchInput = ref(typeof route.query.q === 'string' ? route.query.q : '')
const debouncedSearch = refDebounced(searchInput, 300)

function pageFromQuery(): number {
  const p = Number(route.query.page)
  return Number.isFinite(p) && p > 0 ? p - 1 : 0
}

const router = useRouter()

/** 筛选/页码变化时同步到 URL（replace 不产生历史记录垃圾） */
watch([debouncedSearch, activeGenre, activeType, activeCountry, activeYearFrom, activeYearTo, currentPage], () => {
  const query: Record<string, string> = {}
  if (debouncedSearch.value) query.q = debouncedSearch.value
  if (activeGenre.value) query.genre = activeGenre.value
  if (activeType.value) query.type = activeType.value
  if (activeCountry.value) query.country = activeCountry.value
  if (activeYearFrom.value !== null) query.yearFrom = String(activeYearFrom.value)
  if (activeYearTo.value !== null) query.yearTo = String(activeYearTo.value)
  if (currentPage.value > 0) query.page = String(currentPage.value + 1)
  router.replace({ query })
})

// ── 年份芯片：近年逐年份 + 更早按年代区间（与 B 站一致） ──
const YEAR_RANGES: { label: string; from: number | null; to: number }[] = [
  { label: '2014-2010', from: 2010, to: 2014 },
  { label: '2009-2005', from: 2005, to: 2009 },
  { label: '2004-2000', from: 2000, to: 2004 },
  { label: '90年代', from: 1990, to: 1999 },
  { label: '80年代', from: 1980, to: 1989 },
  { label: '更早', from: null, to: 1979 },
]

watch(() => route.query.q, (query) => {
  searchInput.value = typeof query === 'string' ? query : ''
})

onMounted(() => {
  if (route.query.focus === 'search') {
    requestAnimationFrame(() => searchInputRef.value?.focus())
  }
})

function resetFilters() {
  searchInput.value = ''
  activeGenre.value = ''
  activeType.value = ''
  activeCountry.value = ''
  activeYearFrom.value = null
  activeYearTo.value = null
}

const hasActiveFilter = computed(() =>
  !!debouncedSearch.value || !!activeGenre.value || !!activeType.value
  || !!activeCountry.value || activeYearFrom.value !== null || activeYearTo.value !== null,
)

function goToPage(page: number) {
  currentPage.value = Math.max(0, Math.min(page, totalPages.value - 1))
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// ═══════ 筛选选项集 ═══════

const { data: filterOptions } = await useAsyncData(
  'anime-filter-options',
  () => fetchFilterOptions(),
  { server: true },
)

const countryOptions = computed(() => {
  const all = filterOptions.value?.countries ?? []
  const priority = ['日本', '中国', '韩国', '美国'].filter(c => all.includes(c))
  const rest = all.filter(c => !priority.includes(c)).sort((a, b) => a.localeCompare(b, 'zh'))
  return [{ label: '全部', value: '' }, ...[...priority, ...rest].map(c => ({ label: c, value: c }))]
})

const typeOptions = computed(() => {
  const all = filterOptions.value?.types ?? []
  return [
    { label: '全部', value: '' },
    ...all.map(t => ({ label: TYPE_LABELS[t] ?? t, value: t })),
  ]
})

const yearChips = computed(() => {
  const allYears = filterOptions.value?.years ?? []
  const individual = allYears.filter(y => y >= 2015).sort((a, b) => b - a)
    .map(y => ({ label: `${y}`, from: y as number | null, to: y as number | null }))
  const ranges = YEAR_RANGES
    .filter(r => allYears.some(y => (r.from === null || y >= r.from) && y <= r.to))
    .map(r => ({ label: r.label, from: r.from as number | null, to: r.to as number | null }))
  return [{ label: '全部年份', from: null, to: null }, ...individual, ...ranges]
})

// ═══════ 数据获取：统一筛选接口（关键词与所有条件可自由组合） ═══════

const { data, pending } = await useAsyncData(
  'anime-filtered-list',
  async () => {
    return fetchFilteredAnime(
      {
        keyword: debouncedSearch.value || undefined,
        genre: activeGenre.value || undefined,
        type: activeType.value || undefined,
        yearFrom: activeYearFrom.value ?? undefined,
        yearTo: activeYearTo.value ?? undefined,
        country: activeCountry.value || undefined,
      },
      currentPage.value,
      PAGE_SIZE,
    )
  },
  { server: true, watch: [debouncedSearch, activeGenre, activeType, activeCountry, activeYearFrom, activeYearTo, currentPage] },
)

// ═══════ 分页状态同步 ═══════

const pageResponse = computed<AnimeListResponse>(() => {
  return (data.value as AnimeListResponse) ?? {
    content: [],
    totalElements: 0,
    totalPages: 0,
    pageNumber: 0,
    pageSize: PAGE_SIZE,
    last: true,
  }
})

const totalPages = computed(() => pageResponse.value.totalPages)
const totalResults = computed(() => pageResponse.value.totalElements)

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i)
  }
  const pages: (number | string)[] = [0]
  if (current > 3) pages.push('...')
  const start = Math.max(1, current - 1)
  const end = Math.min(total - 2, current + 1)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  if (current < total - 4) pages.push('...')
  pages.push(total - 1)
  return pages
})

// ═══════ 任意条件变化时回到第一页 ═══════

watch([debouncedSearch, activeGenre, activeType, activeCountry, activeYearFrom, activeYearTo], () => {
  currentPage.value = 0
})

const activeCategory = computed(() => {
  return categories.find(c => c.genre === activeGenre.value) ?? categories[0]
})

const animeCards = computed(() => {
  const items = pageResponse.value.content ?? []
  return items.map((a: AnimeDto) => ({
    id: a.id,
    title: displayTitle(a),
    cover: a.coverImage,
    rating: communityScore(a),
    tags: mapGenres(a.genres ?? []),
    hasAgent: !!a.hasAgent,
  }))
})

// ═══════ 离开列表时保存滚动位置，由 nuxt.config.ts 的 scrollBehavior 恢复 ═══════

const SCROLL_KEY = 'aniglow_anime_list_scroll_y'

onBeforeRouteLeave(() => {
  sessionStorage.setItem(SCROLL_KEY, String(window.scrollY))
})
</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.filter-row-label {
  flex: 0 0 3rem;
  padding-top: 0.55rem;
  font-size: 0.8rem;
  font-weight: 800;
  color: #8a95a1;
}

.category-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  flex: 0 0 auto;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background: rgba(255, 255, 255, 0.62);
  padding: 0.62rem 0.95rem;
  color: #596473;
  font-size: 0.85rem;
  font-weight: 800;
  box-shadow: 0 10px 28px rgba(31, 68, 47, 0.06);
  transition: all 0.24s ease;
}

.category-chip:hover {
  color: #07873f;
  transform: translateY(-1px);
  box-shadow: 0 16px 34px rgba(0, 170, 68, 0.13);
}

.category-chip-active {
  border-color: rgba(0, 230, 118, 0.45);
  background: linear-gradient(135deg, rgba(0, 230, 118, 0.92), rgba(134, 239, 172, 0.82));
  color: #ffffff;
  box-shadow: 0 16px 38px rgba(0, 230, 118, 0.26);
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background: rgba(255, 255, 255, 0.62);
  padding: 0.55rem 0.9rem;
  color: #596473;
  font-size: 0.82rem;
  font-weight: 700;
  box-shadow: 0 10px 28px rgba(31, 68, 47, 0.05);
  transition: all 0.24s ease;
}

.filter-chip:hover {
  color: #07873f;
  transform: translateY(-1px);
  box-shadow: 0 16px 34px rgba(0, 170, 68, 0.12);
}

/* 双类名提升优先级：激活的绿色必须压过 .filter-chip 的白底（后者源码顺序在后） */
.filter-chip.category-chip-active {
  border-color: rgba(0, 230, 118, 0.45);
  background: linear-gradient(135deg, rgba(0, 230, 118, 0.92), rgba(134, 239, 172, 0.82));
  color: #ffffff;
  box-shadow: 0 16px 38px rgba(0, 230, 118, 0.26);
}

/* ── 分页按钮 ───────────────────────────────────── */

.pagination-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  color: #596473;
  font-size: 0.875rem;
  font-weight: 800;
  box-shadow: 0 6px 20px rgba(31, 68, 47, 0.05);
  transition: all 0.24s ease;
}

.pagination-btn:hover:not(:disabled) {
  color: #07873f;
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(0, 170, 68, 0.12);
}

.pagination-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.pagination-btn-active {
  border-color: rgba(0, 230, 118, 0.4);
  background: linear-gradient(135deg, rgba(0, 230, 118, 0.88), rgba(134, 239, 172, 0.78));
  color: #ffffff;
  box-shadow: 0 10px 28px rgba(0, 230, 118, 0.22);
}
</style>
