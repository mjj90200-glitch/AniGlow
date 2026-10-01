<template>
  <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-24">
    <div v-if="!post" class="glass-card-cream rounded-4xl p-10 text-center">
      <h1 class="text-2xl font-black text-gray-800 mb-3">帖子好像迷路了</h1>
      <NuxtLink :to="`/community/${slug}`" class="btn-glow inline-flex px-6 py-3">回到社区</NuxtLink>
    </div>

    <template v-else>
      <NuxtLink
        :to="`/community/${post.communitySlug}`"
        class="inline-flex items-center gap-2 rounded-full bg-white/70 px-4 py-2 text-sm font-black text-gray-600 backdrop-blur-md mb-5"
      >
        <ArrowLeft class="w-4 h-4" />
        回到 {{ post.communityName }}
      </NuxtLink>

      <article class="glass-card-cream rounded-4xl overflow-hidden mb-6 relative">
        <div v-if="post.coverImage" class="relative h-64 sm:h-80 overflow-hidden">
          <img :src="post.coverImage" :alt="post.title" class="w-full h-full object-cover" />
          <div class="absolute inset-0 bg-gradient-to-t from-black/65 via-black/10 to-transparent" />
          <div class="absolute left-6 right-6 bottom-6">
            <span class="rounded-full bg-firefly/95 px-3 py-1 text-xs font-black text-white">{{ post.communityName }}</span>
          </div>
        </div>

        <div class="p-6 sm:p-8">
          <div class="flex items-center gap-3 mb-5">
            <div class="w-11 h-11 rounded-full overflow-hidden bg-firefly/15 flex items-center justify-center text-sm font-black text-firefly-700">
              <img v-if="post.avatarUrl" :src="post.avatarUrl" :alt="authorName(post)" class="w-full h-full object-cover" />
              <span v-else>{{ authorName(post).slice(0, 1) }}</span>
            </div>
            <div>
              <p class="text-sm font-black text-gray-800">{{ authorName(post) }}</p>
              <p class="text-xs text-gray-400">{{ formatTime(post.createdAt) }} 发布</p>
            </div>
          </div>

          <h1 class="text-3xl sm:text-4xl font-black text-gray-900 leading-tight mb-5">{{ post.title }}</h1>
          <p class="whitespace-pre-wrap text-gray-600 leading-8">{{ post.content }}</p>

          <!-- 图片展示：全宽纵向排列 -->
          <div v-if="post.images && post.images.length" class="mt-6 space-y-3">
            <div
              v-for="(imgUrl, imgIdx) in post.images"
              :key="imgIdx"
              class="overflow-hidden rounded-3xl cursor-pointer"
              @click="openLightbox(imgIdx)"
            >
              <img
                :src="imgUrl"
                :alt="`图片 ${imgIdx + 1}`"
                class="w-full h-auto max-h-[70vh] object-contain bg-black/5 hover:scale-[1.02] transition duration-500"
                loading="lazy"
              />
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-3 mt-8">
            <button class="rounded-full bg-white/70 px-4 py-2 text-sm font-black text-gray-500 transition hover:text-sakura-dark hover:bg-sakura/15" @click="handleLikePost">
              <span class="inline-flex items-center gap-2">
                <Heart class="w-4 h-4" :class="post.likedByMe ? 'fill-sakura text-sakura' : ''" />
                {{ post.likeCount || 0 }} 人点亮
              </span>
            </button>
            <span class="rounded-full bg-white/70 px-4 py-2 text-sm font-black text-gray-400 inline-flex items-center gap-2">
              <MessageCircle class="w-4 h-4" />
              {{ post.replyCount || 0 }} 条回复
            </span>
            <span class="rounded-full bg-white/70 px-4 py-2 text-sm font-black text-gray-400 inline-flex items-center gap-2">
              <Eye class="w-4 h-4" />
              {{ post.viewCount || 0 }} 次浏览
            </span>

            <button
              v-if="canDeletePost"
              class="rounded-full bg-white/70 px-3 py-2 text-sm font-black text-gray-400 transition hover:text-sakura-dark hover:bg-sakura/15 ml-auto"
              @click="showPostDeleteConfirm = true"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
        </div>
      </article>

      <!-- Lightbox 灯箱 -->
      <Teleport to="body">
        <Transition enter-active-class="transition duration-200" enter-from-class="opacity-0" leave-active-class="transition duration-150" leave-to-class="opacity-0">
          <div
            v-if="lightboxOpen && post?.images"
            class="fixed inset-0 z-[200] bg-black/95 flex items-center justify-center"
            @click.self="closeLightbox"
            @keydown="onLightboxKeydown"
            tabindex="0"
          >
            <button class="absolute top-4 right-4 text-white/70 hover:text-white text-2xl z-10" @click="closeLightbox">&times;</button>
            <button
              v-if="post.images.length > 1"
              class="absolute left-4 top-1/2 -translate-y-1/2 text-white/70 hover:text-white text-3xl z-10"
              @click.stop="prevImage"
            >&lsaquo;</button>
            <button
              v-if="post.images.length > 1"
              class="absolute right-4 top-1/2 -translate-y-1/2 text-white/70 hover:text-white text-3xl z-10"
              @click.stop="nextImage"
            >&rsaquo;</button>
            <img
              :src="post.images[lightboxIndex]"
              class="max-w-full max-h-[90vh] object-contain select-none"
              :alt="`图片 ${lightboxIndex + 1}`"
              @click.stop
            />
            <div v-if="post.images.length > 1" class="absolute bottom-4 text-white/50 text-sm">
              {{ lightboxIndex + 1 }} / {{ post.images.length }}
            </div>
          </div>
        </Transition>
      </Teleport>

      <section class="glass-card rounded-4xl p-5 sm:p-7">
        <div class="flex items-center justify-between gap-4 mb-5">
          <div>
            <p class="text-xs font-black tracking-[0.25em] uppercase text-firefly-700">Replies</p>
            <h2 class="text-2xl font-black text-gray-900">同好交流</h2>
          </div>
          <span class="text-xs font-bold text-gray-400">认真聊，慢慢沉淀</span>
        </div>

        <div class="rounded-3xl bg-white/55 backdrop-blur-md p-4 mb-6">
          <textarea
            v-model="draftReply"
            rows="4"
            class="w-full resize-none rounded-3xl border border-white/70 bg-white/75 px-4 py-3 text-sm leading-7 outline-none focus:border-firefly focus:ring-4 focus:ring-firefly/10"
            placeholder="回复这条帖子，分享你的理解、补充或提问..."
          />
          <p v-if="replyError" class="mt-2 text-sm font-bold text-sakura-dark">{{ replyError }}</p>
          <div class="flex justify-end mt-3">
            <button
              class="btn-glow px-5 py-2.5"
              :class="!draftReply.trim() || replying ? 'opacity-40 pointer-events-none' : ''"
              @click="submitReply"
            >
              {{ replying ? '回复中...' : '发布回复' }}
            </button>
          </div>
        </div>

        <div class="space-y-3">
          <article
            v-for="reply in replies"
            :key="reply.id"
            class="rounded-3xl bg-white/60 border border-white/60 p-4 relative"
          >
            <div class="flex items-start gap-3">
              <div class="w-10 h-10 rounded-full overflow-hidden bg-firefly/15 flex items-center justify-center shrink-0 text-sm font-black text-firefly-700">
                <img v-if="reply.avatarUrl" :src="reply.avatarUrl" :alt="authorName(reply)" class="w-full h-full object-cover" />
                <span v-else>{{ authorName(reply).slice(0, 1) }}</span>
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2 mb-1">
                  <span class="text-sm font-black text-gray-800">{{ authorName(reply) }}</span>
                  <span class="text-xs text-gray-400">{{ formatTime(reply.createdAt) }}</span>
                </div>
                <p class="text-sm text-gray-600 leading-7 whitespace-pre-wrap">{{ reply.content }}</p>
                <button
                  class="mt-3 inline-flex items-center gap-1.5 rounded-full bg-white/70 px-3 py-1.5 text-xs font-bold text-gray-500 transition hover:text-sakura-dark hover:bg-sakura/15"
                  @click="handleLikeReply(reply)"
                >
                  <Heart class="w-3.5 h-3.5" :class="reply.likedByMe ? 'fill-sakura text-sakura' : ''" />
                  {{ reply.likeCount || 0 }} 人点亮
                </button>
              </div>
            </div>

            <button
              v-if="canDeleteReply(reply)"
              class="absolute top-3 right-3 rounded-full bg-white/70 p-1.5 text-gray-400 hover:text-sakura-dark transition"
              @click.stop="openDeleteReplyConfirm(reply)"
            >
              <MoreHorizontal class="w-3.5 h-3.5" />
            </button>
          </article>

          <div v-if="!replies.length" class="rounded-3xl bg-white/55 p-8 text-center">
            <MessageCircle class="w-10 h-10 mx-auto text-firefly mb-3" />
            <p class="text-sm font-bold text-gray-500">还没有回复，等你把第一束萤火放进来。</p>
          </div>
        </div>
      </section>

      <!-- 删除帖子确认弹窗 -->
      <Transition enter-active-class="transition duration-200" enter-from-class="opacity-0" leave-active-class="transition duration-150" leave-to-class="opacity-0">
        <div v-if="showPostDeleteConfirm" class="fixed inset-0 z-[100] flex items-center justify-center px-4 bg-black/20 backdrop-blur-md" @click.self="showPostDeleteConfirm = false">
          <div class="glass-card-cream rounded-4xl p-6 max-w-sm w-full text-center">
            <p class="text-lg font-black text-gray-900 mb-2">确认删除帖子</p>
            <p class="text-sm text-gray-500 mb-5">删除后无法恢复，确定要删除这个帖子吗？</p>
            <div class="flex justify-center gap-3">
              <button class="btn-glass px-5 py-2.5" @click="showPostDeleteConfirm = false">取消</button>
              <button
                class="bg-sakura text-white px-5 py-2.5 rounded-full font-bold text-sm transition hover:bg-sakura-dark"
                :class="deletingPost ? 'opacity-50 pointer-events-none' : ''"
                @click="confirmDeletePost"
              >
                {{ deletingPost ? '删除中...' : '确认删除' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 删除回复确认弹窗 -->
      <Transition enter-active-class="transition duration-200" enter-from-class="opacity-0" leave-active-class="transition duration-150" leave-to-class="opacity-0">
        <div v-if="deleteReplyTarget" class="fixed inset-0 z-[100] flex items-center justify-center px-4 bg-black/20 backdrop-blur-md" @click.self="deleteReplyTarget = null">
          <div class="glass-card-cream rounded-4xl p-6 max-w-sm w-full text-center">
            <p class="text-lg font-black text-gray-900 mb-2">确认删除回复</p>
            <p class="text-sm text-gray-500 mb-5">删除后无法恢复，确定要删除这条回复吗？</p>
            <div class="flex justify-center gap-3">
              <button class="btn-glass px-5 py-2.5" @click="deleteReplyTarget = null">取消</button>
              <button
                class="bg-sakura text-white px-5 py-2.5 rounded-full font-bold text-sm transition hover:bg-sakura-dark"
                :class="deletingReply ? 'opacity-50 pointer-events-none' : ''"
                @click="confirmDeleteReply"
              >
                {{ deletingReply ? '删除中...' : '确认删除' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Eye, Heart, MessageCircle, MoreHorizontal, Trash2 } from 'lucide-vue-next'
import type { CommunityPostDto, CommunityReplyDto } from '~/composables/useCommunity'

const route = useRoute()
const slug = computed(() => String(route.params.slug || ''))
const postId = computed(() => Number(route.params.id))
const { fetchPost, likePost, fetchReplies, createReply, likeReply, deletePost, deleteReply } = useCommunity()
const { requireAuth } = useAuthModal()
const userStore = useUserStore()

const draftReply = ref('')
const replyError = ref('')
const replying = ref(false)

const showPostDeleteConfirm = ref(false)
const deletingPost = ref(false)
const deleteReplyTarget = ref<CommunityReplyDto | null>(null)
const deletingReply = ref(false)
const lightboxOpen = ref(false)
const lightboxIndex = ref(0)

const { data: postData } = await useAsyncData(
  () => `community-post-${postId.value}`,
  () => fetchPost(postId.value),
  { server: true, watch: [postId] }
)
const { data: repliesData } = await useAsyncData(
  () => `community-post-replies-${postId.value}`,
  () => fetchReplies(postId.value),
  { server: true, watch: [postId] }
)

const post = computed(() => postData.value)
const replies = computed(() => repliesData.value ?? [])

const canDeletePost = computed(() => {
  if (!post.value) return false
  const uid = userStore.backendUserId
  if (!uid) return false
  return post.value.userId === uid
})

function canDeleteReply(reply: CommunityReplyDto) {
  const uid = userStore.backendUserId
  if (!uid) return false
  return reply.userId === uid || post.value?.userId === uid
}

async function handleLikePost() {
  requireAuth(async () => {
    if (!post.value) return
    try {
      postData.value = await likePost(post.value.id)
    } catch {
      // Keep the UI calm
    }
  })
}

async function submitReply() {
  requireAuth(async () => {
    if (!post.value || !draftReply.value.trim() || replying.value) return
    replying.value = true
    replyError.value = ''
    try {
      const created = await createReply(post.value.id, draftReply.value.trim())
      draftReply.value = ''
      repliesData.value = [...replies.value, created]
      postData.value = { ...post.value, replyCount: (post.value.replyCount || 0) + 1 }
    } catch (e: any) {
      replyError.value = e?.message || '回复没有送达，请稍后再试'
    } finally {
      replying.value = false
    }
  })
}

async function handleLikeReply(reply: CommunityReplyDto) {
  requireAuth(async () => {
    const liked = await likeReply(reply.id)
    const next = replies.value.map(item => item.id === liked.id ? liked : item)
    repliesData.value = next
  })
}

async function confirmDeletePost() {
  if (!post.value || deletingPost.value) return
  deletingPost.value = true
  try {
    await deletePost(post.value.id)
    showPostDeleteConfirm.value = false
    await navigateTo(`/community/${post.value.communitySlug}`)
  } catch (e: any) {
    console.warn('删除帖子失败:', e?.message)
    showPostDeleteConfirm.value = false
  } finally {
    deletingPost.value = false
  }
}

function openDeleteReplyConfirm(reply: CommunityReplyDto) {
  deleteReplyTarget.value = reply
}

async function confirmDeleteReply() {
  if (!deleteReplyTarget.value || deletingReply.value) return
  deletingReply.value = true
  try {
    const deletedId = deleteReplyTarget.value.id
    await deleteReply(deletedId)
    repliesData.value = replies.value.filter(reply => reply.id !== deletedId)
    if (post.value) {
      postData.value = { ...post.value, replyCount: Math.max(0, (post.value.replyCount || 0) - 1) }
    }
    deleteReplyTarget.value = null
  } catch (e: any) {
    console.warn('删除回复失败:', e?.message)
    deleteReplyTarget.value = null
  } finally {
    deletingReply.value = false
  }
}

function authorName(item: Pick<CommunityPostDto | CommunityReplyDto, 'displayName' | 'username'>) {
  const displayName = (item.displayName || '').trim()
  if (displayName && !/^1\d{10}$/.test(displayName)) return displayName
  if (item.username && !/^1\d{10}$/.test(item.username)) return item.username
  return '番舍同好'
}

function openLightbox(index: number) {
  lightboxIndex.value = index
  lightboxOpen.value = true
  document.body.style.overflow = 'hidden'
}

function closeLightbox() {
  lightboxOpen.value = false
  document.body.style.overflow = ''
}

function prevImage() {
  if (!post.value?.images) return
  lightboxIndex.value = (lightboxIndex.value - 1 + post.value.images.length) % post.value.images.length
}

function nextImage() {
  if (!post.value?.images) return
  lightboxIndex.value = (lightboxIndex.value + 1) % post.value.images.length
}

function onLightboxKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') closeLightbox()
  if (e.key === 'ArrowLeft') prevImage()
  if (e.key === 'ArrowRight') nextImage()
}

function formatTime(value?: string) {
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

useHead(() => ({
  title: post.value ? `${post.value.title} - ${post.value.communityName}` : '帖子详情 - 萤火社区',
  meta: [{ name: 'description', content: post.value?.content?.slice(0, 100) || '在社区盒子中与同好交流。' }],
}))
</script>
