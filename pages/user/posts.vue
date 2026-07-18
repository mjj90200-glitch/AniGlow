<template>
  <div class="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-24">
    <div class="flex gap-5">
      <!-- 左侧导航 -->
      <div class="w-[220px] shrink-0 hidden lg:block">
        <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
          <div class="px-5 py-4 border-b border-gray-100">
            <p class="text-xs font-bold tracking-widest uppercase text-gray-400">用户中心</p>
          </div>
          <NuxtLink
            to="/user/posts"
            class="flex items-center gap-3 px-5 py-3.5 text-sm font-bold text-[#00C853] bg-[#00C853]/5 border-l-[3px] border-[#00C853] transition"
          >
            <FileText class="w-4 h-4" />
            我的帖子
          </NuxtLink>
        </div>
      </div>

      <!-- 右侧内容 -->
      <main class="flex-1 min-w-0">
        <!-- 页面标题 -->
        <div class="mb-6">
          <h1 class="text-2xl font-extrabold text-gray-900 mb-1">我的帖子</h1>
          <p class="text-sm text-gray-500">管理你发布的所有帖子，查看回复提醒</p>
        </div>

        <!-- 未登录 -->
        <div v-if="!userStore.isLoggedIn" class="bg-white rounded-xl border border-gray-100 p-12 text-center">
          <MessageCircle class="w-12 h-12 mx-auto text-gray-300 mb-4" />
          <h2 class="text-lg font-extrabold text-gray-400 mb-2">需要登录</h2>
          <p class="text-sm text-gray-400 mb-5">登录后查看你发布的帖子</p>
        </div>

        <!-- 帖子列表 -->
        <template v-else>
          <div v-if="!posts.length && !loading" class="bg-white rounded-xl border border-gray-100 p-12 text-center">
            <FileText class="w-12 h-12 mx-auto text-gray-300 mb-4" />
            <h2 class="text-lg font-extrabold text-gray-400 mb-2">还没有帖子</h2>
            <p class="text-sm text-gray-400 mb-5">去社区发一篇帖子吧</p>
            <NuxtLink to="/community" class="btn-glow inline-flex px-5 py-2.5 text-sm">进入社区</NuxtLink>
          </div>

          <div v-else class="bg-white rounded-xl border border-gray-100 overflow-hidden">
            <article
              v-for="(post, idx) in posts"
              :key="post.id"
              class="relative"
              :class="idx < posts.length - 1 ? 'border-b border-gray-100' : ''"
            >
              <div class="flex items-center gap-4 px-5 py-4">
                <!-- 左侧信息 -->
                <NuxtLink :to="`/community/${post.communitySlug}/posts/${post.id}`" class="flex-1 min-w-0 group">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="text-xs text-gray-400 bg-gray-100 rounded-full px-2 py-0.5">{{ post.communityName }}</span>
                  </div>
                  <h3 class="text-[15px] font-bold text-gray-900 leading-snug line-clamp-1 group-hover:text-[#00C853] transition mb-1.5">
                    {{ post.title }}
                  </h3>
                  <div class="flex items-center gap-4 text-[11px] text-gray-400">
                    <span class="inline-flex items-center gap-1"><Heart class="w-3 h-3" />{{ post.likeCount || 0 }}</span>
                    <span>{{ formatTime(post.createdAt) }}</span>
                  </div>
                </NuxtLink>

                <!-- 回复红点 -->
                <div class="flex items-center gap-3 shrink-0">
                  <div class="flex items-center gap-1.5" :class="post.replyCount > 0 ? 'bg-sakura/10 rounded-full px-3 py-1.5' : ''">
                    <MessageCircle class="w-4 h-4" :class="post.replyCount > 0 ? 'text-sakura' : 'text-gray-300'" />
                    <span class="text-sm font-extrabold" :class="post.replyCount > 0 ? 'text-sakura' : 'text-gray-400'">
                      {{ post.replyCount || 0 }}
                    </span>
                  </div>

                  <!-- 删除按钮 -->
                  <button
                    class="rounded-full bg-white border border-gray-200 p-2 text-gray-400 hover:text-sakura hover:border-sakura/30 hover:bg-sakura/5 transition"
                    @click.stop="openDeleteConfirm(post)"
                    title="删除帖子"
                  >
                    <Trash2 class="w-4 h-4" />
                  </button>
                </div>
              </div>
            </article>
          </div>

          <!-- 加载更多 -->
          <button
            v-if="hasMore"
            class="w-full mt-3 py-3 text-[13px] font-semibold text-gray-500 hover:text-[#00C853] hover:bg-white rounded-xl border border-gray-100 transition bg-white"
            :class="loadingMore ? 'opacity-50 pointer-events-none' : ''"
            @click="loadMore"
          >
            {{ loadingMore ? '加载中...' : `加载更多（还剩 ${remainingPosts} 条）` }}
          </button>
        </template>
      </main>
    </div>

    <!-- 删除确认弹窗 -->
    <Transition enter-active-class="transition duration-200" enter-from-class="opacity-0" leave-active-class="transition duration-150" leave-to-class="opacity-0">
      <div v-if="deleteTarget" class="fixed inset-0 z-[100] flex items-center justify-center px-4 bg-black/20 backdrop-blur-md" @click.self="deleteTarget = null">
        <div class="bg-white rounded-3xl p-6 max-w-sm w-full text-center shadow-xl">
          <div class="w-12 h-12 rounded-full bg-sakura/10 flex items-center justify-center mx-auto mb-4">
            <Trash2 class="w-6 h-6 text-sakura" />
          </div>
          <p class="text-lg font-extrabold text-gray-900 mb-2">确认删除</p>
          <p class="text-sm text-gray-500 mb-5">帖子 "<strong>{{ deleteTarget.title }}</strong>" 将无法恢复</p>
          <div class="flex justify-center gap-3">
            <button class="btn-glass px-5 py-2.5 text-sm" @click="deleteTarget = null">取消</button>
            <button
              class="bg-sakura text-white px-5 py-2.5 rounded-full font-bold text-sm transition hover:bg-sakura-dark"
              :class="deleting ? 'opacity-50 pointer-events-none' : ''"
              @click="confirmDelete"
            >
              {{ deleting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { FileText, Heart, MessageCircle, Trash2 } from 'lucide-vue-next'
import type { CommunityPostDto } from '~/composables/useCommunity'

const userStore = useUserStore()
const { fetchMyPosts, deletePost } = useCommunity()
const { requireAuth } = useAuthModal()

const posts = ref<CommunityPostDto[]>([])
const currentPage = ref(0)
const totalPosts = ref(0)
const loading = ref(true)
const loadingMore = ref(false)
const pageSize = 20

const deleteTarget = ref<CommunityPostDto | null>(null)
const deleting = ref(false)

const hasMore = computed(() => posts.value.length < totalPosts.value)
const remainingPosts = computed(() => totalPosts.value - posts.value.length)

async function loadPosts() {
  loading.value = true
  currentPage.value = 0
  const result = await fetchMyPosts(0, pageSize)
  posts.value = result.items
  totalPosts.value = result.total
  loading.value = false
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const nextPage = currentPage.value + 1
    const result = await fetchMyPosts(nextPage, pageSize)
    posts.value = [...posts.value, ...result.items]
    totalPosts.value = result.total
    currentPage.value = nextPage
  } finally {
    loadingMore.value = false
  }
}

function openDeleteConfirm(post: CommunityPostDto) {
  deleteTarget.value = post
}

async function confirmDelete() {
  if (!deleteTarget.value || deleting.value) return
  deleting.value = true
  try {
    await deletePost(deleteTarget.value.id)
    posts.value = posts.value.filter(p => p.id !== deleteTarget.value!.id)
    totalPosts.value = Math.max(0, totalPosts.value - 1)
    deleteTarget.value = null
  } catch (e: any) {
    console.warn('删除失败:', e?.message)
    deleteTarget.value = null
  } finally {
    deleting.value = false
  }
}

function formatTime(value?: string) {
  if (!value) return ''
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

onMounted(() => {
  if (userStore.isLoggedIn) {
    loadPosts()
  } else {
    loading.value = false
  }
})

useHead({
  title: '我的帖子 - 萤火社区',
})
</script>
