<template>
  <aside class="glass-card rounded-4xl flex flex-col h-full overflow-hidden">
    <!-- 头部 -->
    <div class="shrink-0 px-5 py-4 border-b border-white/40">
      <h2 class="text-lg font-black text-gray-900 flex items-center gap-2">
        <LayoutGrid class="w-5 h-5 text-firefly" />
        社区盒子
      </h2>
      <p class="text-xs text-gray-400 font-bold mt-1">{{ totalCommunities }} 个固定入口</p>
    </div>

    <!-- 社区列表 - 独立滚动 -->
    <div class="flex-1 overflow-y-auto px-3 py-3 space-y-4">
      <div v-for="group in groupedCommunities" :key="group.category">
        <p class="px-3 mb-2 text-[11px] font-extrabold tracking-[0.2em] uppercase text-firefly-700/60">
          {{ group.category }}
        </p>
        <ul class="space-y-1">
          <li v-for="community in group.items" :key="community.slug">
            <NuxtLink
              :to="`/community/${community.slug}`"
              class="flex items-center gap-3 px-3 py-2.5 rounded-2xl transition-all duration-200 group"
              :class="community.slug === selectedSlug
                ? 'bg-firefly/12 text-firefly-700 font-bold shadow-sm'
                : 'text-gray-600 hover:bg-white/50'"
            >
              <div class="w-9 h-9 rounded-xl overflow-hidden shrink-0 bg-firefly/10">
                <img
                  v-if="community.coverImage"
                  :src="community.coverImage"
                  :alt="community.name"
                  class="w-full h-full object-cover"
                />
                <span v-else class="w-full h-full flex items-center justify-center text-xs font-black text-firefly-700/50">
                  {{ community.name.slice(0, 1) }}
                </span>
              </div>
              <div class="min-w-0 flex-1">
                <p
                  class="text-sm truncate"
                  :class="community.slug === selectedSlug ? 'font-black text-firefly-700' : 'font-bold'"
                >
                  {{ community.name }}
                </p>
                <p class="text-[11px] text-gray-400 font-bold mt-0.5">
                  {{ community.slug === 'all' ? '汇总全部社区' : `${community.memberCount || 0} 同好` }}
                </p>
              </div>
              <span
                v-if="community.featured"
                class="shrink-0 rounded-full bg-sakura/15 px-1.5 py-0.5 text-[9px] font-black text-sakura-dark"
              >
                荐
              </span>
            </NuxtLink>
          </li>
        </ul>
      </div>

      <div v-if="!groupedCommunities.length" class="text-center py-8">
        <p class="text-sm text-gray-400 font-bold">暂无社区盒子</p>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { LayoutGrid } from 'lucide-vue-next'
import type { CommunityDto } from '~/composables/useCommunity'

defineProps<{
  selectedSlug?: string
}>()

const { fetchCommunities } = useCommunity()

const { data } = await useAsyncData('community-sidebar-list', fetchCommunities, { server: true })
const communities = computed(() => data.value ?? [])

const groupedCommunities = computed(() => {
  const groups = new Map<string, CommunityDto[]>()
  for (const community of communities.value) {
    const key = community.category || '同好盒子'
    groups.set(key, [...(groups.get(key) || []), community])
  }
  return Array.from(groups.entries()).map(([category, items]) => ({ category, items }))
})

const totalCommunities = computed(() => communities.value.length)
</script>
