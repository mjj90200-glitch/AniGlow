<template>
  <div v-if="loading" class="grid grid-cols-2 lg:grid-cols-4 gap-4 lg:gap-5">
    <div v-for="n in 4" :key="n" class="rounded-3xl bg-white/40 backdrop-blur-sm animate-pulse h-48" />
  </div>
  <div v-else class="grid grid-cols-2 lg:grid-cols-4 gap-4 lg:gap-5">
    <NuxtLink v-for="(box, index) in communities" :key="box.slug" :to="`/community/${box.slug}`" class="glass-card rounded-3xl p-5 group transition-all duration-500 hover:-translate-y-2 animate-scale-in" :style="{ animationDelay: `${index * 80}ms` }">
      <div class="w-full h-28 rounded-2xl overflow-hidden mb-4 bg-cream-200">
        <img v-if="box.coverImage" :src="box.coverImage" :alt="box.name" class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" loading="lazy" />
        <div v-else class="w-full h-full flex items-center justify-center bg-firefly/5"><Users class="w-8 h-8 text-firefly/30" /></div>
      </div>
      <h3 class="font-extrabold text-gray-800 text-sm mb-1.5 line-clamp-1 group-hover:text-firefly-600 transition-colors">{{ box.name }}</h3>
      <p class="text-xs text-gray-400 leading-relaxed line-clamp-2 mb-3">{{ box.description }}</p>
      <div class="flex items-center gap-4 text-xs text-gray-400">
        <span v-if="box.slug !== 'all'" class="inline-flex items-center gap-1"><Users class="w-3.5 h-3.5" />{{ box.memberCount }}</span>
      </div>
    </NuxtLink>
  </div>
  <div v-if="!loading && communities.length === 0" class="text-center py-16"><p class="text-gray-400 text-sm">社区盒子正在搭建中，敬请期待</p></div>
</template>

<script setup lang="ts">
import { Users } from 'lucide-vue-next'
defineProps<{ loading: boolean; communities: any[] }>()
</script>
