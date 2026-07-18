import type { AnimeDto, AnimeListResponse, ApiResponse } from '~/types/anime'

/**
 * 动漫数据获取 composable
 * 使用 $fetch 直接请求（通过 nitro routeRules 代理到后端）
 * 当后端不可用时返回空数据，保证页面正常渲染
 */
export function useAnime() {
  const config = useRuntimeConfig()
  const userStore = useUserStore()
  const base = config.public.apiBase || '/api'
  const api = (path: string) => `${base}${path}`
  const authHeaders = () => userStore.backendToken
    ? { Authorization: `Bearer ${userStore.backendToken}` }
    : undefined

  // ─── 热门排行 ───────────────────────────────────────────────

  /** 获取高分动漫（贝叶斯排序） */
  const fetchTopRated = async (page = 0, size = 12): Promise<AnimeDto[]> => {
    try {
      const res = await $fetch<ApiResponse<AnimeListResponse>>(
        api('/anime/top-rated'),
        { params: { page, size } }
      )
      return res?.data?.content ?? []
    } catch (e) {
      console.warn('获取热门排行失败（后端可能未启动）:', (e as Error).message)
      return []
    }
  }

  /** 获取站内用户评分排行 */
  const fetchCommunityRated = async (page = 0, size = 20): Promise<AnimeDto[]> => {
    try {
      const res = await $fetch<ApiResponse<AnimeListResponse>>(
        api('/anime/community-rated'),
        { params: { page, size } }
      )
      return res?.data?.content ?? []
    } catch (e) {
      console.warn('获取站内评分排行失败:', (e as Error).message)
      return []
    }
  }

  /** 获取全站萤火投票排行 */
  const fetchFireflyRanking = async (page = 0, size = 20): Promise<AnimeDto[]> => {
    try {
      const res = await $fetch<ApiResponse<AnimeListResponse>>(
        api('/anime/firefly-ranking'),
        { params: { page, size } }
      )
      return res?.data?.content ?? []
    } catch (e) {
      console.warn('获取萤火投票排行失败:', (e as Error).message)
      return []
    }
  }

  // ─── 全部动漫列表 ──────────────────────────────────────────

  /** 获取动漫列表（支持排序） */
  const fetchAnimeList = async (
    page = 0,
    size = 20,
    sortBy = 'bayesianRating',
    direction = 'desc'
  ): Promise<AnimeListResponse> => {
    try {
      const res = await $fetch<ApiResponse<AnimeListResponse>>(
        api('/anime'),
        { params: { page, size, sortBy, direction } }
      )
      return res?.data ?? { content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: size, last: true }
    } catch (e) {
      console.warn('获取动漫列表失败:', (e as Error).message)
      return { content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: size, last: true }
    }
  }

  // ─── 动漫详情 ──────────────────────────────────────────────

  /** 根据 ID 获取动漫详情 */
  const fetchAnimeById = async (id: number): Promise<AnimeDto | null> => {
    try {
      const res = await $fetch<ApiResponse<AnimeDto>>(
        api(`/anime/${id}`)
      )
      return res?.data ?? null
    } catch (e) {
      console.warn('获取动漫详情失败:', (e as Error).message)
      return null
    }
  }

  /** 根据 MAL ID 获取动漫 */
  const fetchAnimeByMalId = async (malId: number): Promise<AnimeDto | null> => {
    try {
      const res = await $fetch<ApiResponse<AnimeDto>>(
        api(`/anime/mal/${malId}`)
      )
      return res?.data ?? null
    } catch (e) {
      console.warn('获取动漫失败:', (e as Error).message)
      return null
    }
  }

  // ─── 搜索 ──────────────────────────────────────────────────

  /** 按关键词搜索 */
  const searchAnime = async (keyword: string, page = 0, size = 20): Promise<AnimeDto[]> => {
    const res = await searchAnimeList(keyword, page, size)
    return res.content
  }

  /** 按关键词搜索（返回分页信息） */
  const searchAnimeList = async (keyword: string, page = 0, size = 20): Promise<AnimeListResponse> => {
    try {
      const res = await $fetch<ApiResponse<AnimeListResponse>>(
        api('/anime/search'),
        { params: { keyword, page, size } }
      )
      return res?.data ?? { content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: size, last: true }
    } catch (e) {
      console.warn('搜索失败:', (e as Error).message)
      return { content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: size, last: true }
    }
  }

  // ─── 按类型筛选 ────────────────────────────────────────────

  /** 按类型（genre）筛选 */
  const fetchByGenre = async (genre: string, page = 0, size = 20): Promise<AnimeDto[]> => {
    const res = await fetchByGenreList(genre, page, size)
    return res.content
  }

  /** 按类型（genre）筛选，返回分页信息 */
  const fetchByGenreList = async (genre: string, page = 0, size = 20): Promise<AnimeListResponse> => {
    try {
      const res = await $fetch<ApiResponse<AnimeListResponse>>(
        api(`/anime/genre/${encodeURIComponent(genre)}`),
        { params: { page, size } }
      )
      return res?.data ?? { content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: size, last: true }
    } catch (e) {
      console.warn('类型筛选失败:', (e as Error).message)
      return { content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: size, last: true }
    }
  }

  // ─── 评分与评论 ──────────────────────────────────────────

  /** 获取动漫评论列表 */
  const fetchReviews = async (animeId: number, page = 0, size = 20): Promise<RatingDto[]> => {
    try {
      const res = await $fetch<ApiResponse<RatingDto[]>>(
        api(`/ratings/anime/${animeId}/reviews`),
        { params: { page, size } }
      )
      return res?.data ?? []
    } catch (e) {
      console.warn('获取评论失败:', (e as Error).message)
      return []
    }
  }

  /** 提交评分/评论 */
  const submitRating = async (data: {
    animeId: number
    score: number
    review?: string
    displayName?: string
    avatarUrl?: string
  }): Promise<RatingDto | null> => {
    try {
      const apiReady = await userStore.ensureBackendToken()
      if (!apiReady) {
        throw new Error('登录状态需要刷新，请重新登录后再发布')
      }

      const publicProfile = currentPublicProfile()
      const res = await $fetch<ApiResponse<RatingDto>>(
        api('/ratings'),
        {
          method: 'POST',
          body: {
            ...data,
            displayName: data.displayName || publicProfile.displayName,
            avatarUrl: data.avatarUrl || publicProfile.avatarUrl,
          },
        }
      )
      if (res?.success === false) {
        throw new Error(res.message || '提交失败，请稍后重试')
      }
      // 处理后端返回非标准错误格式（如 401 穿透过来的 error 字段）
      if ((res as any)?.error) {
        throw new Error((res as any).message || (res as any).error || '鉴权失败，请重新登录后重试')
      }
      if (!res?.data) {
        throw new Error('服务器返回异常，请稍后重试')
      }
      return res.data
    } catch (e: any) {
      console.warn('提交评分失败:', e?.message || e)
      if (e?.data?.message) {
        throw new Error(e.data.message)
      }
      throw new Error(e?.message || '提交失败，请稍后重试')
    }
  }

  /** 更新评分/评论 */
  const updateRating = async (id: number, data: {
    score?: number
    review?: string
    displayName?: string
    avatarUrl?: string
  }): Promise<RatingDto | null> => {
    try {
      const publicProfile = currentPublicProfile()
      const res = await $fetch<ApiResponse<RatingDto>>(
        api(`/ratings/${id}`),
        {
          method: 'PUT',
          body: {
            ...data,
            displayName: data.displayName || publicProfile.displayName,
            avatarUrl: data.avatarUrl || publicProfile.avatarUrl,
          },
        }
      )
      return res?.data ?? null
    } catch (e: any) {
      console.warn('更新评分失败:', e?.message || e)
      return null
    }
  }

  /** 获取某条评论下的楼中楼回复 */
  const fetchRatingReplies = async (ratingId: number, page = 0, size = 20): Promise<RatingReplyDto[]> => {
    try {
      const res = await $fetch<ApiResponse<RatingReplyDto[]>>(
        api(`/ratings/${ratingId}/replies`),
        { params: { page, size } }
      )
      return res?.data ?? []
    } catch (e) {
      console.warn('获取评论回复失败:', (e as Error).message)
      return []
    }
  }

  /** 回复某条评分评论 */
  const submitRatingReply = async (ratingId: number, content: string): Promise<RatingReplyDto | null> => {
    try {
      const apiReady = await userStore.ensureBackendToken()
      if (!apiReady) {
        throw new Error('登录状态需要刷新，请重新登录后再回复')
      }

      const publicProfile = currentPublicProfile()
      const res = await $fetch<ApiResponse<RatingReplyDto>>(
        api(`/ratings/${ratingId}/replies`),
        {
          method: 'POST',
          body: {
            content,
            displayName: publicProfile.displayName,
            avatarUrl: publicProfile.avatarUrl,
          },
        }
      )

      if (res?.success === false) {
        throw new Error(res.message || '回复失败，请稍后重试')
      }
      if (!res?.data) {
        throw new Error('服务器返回异常，请稍后重试')
      }
      return res.data
    } catch (e: any) {
      console.warn('提交评论回复失败:', e?.message || e)
      if (e?.data?.message) {
        throw new Error(e.data.message)
      }
      throw new Error(e?.message || '回复失败，请稍后重试')
    }
  }

  /** 点亮楼中楼回复 */
  const likeRatingReply = async (replyId: number): Promise<RatingReplyDto | null> => {
    try {
      const apiReady = await userStore.ensureBackendToken()
      if (!apiReady) {
        throw new Error('登录状态需要刷新，请重新登录后再点亮')
      }

      const res = await $fetch<ApiResponse<RatingReplyDto>>(
        api(`/rating-replies/${replyId}/like`),
        { method: 'POST' }
      )
      if (res?.success === false) {
        throw new Error(res.message || '点亮失败，请稍后重试')
      }
      return res?.data ?? null
    } catch (e: any) {
      console.warn('点亮评论回复失败:', e?.message || e)
      if (e?.data?.message) {
        throw new Error(e.data.message)
      }
      throw new Error(e?.message || '点亮失败，请稍后重试')
    }
  }

  /** 删除自己的评分/评论 */
  const deleteRating = async (id: number): Promise<void> => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再操作')

    const res = await $fetch<ApiResponse<string>>(api(`/ratings/${id}`), { method: 'DELETE' })
    if (res?.success === false) {
      throw new Error(res?.message || '删除评论失败')
    }
  }

  /** 删除自己的楼中楼回复 */
  const deleteRatingReply = async (replyId: number): Promise<void> => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再操作')

    const res = await $fetch<ApiResponse<string>>(api(`/rating-replies/${replyId}`), { method: 'DELETE' })
    if (res?.success === false) {
      throw new Error(res?.message || '删除回复失败')
    }
  }

  /** 投出一张萤火票 */
  const voteFirefly = async (animeId: number): Promise<{ animeId: number; fireflyVoteCount: number } | null> => {
    try {
      const apiReady = await userStore.ensureBackendToken()
      if (!apiReady) {
        throw new Error('登录状态需要刷新，请重新登录后再投票')
      }

      const res = await $fetch<ApiResponse<{ animeId: number; fireflyVoteCount: number }>>(
        api('/votes'),
        { method: 'POST', headers: authHeaders(), body: { animeId } }
      )
      if (res?.success === false) {
        throw new Error(res.message || '投票失败，请稍后重试')
      }
      return res?.data ?? null
    } catch (e: any) {
      console.warn('萤火投票失败:', e?.message || e)
      if (e?.data?.message) {
        throw new Error(e.data.message)
      }
      throw new Error('投票失败，请稍后重试')
    }
  }

  function currentPublicProfile() {
    const user = userStore.user
    const name = (user?.name || '').trim()
    const phone = (user?.phone || '').trim()
    const isPublicName = name && name !== phone && !/^1\d{10}$/.test(name) && name !== '番舍同好'

    return {
      displayName: isPublicName ? name : undefined,
      avatarUrl: user?.avatar || undefined,
    }
  }

  return {
    fetchTopRated,
    fetchCommunityRated,
    fetchFireflyRanking,
    fetchAnimeList,
    fetchAnimeById,
    fetchAnimeByMalId,
    searchAnime,
    searchAnimeList,
    fetchByGenre,
    fetchByGenreList,
    fetchReviews,
    submitRating,
    updateRating,
    fetchRatingReplies,
    submitRatingReply,
    likeRatingReply,
    deleteRating,
    deleteRatingReply,
    voteFirefly,
  }
}

/** 后端评分 DTO 类型 */
export interface RatingDto {
  id: number
  userId: number
  username: string
  displayName?: string
  avatarUrl?: string
  animeId: number
  animeTitle?: string
  animeCoverImage?: string
  score: number
  review?: string
  storyScore?: number
  animationScore?: number
  soundScore?: number
  characterScore?: number
  enjoymentScore?: number
  isRecommended?: boolean
  containsSpoiler?: boolean
  likeCount: number
  createdAt: string
  updatedAt?: string
}

/** 后端评论回复 DTO 类型 */
export interface RatingReplyDto {
  id: number
  ratingId: number
  userId: number
  username: string
  displayName?: string
  avatarUrl?: string
  content: string
  likeCount: number
  createdAt: string
  updatedAt?: string
}
