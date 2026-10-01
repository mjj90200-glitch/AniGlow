<template>
  <NuxtLink
    :to="`/community/${communitySlug}/posts/${post.id}`"
    class="block group"
  >
    <article class="bg-white rounded-xl sm:rounded-2xl p-4 sm:p-5
                    border border-gray-100/80
                    hover:border-gray-200/80 hover:shadow-md
                    transition-all duration-200">
      <!-- 头部：头像 + 昵称 + 时间 -->
      <div class="flex items-center gap-3 mb-3">
        <div class="w-9 h-9 sm:w-10 sm:h-10 rounded-full overflow-hidden bg-firefly/10 flex items-center justify-center shrink-0">
          <img
            v-if="post.avatarUrl"
            :src="post.avatarUrl"
            :alt="authorName"
            class="w-full h-full object-cover"
          />
          <span v-else class="text-sm font-extrabold text-firefly-700">{{ authorName.slice(0, 1) }}</span>
        </div>
        <div class="min-w-0 flex-1">
          <p class="text-sm font-bold text-gray-800 truncate">
            {{ authorName }}
            <span v-if="showCommunity" class="ml-2 text-[11px] font-semibold text-firefly-700">{{ post.communityName }}</span>
          </p>
        </div>
        <div class="flex items-center gap-2 shrink-0">
          <span v-if="post.pinned" class="rounded-full bg-sakura/10 px-1.5 py-0.5 text-[10px] font-extrabold text-sakura-dark">置顶</span>
          <span class="text-xs text-gray-400">{{ formatTime(post.createdAt) }}</span>
        </div>
      </div>

      <!-- 标题 -->
      <h3 class="text-[15px] sm:text-base font-bold text-gray-900 leading-snug mb-1.5
                 line-clamp-2 group-hover:text-firefly-700 transition-colors">
        {{ post.title }}
      </h3>

      <!-- 正文预览 -->
      <p class="text-[13px] sm:text-sm text-gray-500 leading-relaxed line-clamp-3 mb-3">
        {{ post.content }}
      </p>

      <!-- 保留原始宽高比：不设固定高度、不裁切图片 -->
      <div v-if="imageUrls.length" class="mb-3 overflow-hidden">
        <div v-if="imageUrls.length === 1" class="max-w-[82%] sm:max-w-[62%]">
          <img
            :src="imageUrls[0]"
            class="block w-full h-auto rounded-xl bg-gray-50"
            loading="lazy"
            :alt="`${post.title} 配图 1`"
          />
        </div>
        <div v-else class="columns-2 sm:columns-3 gap-2 max-w-full sm:max-w-[86%]">
          <div
            v-for="(url, i) in displayedImages"
            :key="i"
            class="mb-2 break-inside-avoid overflow-hidden rounded-xl bg-gray-50"
          >
            <img
              :src="url"
              class="block w-full h-auto"
              loading="lazy"
              :alt="`${post.title} 配图 ${i + 1}`"
            />
          </div>
        </div>
      </div>

      <!-- 底部互动栏 -->
      <div class="flex items-center gap-5 text-xs text-gray-400">
        <span class="inline-flex items-center gap-1.5">
          <Heart class="w-3.5 h-3.5" />
          {{ post.likeCount || 0 }}人点亮
        </span>
        <span class="inline-flex items-center gap-1.5">
          <MessageCircle class="w-3.5 h-3.5" />
          {{ post.replyCount || 0 }}条回复
        </span>
        <span class="inline-flex items-center gap-1.5 ml-auto">
          <Eye class="w-3.5 h-3.5" />
          {{ post.viewCount || 0 }}
        </span>
      </div>
    </article>
  </NuxtLink>
</template>

<script setup lang="ts">
import { Heart, MessageCircle, Eye } from 'lucide-vue-next'
import type { CommunityPostDto } from '~/composables/useCommunity'

const props = defineProps<{
  post: CommunityPostDto
  communitySlug: string
  showCommunity?: boolean
}>()

const imageUrls = computed(() => props.post.images ?? [])

const displayedImages = computed(() => imageUrls.value.slice(0, 9))

const authorName = computed(() => {
  const displayName = (props.post.displayName || '').trim()
  if (displayName && !/^1\d{10}$/.test(displayName)) return displayName
  if (props.post.username && !/^1\d{10}$/.test(props.post.username)) return props.post.username
  return '番舍同好'
})

function formatTime(value?: string): string {
  if (!value) return '刚刚'
  const diff = Date.now() - new Date(value).getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return new Date(value).toLocaleDateString('zh-CN')
}
</script>
