<template>
  <NuxtPage v-if="isPostRoute" />
  <div v-else class="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 pt-24 pb-24">
    <div v-if="!community" class="glass-card-cream rounded-4xl p-10 text-center">
      <h1 class="text-2xl font-black text-gray-800 mb-3">这个盒子还没有被点亮</h1>
      <NuxtLink to="/community/all" class="btn-glow inline-flex px-6 py-3">回到社区广场</NuxtLink>
    </div>

    <template v-else>
      <div class="flex gap-5">
        <!-- 左侧边栏：社区列表 -->
        <div class="w-[250px] shrink-0 hidden lg:block">
          <CommunitySidebar :selected-slug="slug" />
        </div>

        <!-- 右侧内容区 -->
        <main class="flex-1 min-w-0">
          <!-- 社区介绍 Banner -->
          <div class="relative overflow-hidden rounded-2xl mb-5" style="background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);">
            <div class="absolute inset-0 opacity-10" :style="community.coverImage ? `background-image:url(${community.coverImage});background-size:cover;background-position:center` : ''" />
            <div class="relative px-6 py-6 sm:px-8 sm:py-8 flex flex-col sm:flex-row sm:items-center gap-5">
              <div class="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl overflow-hidden shrink-0 ring-2 ring-white/20 bg-white/10 flex items-center justify-center">
                <img v-if="community.coverImage" :src="community.coverImage" :alt="community.name" class="w-full h-full object-cover" />
                <Users v-else class="w-8 h-8 text-white/40" />
              </div>
              <div class="min-w-0 flex-1">
                <h1 class="text-xl sm:text-2xl font-extrabold text-white mb-1.5">{{ community.name }}</h1>
                <p class="text-sm text-white/60 leading-6 line-clamp-2 mb-3">{{ community.description }}</p>
                <div class="flex flex-wrap items-center gap-4 text-xs text-white/50">
                  <span v-if="community.slug !== 'all'" class="inline-flex items-center gap-1.5"><Users class="w-3.5 h-3.5" />{{ community.memberCount || 0 }} 成员</span>
                  <span v-if="community.tags && community.tags.length" class="inline-flex items-center gap-1.5">
                    <span v-for="tag in community.tags.slice(0, 4)" :key="tag" class="rounded-full bg-white/10 px-2.5 py-0.5 text-[11px] text-white/70">{{ tag }}</span>
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 排序栏 -->
          <div class="flex items-center justify-between gap-3 mb-3">
            <div class="flex bg-white border border-gray-100 rounded-lg overflow-hidden">
              <button
                v-for="item in sortTabs"
                :key="item.key"
                class="px-4 py-2 text-[13px] font-semibold transition border-b-2"
                :class="activeSort === item.key ? 'text-[#00C853] border-[#00C853] bg-white' : 'text-gray-400 border-transparent hover:text-gray-600'"
                @click="changeSort(item.key)"
              >
                {{ item.label }}
              </button>
            </div>
            <button v-if="community.slug !== 'all'" class="btn-glow px-4 py-2 gap-1.5 text-[13px]" @click="openPostBox">
              <PenLine class="w-3.5 h-3.5" />
              发布
            </button>
          </div>

          <!-- 帖子列表 · 朋友圈风格 -->
          <div v-if="posts.length" class="space-y-3">
            <PostCard
              v-for="post in posts"
              :key="post.id"
              :post="post"
              :community-slug="community.slug"
              :show-community="community.slug === 'all'"
            />
          </div>

          <!-- 空状态 -->
          <div v-if="!posts.length" class="bg-white rounded-xl sm:rounded-2xl border border-gray-100/80 px-5 py-16 text-center">
              <MessageCircle class="w-12 h-12 mx-auto text-gray-300 mb-4" />
              <h2 class="text-lg font-extrabold text-gray-400 mb-1.5">还没有帖子</h2>
              <p class="text-sm text-gray-400 mb-5">成为第一个点亮这个盒子的同好吧。</p>
              <button v-if="community.slug !== 'all'" class="btn-glow px-5 py-2.5 text-sm" @click="openPostBox">发布第一帖</button>
              <NuxtLink v-else to="/community/anime" class="btn-glow inline-flex px-5 py-2.5 text-sm">进入番剧社区</NuxtLink>
            </div>

          <!-- 加载更多 -->
          <button
            v-if="hasMore"
            class="w-full mt-3 py-3 text-[13px] font-semibold text-gray-500 hover:text-[#00C853] hover:bg-white rounded-lg border border-gray-100 transition bg-white"
            :class="loadingMore ? 'opacity-50 pointer-events-none' : ''"
            @click="loadMore"
          >
            {{ loadingMore ? '加载中...' : `加载更多（还剩 ${remainingPosts} 条）` }}
          </button>
        </main>
      </div>

      <!-- 发帖弹窗 -->
      <Transition enter-active-class="transition duration-200" enter-from-class="opacity-0" leave-active-class="transition duration-150" leave-to-class="opacity-0">
        <div v-if="showPostBox" class="fixed inset-0 z-[90] flex items-center justify-center px-4 py-8 bg-black/20 backdrop-blur-md" @click.self="showPostBox = false">
          <div class="glass-card-cream rounded-4xl p-6 sm:p-7 w-full max-w-2xl">
            <div class="flex items-center justify-between gap-4 mb-5">
              <div>
                <p class="text-xs font-black tracking-[0.25em] uppercase text-firefly-700">New Post</p>
                <h2 class="text-2xl font-black text-gray-900">在 {{ community.name }} 发帖</h2>
              </div>
              <button class="rounded-full bg-white/70 p-2 text-gray-500 hover:text-gray-800" @click="showPostBox = false">
                <X class="w-5 h-5" />
              </button>
            </div>
            <input
              v-model="draftTitle"
              class="w-full rounded-3xl border border-white/70 bg-white/70 px-4 py-3 text-sm font-bold outline-none focus:border-firefly focus:ring-4 focus:ring-firefly/10"
              placeholder="标题，例如：辛美尔那句台词我现在才懂"
            />
            <textarea
              v-model="draftContent"
              rows="7"
              class="mt-3 w-full resize-none rounded-3xl border border-white/70 bg-white/70 px-4 py-3 text-sm leading-7 outline-none focus:border-firefly focus:ring-4 focus:ring-firefly/10"
              placeholder="写下你的想法、安利、提问或者考据..."
            />
            <div class="mt-3 flex flex-wrap gap-2">
              <div
                v-for="(img, idx) in draftImages"
                :key="idx"
                class="relative w-20 h-20 rounded-2xl overflow-hidden shrink-0"
              >
                <img :src="img.preview" class="w-full h-full object-cover" alt="预览" />
                <div v-if="img.uploading" class="absolute inset-0 bg-black/40 flex items-center justify-center">
                  <div class="text-center text-white">
                    <span class="mx-auto block w-5 h-5 border-2 border-white/60 border-t-white rounded-full animate-spin"></span>
                    <span class="mt-1 block text-[9px] font-bold">{{ img.stage === 'optimizing' ? '优化中' : '上传中' }}</span>
                  </div>
                </div>
                <div v-if="img.error" class="absolute inset-0 bg-sakura/80 flex items-center justify-center text-white text-[10px] font-bold text-center leading-tight p-1">
                  {{ img.error }}
                </div>
                <button
                  class="absolute top-1 right-1 w-5 h-5 rounded-full bg-black/50 text-white flex items-center justify-center text-xs hover:bg-black/70"
                  @click="removeDraftImage(idx)"
                >×</button>
              </div>
              <label
                v-if="draftImages.length < MAX_IMAGES"
                class="w-20 h-20 rounded-2xl border-2 border-dashed border-firefly/30 flex items-center justify-center cursor-pointer hover:border-firefly/60 transition shrink-0"
                role="button"
                tabindex="0"
                aria-label="添加图片"
              >
                <span class="text-firefly/50 text-2xl font-light">+</span>
                <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple class="hidden" @change="handleImageSelect" />
              </label>
            </div>
            <p v-if="draftImages.length > 0" class="mt-1.5 text-[11px] text-gray-400 font-bold">
              <template v-if="pendingImageCount > 0">正在处理 {{ pendingImageCount }} 张图片，请稍候...</template>
              <template v-else>已选 {{ draftImages.length }}/{{ MAX_IMAGES }} 张 · 发布前已自动优化大小</template>
            </p>
            <p v-if="submitError" class="mt-2 text-sm font-bold text-sakura-dark">{{ submitError }}</p>
            <div class="flex justify-end gap-3 mt-5">
              <button class="btn-glass px-5 py-2.5" @click="showPostBox = false">取消</button>
              <button
                class="btn-glow px-6 py-2.5"
                :class="!canSubmit || submitting ? 'opacity-40 pointer-events-none' : ''"
                @click="submitPost"
              >
                {{ submitting ? '发布中...' : '发布帖子' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>

    </template>
  </div>
</template>

<script setup lang="ts">
import { MessageCircle, PenLine, Users, X } from 'lucide-vue-next'
import type { CommunityPostDto } from '~/composables/useCommunity'
import PostCard from '~/components/community/PostCard.vue'
import { optimizeCommunityImage } from '~/utils/image-upload'

const route = useRoute()
const slug = computed(() => String(route.params.slug || ''))
const isPostRoute = computed(() => route.params.id != null)
const { fetchCommunity, fetchPosts, createPost, uploadImages } = useCommunity()
const { requireAuth } = useAuthModal()
const userStore = useUserStore()

const activeSort = ref<'latest' | 'hot'>('latest')
const showPostBox = ref(false)
const draftTitle = ref('')
const draftContent = ref('')
const submitError = ref('')
const submitting = ref(false)

type DraftImage = {
  file: File
  preview: string
  uploading: boolean
  stage: 'optimizing' | 'uploading' | 'done'
  url?: string
  error?: string
}

const draftImages = ref<DraftImage[]>([])
const MAX_IMAGES = 9

const currentPage = ref(0)
const totalPosts = ref(0)
const posts = ref<CommunityPostDto[]>([])
const loadingMore = ref(false)
const pageSize = 20

const sortTabs = [
  { key: 'latest' as const, label: '最新回复' },
  { key: 'hot' as const, label: '最多点亮' },
]

const { data: communityData, refresh: refreshCommunity } = await useAsyncData(
  () => `community-${slug.value}`,
  () => fetchCommunity(slug.value),
  { server: true, watch: [slug] }
)

const community = computed(() => communityData.value)
const pendingImageCount = computed(() => draftImages.value.filter(image => image.uploading).length)
const imagesReady = computed(() => draftImages.value.every(image => !image.uploading && !image.error))
const canSubmit = computed(() => draftTitle.value.trim().length > 0
  && draftContent.value.trim().length > 0
  && imagesReady.value)

const hasMore = computed(() => posts.value.length < totalPosts.value)
const remainingPosts = computed(() => totalPosts.value - posts.value.length)

async function loadInitialPosts() {
  currentPage.value = 0
  posts.value = []
  const result = await fetchPosts(slug.value, activeSort.value, 0, pageSize)
  posts.value = result.items
  totalPosts.value = result.total
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const nextPage = currentPage.value + 1
    const result = await fetchPosts(slug.value, activeSort.value, nextPage, pageSize)
    posts.value = [...posts.value, ...result.items]
    totalPosts.value = result.total
    currentPage.value = nextPage
  } finally {
    loadingMore.value = false
  }
}

function changeSort(sort: 'latest' | 'hot') {
  activeSort.value = sort
  loadInitialPosts()
}

function openPostBox() {
  requireAuth(() => {
    submitError.value = ''
    showPostBox.value = true
  })
}

function handleImageSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input?.files
  if (!files || files.length === 0) return

  const remaining = MAX_IMAGES - draftImages.value.length
  const toAdd = Array.from(files).slice(0, remaining)

  toAdd.forEach(file => {
    if (!file.type.match(/^image\/(jpeg|png|gif|webp)$/)) {
      return
    }
    const item = {
      file,
      preview: URL.createObjectURL(file),
      uploading: true,
      stage: 'optimizing' as const,
      url: undefined as string | undefined,
      error: undefined as string | undefined,
    }
    draftImages.value.push(item)
    // 使用数组返回的响应式代理，确保上传进度和发布状态会立即刷新。
    void uploadSingleImage(draftImages.value[draftImages.value.length - 1])
  })
  input.value = ''
}

async function uploadSingleImage(item: typeof draftImages.value[number]) {
  try {
    item.file = await optimizeCommunityImage(item.file, item.preview)
    item.stage = 'uploading'
    const urls = await uploadImages([item.file])
    item.url = urls[0]
    item.stage = 'done'
    item.uploading = false
  } catch (e: any) {
    item.error = e?.message || '上传失败'
    item.uploading = false
  }
}

function removeDraftImage(index: number) {
  const item = draftImages.value[index]
  if (item?.preview) URL.revokeObjectURL(item.preview)
  draftImages.value.splice(index, 1)
}

async function submitPost() {
  if (!community.value || !canSubmit.value || submitting.value) return
  submitting.value = true
  submitError.value = ''
  try {
    const uploadedUrls = draftImages.value
      .filter(img => img.url)
      .map(img => img.url!)
    const post = await createPost(community.value.slug, {
      title: draftTitle.value.trim(),
      content: draftContent.value.trim(),
      coverImage: community.value.coverImage,
      ...(uploadedUrls.length > 0 ? { images: uploadedUrls } : {}),
    })
    draftTitle.value = ''
    draftContent.value = ''
    draftImages.value.forEach(img => {
      if (img.preview) URL.revokeObjectURL(img.preview)
    })
    draftImages.value = []
    showPostBox.value = false
    await Promise.all([refreshCommunity(), loadInitialPosts()])
    await navigateTo(`/community/${community.value.slug}/posts/${post.id}`)
  } catch (e: any) {
    submitError.value = e?.message || '帖子没有送达，请稍后再试'
  } finally {
    submitting.value = false
  }
}

// 初始加载
if (!isPostRoute.value) {
  await loadInitialPosts()
}

watch([slug, isPostRoute], ([, postRoute]) => {
  if (!postRoute) void loadInitialPosts()
})

useHead(() => ({
  ...(isPostRoute.value
    ? {}
    : {
        title: community.value ? `${community.value.name} - 萤火社区` : '社区盒子 - 萤火社区',
        meta: [{ name: 'description', content: community.value?.description || '进入社区盒子与同好交流。' }],
      }),
}))
</script>
