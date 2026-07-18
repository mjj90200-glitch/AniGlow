<template>
  <div class="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 pt-24 pb-24">
    <div class="flex gap-5">
      <!-- 左侧边栏：社区列表（仅桌面端） -->
      <div class="w-[250px] shrink-0 hidden lg:block">
        <CommunitySidebar />
      </div>

      <!-- 主内容区 -->
      <main class="flex-1 min-w-0">
        <!-- 桌面端：广场介绍 -->
        <div class="hidden lg:flex items-center justify-center h-[calc(100vh-7rem)]">
          <div class="text-center max-w-md">
            <div class="w-20 h-20 rounded-2xl bg-firefly/10 flex items-center justify-center mx-auto mb-5">
              <MessageCircle class="w-10 h-10 text-firefly/50" />
            </div>
            <h2 class="text-xl font-extrabold text-gray-500 mb-2">萤火社区</h2>
            <p class="text-sm text-gray-400 leading-relaxed">番剧、漫画、游戏、Vibe Coding — 四个盒子，四群同好。<br/>从左侧选择一个社区，开始聊吧。</p>
          </div>
        </div>

        <!-- 移动端：社区卡片网格 -->
        <div class="lg:hidden">
          <div class="mb-8">
            <p class="text-xs font-semibold tracking-widest uppercase mb-2" style="color: #00AA44;">
              Community Boxes
            </p>
            <h1 class="text-2xl font-extrabold text-gray-800">社区盒子</h1>
            <p class="mt-2 text-sm text-gray-500">选择一个社区盒子，与同好们一起交流</p>
          </div>

          <!-- 加载中 -->
          <div v-if="pending" class="grid grid-cols-2 gap-4">
            <div
              v-for="n in 4"
              :key="'skel-' + n"
              class="rounded-3xl bg-white/40 backdrop-blur-sm animate-pulse h-48"
            />
          </div>

          <!-- 社区卡片 -->
          <div v-else-if="communities.length" class="grid grid-cols-2 gap-4">
            <NuxtLink
              v-for="(box, index) in communities"
              :key="box.slug"
              :to="`/community/${box.slug}`"
              class="glass-card rounded-3xl p-5 group cursor-pointer
                     transition-all duration-500 hover:-translate-y-2 animate-scale-in"
              :style="{ animationDelay: `${index * 80}ms` }"
            >
              <!-- 封面 -->
              <div class="w-full h-28 rounded-2xl overflow-hidden mb-4 bg-cream-200">
                <img
                  v-if="box.coverImage"
                  :src="box.coverImage"
                  :alt="box.name"
                  class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                  loading="lazy"
                />
                <div v-else class="w-full h-full flex items-center justify-center bg-firefly/5">
                  <MessageCircle class="w-8 h-8 text-firefly/30" />
                </div>
              </div>

              <!-- 信息 -->
              <h3 class="font-extrabold text-gray-800 text-sm mb-1.5 line-clamp-1
                         group-hover:text-firefly-600 transition-colors duration-300">
                {{ box.name }}
              </h3>
              <p class="text-xs text-gray-400 leading-relaxed line-clamp-2 mb-3">
                {{ box.description || '与同好们一起交流讨论' }}
              </p>

              <!-- 统计 -->
              <div class="flex items-center gap-4 text-xs text-gray-400">
                <span class="inline-flex items-center gap-1">
                  <MessageCircle class="w-3.5 h-3.5" />
                  {{ box.postCount || 0 }}
                </span>
                <span class="inline-flex items-center gap-1">
                  <Users class="w-3.5 h-3.5" />
                  {{ box.memberCount || 0 }}
                </span>
              </div>
            </NuxtLink>
          </div>

          <!-- 空状态 -->
          <div v-else class="text-center py-20">
            <MessageCircle class="w-12 h-12 mx-auto text-gray-300 mb-4" />
            <p class="text-gray-400">社区盒子正在搭建中，敬请期待</p>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { MessageCircle, Users } from 'lucide-vue-next'
import { useCommunity } from '~/composables/useCommunity'

const { fetchCommunities } = useCommunity()

const { data: communitiesData, pending } = await useAsyncData(
  'community-index-list',
  () => fetchCommunities(),
  { server: true }
)

const communities = computed(() => communitiesData.value ?? [])

useHead({
  title: '社区盒子 - 萤火社区',
  meta: [
    { name: 'description', content: '进入不同番剧和兴趣社区盒子，发布帖子并与同好交流。' },
  ],
})
</script>
