<template>
  <section class="relative overflow-hidden pt-28 pb-10">
    <div class="absolute inset-x-0 top-0 h-[520px] overflow-hidden">
      <img :src="anime.coverImage" :alt="displayTitle(anime)" class="w-full h-full object-cover blur-sm scale-105 opacity-60" />
      <div class="absolute inset-0 bg-gradient-to-b from-cream/35 via-cream/80 to-cream" />
    </div>
    <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <NuxtLink to="/anime" class="inline-flex items-center gap-2 px-4 py-2 mb-6 rounded-full bg-white/70 backdrop-blur-md text-sm font-bold text-gray-600 shadow-soft hover:text-firefly-700 transition-colors">
        <ArrowLeft class="w-4 h-4" />返回番剧
      </NuxtLink>
      <div class="grid lg:grid-cols-[280px,1fr] gap-8 lg:gap-10 items-end">
        <div class="glass-card rounded-[2rem] p-3">
          <img :src="anime.coverImage" :alt="displayTitle(anime)" class="w-full aspect-[3/4] object-cover rounded-[1.45rem] shadow-ambient" />
        </div>
        <div class="glass-card-cream rounded-4xl p-7 sm:p-9">
          <div class="flex flex-wrap items-center gap-2 mb-4">
            <span v-for="tag in mappedGenres" :key="tag.label" class="tag" :class="tagClass(tag.type)">{{ tag.label }}</span>
            <span v-if="anime.year" class="tag tag-new">{{ anime.year }}</span>
            <span v-if="anime.status" class="tag tag-healing">{{ tStatus(anime.status) }}</span>
          </div>
          <h1 class="text-3xl sm:text-5xl font-extrabold text-gray-800 leading-tight mb-3">{{ displayTitle(anime) }}</h1>
          <p v-if="anime.titleEnglish" class="text-sm text-gray-400 font-semibold mb-5">{{ anime.titleEnglish }}</p>
          <p class="text-gray-600 leading-relaxed max-w-3xl line-clamp-3 mb-6">{{ synopsis }}</p>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div v-for="item in stats" :key="item.label" class="rounded-3xl bg-white/60 backdrop-blur-md px-4 py-3">
              <p class="text-xs text-gray-400">{{ item.label }}</p>
              <p class="text-2xl font-extrabold text-gray-800">{{ item.value }}<span v-if="item.suffix" class="text-sm text-gray-400"> {{ item.suffix }}</span></p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ArrowLeft } from 'lucide-vue-next'
import type { AnimeDto } from '~/types/anime'
import { displayTitle, tagClass, tStatus, tType } from '~/composables/useAnimeI18n'

const props = defineProps<{
  anime: AnimeDto
  mappedGenres: { label: string; type: string }[]
  synopsis: string
  scoreText: string
}>()

const stats = computed(() => [
  { label: '用户评分', value: props.scoreText, suffix: '/ 10' },
  { label: '站内共鸣', value: props.anime.communityRatingCount || 0 },
  { label: '集数', value: props.anime.episodes || '?' },
  { label: '类型', value: tType(props.anime.type) || '未知' },
])
</script>
