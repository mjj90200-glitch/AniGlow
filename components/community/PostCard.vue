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
          <p class="text-sm font-bold text-gray-800 truncate">{{ authorName }}</p>
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

      <!-- 自适应图片网格 -->
      <div v-if="imageUrls.length" class="mb-3 overflow-hidden">
        <!-- 1张图：限制宽度 -->
        <div v-if="imageUrls.length === 1" class="max-w-[72%] sm:max-w-[55%]">
          <div class="rounded-lg overflow-hidden bg-gray-100">
            <img
              :src="imageUrls[0]"
              class="w-full h-auto max-h-[200px] sm:max-h-[300px] object-cover"
              loading="lazy"
              alt=""
            />
          </div>
        </div>

        <!-- 2张图：并排 -->
        <div v-else-if="imageUrls.length === 2" class="grid grid-cols-2 gap-[3px] h-[150px] sm:h-[230px]">
          <div class="overflow-hidden rounded-l-lg bg-gray-100">
            <img :src="imageUrls[0]" class="w-full h-full object-cover" loading="lazy" alt="" />
          </div>
          <div class="overflow-hidden rounded-r-lg bg-gray-100">
            <img :src="imageUrls[1]" class="w-full h-full object-cover" loading="lazy" alt="" />
          </div>
        </div>

        <!-- 3张图：左大 + 右二 -->
        <div v-else-if="imageUrls.length === 3" class="flex gap-[3px] h-[180px] sm:h-[260px]">
          <div class="flex-[55%] rounded-l-lg overflow-hidden bg-gray-100">
            <img :src="imageUrls[0]" class="w-full h-full object-cover" loading="lazy" alt="" />
          </div>
          <div class="flex-[45%] flex flex-col gap-[3px]">
            <div class="flex-1 overflow-hidden bg-gray-100">
              <img :src="imageUrls[1]" class="w-full h-full object-cover" loading="lazy" alt="" />
            </div>
            <div class="flex-1 rounded-br-lg overflow-hidden bg-gray-100">
              <img :src="imageUrls[2]" class="w-full h-full object-cover" loading="lazy" alt="" />
            </div>
          </div>
        </div>

        <!-- 4张图：2×2 网格 -->
        <div v-else-if="imageUrls.length === 4" class="grid grid-cols-2 gap-[3px] h-[200px] sm:h-[280px]">
          <div v-for="(url, i) in imageUrls" :key="i" class="overflow-hidden bg-gray-100"
               :class="{
                 'rounded-tl-lg': i === 0,
                 'rounded-tr-lg': i === 1,
                 'rounded-bl-lg': i === 2,
                 'rounded-br-lg': i === 3,
               }">
            <img :src="url" class="w-full h-full object-cover" loading="lazy" alt="" />
          </div>
        </div>

        <!-- 5-9张图：3列网格 -->
        <div v-else class="grid grid-cols-3 gap-[3px] max-w-[90%] sm:max-w-[80%]">
          <div
            v-for="(url, i) in displayedImages"
            :key="i"
            class="aspect-square overflow-hidden bg-gray-100 relative"
            :class="gridCornerClass(i, displayedImages.length)"
          >
            <img :src="url" class="w-full h-full object-cover" loading="lazy" alt="" />
            <div
              v-if="i === 8 && imageUrls.length > 9"
              class="absolute inset-0 bg-black/50 flex items-center justify-center text-white text-sm sm:text-base font-extrabold"
            >
              +{{ imageUrls.length - 9 }}
            </div>
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
}>()

const imageUrls = computed(() => props.post.images ?? [])

const displayedImages = computed(() => imageUrls.value.slice(0, 9))

const authorName = computed(() => {
  const displayName = (props.post.displayName || '').trim()
  if (displayName && !/^1\d{10}$/.test(displayName)) return displayName
  if (props.post.username && !/^1\d{10}$/.test(props.post.username)) return props.post.username
  return '番舍同好'
})

function gridCornerClass(index: number, total: number): string {
  const classes: string[] = []
  const cols = 3
  const rows = Math.ceil(total / cols)
  const row = Math.floor(index / cols)
  const col = index % cols

  if (row === 0 && col === 0) classes.push('rounded-tl-lg')
  if (row === 0 && col === cols - 1) classes.push('rounded-tr-lg')
  // last col of last row (might not be full row)
  if (row === rows - 1 && col === 0 && total % cols === 1) {
    // single item in last row: both bottom corners
    classes.push('rounded-bl-lg', 'rounded-br-lg')
  } else if (row === rows - 1 && col === 0) {
    classes.push('rounded-bl-lg')
  }
  if (row === rows - 1 && col === (total - 1) % cols) {
    classes.push('rounded-br-lg')
  }

  return classes.join(' ')
}

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
