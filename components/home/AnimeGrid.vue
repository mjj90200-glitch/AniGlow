<template>
  <div v-if="loading" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5 lg:gap-6">
    <div v-for="n in 8" :key="n" class="rounded-3xl bg-white/40 backdrop-blur-sm animate-pulse">
      <div class="aspect-[3/4] bg-cream-200 rounded-t-3xl" />
      <div class="p-4 space-y-2"><div class="h-4 bg-cream-200 rounded-full w-3/4" /><div class="h-5 bg-cream-200 rounded-full w-14" /></div>
    </div>
  </div>
  <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5 lg:gap-6">
    <article v-for="(anime, index) in animeList" :key="anime.id" class="anime-card animate-scale-in group" :style="{ animationDelay: `${index * 80}ms` }">
      <NuxtLink :to="`/anime/${anime.id}`" class="block">
        <div class="relative aspect-[3/4] overflow-hidden rounded-t-3xl bg-cream-200">
          <img :src="anime.cover" :alt="anime.title" class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" loading="lazy" @error="hideBrokenImage" />
          <div class="absolute top-3 right-3 z-10"><div class="rating-badge text-xs px-2.5 py-1"><Star class="w-3 h-3 fill-[#5D4037]" />{{ anime.rating }}</div></div>
          <div v-if="anime.hasAgent" class="absolute top-3 left-3 z-10 flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold text-firefly-700 bg-white/80 backdrop-blur-md border border-white/60"><span class="firefly-dot" />AIGC</div>
        </div>
        <div class="p-4">
          <h3 class="font-extrabold text-gray-800 text-sm mb-2.5 line-clamp-1 group-hover:text-firefly-600 transition-colors">{{ anime.title }}</h3>
          <div class="flex flex-wrap gap-1.5"><span v-for="tag in anime.tags" :key="tag.label" class="tag" :class="tagClass(tag.type)">{{ tag.label }}</span></div>
        </div>
      </NuxtLink>
    </article>
  </div>
</template>

<script setup lang="ts">
import { Star } from 'lucide-vue-next'
import { tagClass } from '~/composables/useAnimeI18n'
defineProps<{ loading: boolean; animeList: any[] }>()
const hideBrokenImage = (event: Event) => { (event.target as HTMLImageElement).style.display = 'none' }
</script>
