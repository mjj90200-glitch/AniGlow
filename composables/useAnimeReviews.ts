import type { Ref } from 'vue'
import type { AnimeDto } from '~/types/anime'
import type { RatingDto, RatingReplyDto } from '~/composables/useAnime'
import type { Review, ReviewDeleteTarget, ReviewReply } from '~/types/review'
import {
  formatRelativeTime,
  guestGradient,
  hashText,
  normalizeReview,
  ratingLabel,
  resolvePublicReviewName,
  sortReviewsForDisplay,
} from '~/utils/anime-reviews'

interface UseAnimeReviewsOptions {
  animeId: Ref<number>
  getAnime: () => AnimeDto | null | undefined
  updateAnime: (anime: AnimeDto) => void
}

const REVIEW_STORAGE_PREFIX = 'aniglow_reviews_'
const REVIEW_LIKES_PREFIX = 'aniglow_review_likes_'
const REPLY_LIKES_PREFIX = 'aniglow_reply_likes_'

export function useAnimeReviews(options: UseAnimeReviewsOptions) {
  const {
    fetchAnimeById,
    fetchReviews,
    submitRating,
    fetchRatingReplies,
    submitRatingReply,
    likeRatingReply,
    deleteRating,
    deleteRatingReply,
  } = useAnime()
  const dailyStore = useDailyStore()
  const userStore = useUserStore()
  const { requireAuth } = useAuthModal()

  const selectedRating = ref(5)
  const userRating = ref(0)
  const hoverRating = ref(0)
  const draftComment = ref('')
  const submitError = ref('')
  const reviews = ref<Review[]>([])
  const likedReviewIds = ref<number[]>([])
  const likedReplyIds = ref<number[]>([])
  const openReplyReviewId = ref<number | null>(null)
  const replyDrafts = ref<Record<number, string>>({})
  const replyErrors = ref<Record<number, string>>({})
  const submittingReplyIds = ref<number[]>([])
  const commentPanelRef = useTemplateRef<HTMLDivElement>('commentPanelRef')
  const submitting = ref(false)
  const deleteTarget = ref<ReviewDeleteTarget | null>(null)
  const deleting = ref(false)
  let commentObserver: IntersectionObserver | undefined

  watch(options.animeId, async (id) => {
    if (!id) {
      reviews.value = []
      likedReviewIds.value = []
      likedReplyIds.value = []
      return
    }

    const backendReviews = await loadBackendReviews(id)
    reviews.value = mergeReviews(backendReviews, readStoredReviews(id))
    likedReviewIds.value = readStoredLikes(id)
    likedReplyIds.value = readStoredReplyLikes(id)
  }, { immediate: true })

  const ratingTabs = computed(() => [5, 4, 3, 2, 1].map(star => ({
    star,
    label: ratingLabel(star),
    count: reviews.value.filter(review => review.rating === star).length,
  })))

  const selectedReviews = computed(() => reviews.value
    .filter(review => review.rating === selectedRating.value)
    .slice()
    .sort(sortReviewsForDisplay))

  const topReview = computed(() => reviews.value.slice().sort((a, b) => b.likes - a.likes)[0])

  async function loadBackendReviews(id: number): Promise<Review[]> {
    try {
      const dtos = await fetchReviews(id, 0, 50)
      const mappedReviews = dtos
        .filter(dto => dto.review && dto.review.trim())
        .map(mapRatingDtoToReview)
      await Promise.all(mappedReviews.map(async (review) => {
        review.replies = await loadReviewReplies(review.id)
      }))
      return mappedReviews
    } catch {
      return []
    }
  }

  async function loadReviewReplies(ratingId: number): Promise<ReviewReply[]> {
    if (ratingId < 0) return []
    try {
      const replies = await fetchRatingReplies(ratingId, 0, 30)
      return replies.map(mapReplyDtoToReply)
    } catch {
      return []
    }
  }

  function mapRatingDtoToReview(dto: RatingDto): Review {
    const starRating = dto.score ? Math.round(dto.score / 2) : 5
    const ownAuthor = isOwnReview(dto.userId) ? resolveReviewAuthor() : null
    const backendName = resolvePublicReviewName(dto.displayName, dto.username)
    const displayName = ownAuthor?.hasPublicName ? ownAuthor.name : backendName
    const avatar = ownAuthor?.avatar || dto.avatarUrl || undefined
    return {
      id: dto.id,
      userId: dto.userId,
      user: displayName,
      avatar,
      rating: Math.min(5, Math.max(1, starRating)),
      content: dto.review || '',
      date: formatRelativeTime(dto.createdAt),
      likes: dto.likeCount || 0,
      avatarGradient: avatar ? 'linear-gradient(135deg, #00E676, #7BC4E0)' : guestGradient(String(dto.userId || dto.id)),
      createdAt: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
      isFresh: isOwnReview(dto.userId),
      replies: [],
    }
  }

  function mapReplyDtoToReply(dto: RatingReplyDto): ReviewReply {
    const ownAuthor = isOwnReview(dto.userId) ? resolveReviewAuthor() : null
    const backendName = resolvePublicReviewName(dto.displayName, dto.username)
    const displayName = ownAuthor?.hasPublicName ? ownAuthor.name : backendName
    const avatar = ownAuthor?.avatar || dto.avatarUrl || undefined
    return {
      id: dto.id,
      userId: dto.userId,
      user: displayName,
      avatar,
      content: dto.content || '',
      date: formatRelativeTime(dto.createdAt),
      likes: dto.likeCount || 0,
      avatarGradient: avatar ? 'linear-gradient(135deg, #00E676, #7BC4E0)' : guestGradient(String(dto.userId || dto.id)),
      createdAt: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
      isFresh: isOwnReview(dto.userId),
    }
  }

  function selectRating(star: number) {
    requireAuth(() => {
      userRating.value = star
      selectedRating.value = star
    })
  }

  function submitReview() {
    requireAuth(() => void doSubmitReview())
  }

  async function doSubmitReview() {
    if (!draftComment.value.trim() || submitting.value) return
    submitting.value = true
    submitError.value = ''
    const submittedContent = draftComment.value.trim()
    const submittedRating = selectedRating.value
    const optimisticReview = createLocalReview(submittedContent, submittedRating)

    applyOptimisticCommunityScore(submittedRating * 2)
    upsertReview(optimisticReview)
    userRating.value = submittedRating
    draftComment.value = ''

    try {
      const result = await submitRating({
        animeId: options.animeId.value,
        score: submittedRating * 2,
        review: submittedContent,
      })
      if (result) {
        const review = mapRatingDtoToReview(result)
        review.isFresh = true
        review.date = '刚刚'
        upsertReview(review, optimisticReview.id)
        await refreshAnime()
        dailyStore.completeTask('publish-comment')
      }
    } catch (error: unknown) {
      reviews.value = reviews.value.filter(review => review.id !== optimisticReview.id)
      draftComment.value = submittedContent
      submitError.value = error instanceof Error ? error.message : '评论暂时没有送达，请稍后再试'
      await refreshAnime()
    } finally {
      persistLocalReviews()
      submitting.value = false
    }
  }

  function toggleLike(id: number) {
    requireAuth(() => {
      const review = reviews.value.find(item => item.id === id)
      if (!review) return
      if (likedReviewIds.value.includes(id)) {
        likedReviewIds.value = likedReviewIds.value.filter(reviewId => reviewId !== id)
        review.likes = Math.max(0, review.likes - 1)
      } else {
        likedReviewIds.value = [...likedReviewIds.value, id]
        review.likes += 1
      }
      persistLocalReviews()
      persistStoredLikes()
    })
  }

  function toggleReplyBox(reviewId: number) {
    openReplyReviewId.value = openReplyReviewId.value === reviewId ? null : reviewId
    if (!replyDrafts.value[reviewId]) replyDrafts.value = { ...replyDrafts.value, [reviewId]: '' }
    replyErrors.value = { ...replyErrors.value, [reviewId]: '' }
  }

  function submitReply(reviewId: number) {
    requireAuth(() => void doSubmitReply(reviewId))
  }

  async function doSubmitReply(reviewId: number) {
    const content = (replyDrafts.value[reviewId] || '').trim()
    if (!content || submittingReplyIds.value.includes(reviewId)) return
    const review = reviews.value.find(item => item.id === reviewId)
    if (!review) return

    const optimisticReply = createLocalReply(content)
    review.replies = [...review.replies, optimisticReply]
    replyDrafts.value = { ...replyDrafts.value, [reviewId]: '' }
    replyErrors.value = { ...replyErrors.value, [reviewId]: '' }
    submittingReplyIds.value = [...submittingReplyIds.value, reviewId]

    try {
      const result = await submitRatingReply(reviewId, content)
      if (result) {
        const savedReply = mapReplyDtoToReply(result)
        savedReply.isFresh = true
        savedReply.date = '刚刚'
        replaceReply(reviewId, optimisticReply.id, savedReply)
        openReplyReviewId.value = null
      }
    } catch (error: unknown) {
      removeReply(reviewId, optimisticReply.id)
      replyDrafts.value = { ...replyDrafts.value, [reviewId]: content }
      replyErrors.value = {
        ...replyErrors.value,
        [reviewId]: error instanceof Error ? error.message : '回复暂时没有送达，请稍后再试',
      }
    } finally {
      submittingReplyIds.value = submittingReplyIds.value.filter(id => id !== reviewId)
    }
  }

  function createLocalReply(content: string): ReviewReply {
    const author = resolveReviewAuthor()
    return {
      id: -Date.now(),
      userId: Number(author.id) || undefined,
      user: author.name,
      avatar: author.avatar,
      content,
      date: '刚刚',
      likes: 0,
      avatarGradient: author.gradient,
      isFresh: true,
      createdAt: Date.now(),
    }
  }

  function replaceReply(reviewId: number, optimisticId: number, reply: ReviewReply) {
    const review = reviews.value.find(item => item.id === reviewId)
    if (review) review.replies = review.replies.map(item => item.id === optimisticId ? reply : item)
  }

  function removeReply(reviewId: number, replyId: number) {
    const review = reviews.value.find(item => item.id === reviewId)
    if (review) review.replies = review.replies.filter(item => item.id !== replyId)
  }

  function toggleReplyLike(reviewId: number, replyId: number) {
    requireAuth(() => void doToggleReplyLike(reviewId, replyId))
  }

  async function doToggleReplyLike(reviewId: number, replyId: number) {
    const reply = reviews.value.find(item => item.id === reviewId)?.replies.find(item => item.id === replyId)
    if (!reply) return
    if (likedReplyIds.value.includes(replyId)) {
      likedReplyIds.value = likedReplyIds.value.filter(id => id !== replyId)
      reply.likes = Math.max(0, reply.likes - 1)
      persistStoredReplyLikes()
      return
    }

    likedReplyIds.value = [...likedReplyIds.value, replyId]
    reply.likes += 1
    persistStoredReplyLikes()
    try {
      if (replyId > 0) {
        const updated = await likeRatingReply(replyId)
        if (updated) reply.likes = updated.likeCount || reply.likes
      }
    } catch {
      likedReplyIds.value = likedReplyIds.value.filter(id => id !== replyId)
      reply.likes = Math.max(0, reply.likes - 1)
      persistStoredReplyLikes()
    }
  }

  function confirmDeleteReview(review: Review) {
    requireAuth(() => { deleteTarget.value = { type: 'review', id: review.id } })
  }

  function confirmDeleteReply(reviewId: number, reply: ReviewReply) {
    requireAuth(() => { deleteTarget.value = { type: 'reply', reviewId, replyId: reply.id } })
  }

  async function doDelete() {
    if (!deleteTarget.value || deleting.value) return
    const target = deleteTarget.value
    deleting.value = true
    try {
      if (target.type === 'review') {
        await deleteRating(target.id)
        reviews.value = reviews.value.filter(review => review.id !== target.id)
      } else {
        await deleteRatingReply(target.replyId)
        removeReply(target.reviewId, target.replyId)
      }
      deleteTarget.value = null
      persistLocalReviews()
    } catch (error: unknown) {
      console.warn('删除失败:', error instanceof Error ? error.message : error)
    } finally {
      deleting.value = false
    }
  }

  function createLocalReview(content: string, rating: number): Review {
    const author = resolveReviewAuthor()
    return {
      id: -Date.now(),
      userId: Number(author.id) || undefined,
      user: author.name,
      avatar: author.avatar,
      rating,
      content,
      date: '刚刚',
      likes: 0,
      avatarGradient: author.gradient,
      isFresh: true,
      createdAt: Date.now(),
      replies: [],
    }
  }

  function upsertReview(review: Review, optimisticId?: number) {
    reviews.value = [review, ...reviews.value.filter((item) => {
      if (optimisticId && item.id === optimisticId) return false
      if (item.id === review.id) return false
      if (review.userId && item.userId === review.userId) return false
      return true
    })]
  }

  function applyOptimisticCommunityScore(score: number) {
    const anime = options.getAnime()
    if (!anime) return
    const previousCount = anime.communityRatingCount ?? 0
    const previousMean = anime.communityMeanRating ?? 0
    const previousOwnReview = findOwnReview()
    const oldScore = previousOwnReview ? previousOwnReview.rating * 2 : undefined
    const nextCount = previousOwnReview ? Math.max(previousCount, 1) : previousCount + 1
    const nextMean = previousOwnReview && previousCount > 0
      ? ((previousMean * previousCount) - (oldScore ?? 0) + score) / previousCount
      : ((previousMean * previousCount) + score) / nextCount
    options.updateAnime({
      ...anime,
      communityMeanRating: Math.round(nextMean * 10) / 10,
      communityRatingCount: nextCount,
    })
  }

  async function refreshAnime() {
    const latestAnime = await fetchAnimeById(options.animeId.value)
    if (latestAnime) options.updateAnime(latestAnime)
  }

  function findOwnReview() {
    const currentUserId = Number(userStore.user?.id)
    return currentUserId ? reviews.value.find(review => review.userId === currentUserId) : undefined
  }

  function isOwnReview(userId?: number) {
    const currentUserId = Number(userStore.user?.id)
    return Boolean(currentUserId && userId === currentUserId)
  }

  function storageKey(prefix: string, id = options.animeId.value) {
    return `${prefix}${id}`
  }

  function readJson<T>(key: string, fallback: T): T {
    if (import.meta.server) return fallback
    try {
      const raw = localStorage.getItem(key)
      return raw ? JSON.parse(raw) : fallback
    } catch {
      return fallback
    }
  }

  function readStoredReviews(id: number): Review[] {
    return readJson<Review[]>(storageKey(REVIEW_STORAGE_PREFIX, id), []).map(normalizeReview)
  }

  function readStoredLikes(id: number): number[] {
    return readJson<number[]>(storageKey(REVIEW_LIKES_PREFIX, id), [])
  }

  function readStoredReplyLikes(id: number): number[] {
    return readJson<number[]>(storageKey(REPLY_LIKES_PREFIX, id), [])
  }

  function writeJson(key: string, value: unknown) {
    if (import.meta.server) return
    try {
      localStorage.setItem(key, JSON.stringify(value))
    } catch {
      // localStorage may be unavailable in private modes.
    }
  }

  function persistLocalReviews() {
    writeJson(storageKey(REVIEW_STORAGE_PREFIX), reviews.value.filter(review => review.isLocal))
  }

  function persistStoredLikes() {
    writeJson(storageKey(REVIEW_LIKES_PREFIX), likedReviewIds.value)
  }

  function persistStoredReplyLikes() {
    writeJson(storageKey(REPLY_LIKES_PREFIX), likedReplyIds.value)
  }

  function mergeReviews(backendReviews: Review[], localReviews: Review[]) {
    const backendIds = new Set(backendReviews.map(review => review.id))
    const uniqueLocal = localReviews.filter(review => !backendIds.has(review.id)).map(normalizeReview)
    return [...uniqueLocal, ...backendReviews.map(normalizeReview)]
  }

  function resolveReviewAuthor() {
    const user = userStore.user
    const fallbackId = getOrCreateGuestId()
    const fallbackName = getOrCreateGuestName(fallbackId)
    const savedName = (user?.name || '').trim()
    const isPublicName = savedName && savedName !== user?.phone && !/^1\d{10}$/.test(savedName)
    return {
      id: user?.id || fallbackId,
      name: isPublicName ? savedName : fallbackName,
      avatar: user?.avatar || '',
      hasPublicName: Boolean(isPublicName),
      gradient: user?.avatar ? 'linear-gradient(135deg, #00E676, #7BC4E0)' : guestGradient(user?.id || fallbackId),
    }
  }

  function getOrCreateGuestId() {
    if (import.meta.server) return 'guest'
    const key = 'aniglow_guest_id'
    const existing = localStorage.getItem(key)
    if (existing) return existing
    const id = `guest-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`
    localStorage.setItem(key, id)
    return id
  }

  function getOrCreateGuestName(id: string) {
    if (import.meta.server) return '路过的番舍同好'
    const key = 'aniglow_guest_name'
    const existing = localStorage.getItem(key)
    if (existing) return existing
    const names = ['路过的萤火', '刚入坑的同好', '深夜补番人', '弹幕旁听生', '小小观测员']
    const name = names[Math.abs(hashText(id)) % names.length]
    localStorage.setItem(key, name)
    return name
  }

  onMounted(() => {
    if (!commentPanelRef.value) return
    commentObserver = new IntersectionObserver((entries) => {
      if (entries.some(entry => entry.isIntersecting)) {
        dailyStore.completeTask('view-comments')
        commentObserver?.disconnect()
        commentObserver = undefined
      }
    }, { threshold: 0.35 })
    commentObserver.observe(commentPanelRef.value)
  })

  onBeforeUnmount(() => commentObserver?.disconnect())

  return {
    selectedRating,
    userRating,
    hoverRating,
    draftComment,
    submitError,
    likedReviewIds,
    likedReplyIds,
    openReplyReviewId,
    replyDrafts,
    replyErrors,
    submittingReplyIds,
    commentPanelRef,
    submitting,
    deleteTarget,
    deleting,
    ratingTabs,
    selectedReviews,
    topReview,
    selectRating,
    submitReview,
    toggleLike,
    toggleReplyBox,
    submitReply,
    toggleReplyLike,
    confirmDeleteReview,
    confirmDeleteReply,
    doDelete,
    isOwnReview,
  }
}
