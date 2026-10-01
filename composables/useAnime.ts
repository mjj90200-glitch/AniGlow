import type { AnimeDto, AnimeListResponse } from '~/types/anime'

export function useAnime() {
  const { request } = useApi()
  const userStore = useUserStore()
  const emptyPage = (size: number): AnimeListResponse => ({
    content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: size, last: true,
  })

  const fetchTopRated = async (page = 0, size = 12): Promise<AnimeDto[]> => {
    try { return (await request<AnimeListResponse>('/anime/top-rated', { params: { page, size } })).content }
    catch (error) { console.warn('获取热门排行失败:', (error as Error).message); return [] }
  }

  const fetchCommunityRated = async (page = 0, size = 20): Promise<AnimeDto[]> => {
    try { return (await request<AnimeListResponse>('/anime/community-rated', { params: { page, size } })).content }
    catch (error) { console.warn('获取站内评分排行失败:', (error as Error).message); return [] }
  }

  const fetchFireflyRanking = async (page = 0, size = 20): Promise<AnimeDto[]> => {
    try { return (await request<AnimeListResponse>('/anime/firefly-ranking', { params: { page, size } })).content }
    catch (error) { console.warn('获取萤火排行失败:', (error as Error).message); return [] }
  }

  const fetchAnimeList = async (
    page = 0, size = 20, sortBy = 'bayesianRating', direction = 'desc',
  ): Promise<AnimeListResponse> => {
    try { return await request<AnimeListResponse>('/anime', { params: { page, size, sortBy, direction } }) }
    catch (error) { console.warn('获取动漫列表失败:', (error as Error).message); return emptyPage(size) }
  }

  const fetchAnimeById = async (id: number): Promise<AnimeDto | null> => {
    try { return await request<AnimeDto>(`/anime/${id}`) }
    catch (error) { console.warn('获取动漫详情失败:', (error as Error).message); return null }
  }

  const fetchAnimeByMalId = async (malId: number): Promise<AnimeDto | null> => {
    try { return await request<AnimeDto>(`/anime/mal/${malId}`) }
    catch (error) { console.warn('获取动漫失败:', (error as Error).message); return null }
  }

  const searchAnimeList = async (keyword: string, page = 0, size = 20): Promise<AnimeListResponse> => {
    try { return await request<AnimeListResponse>('/anime/search', { params: { keyword, page, size } }) }
    catch (error) { console.warn('搜索失败:', (error as Error).message); return emptyPage(size) }
  }

  const searchAnime = async (keyword: string, page = 0, size = 20): Promise<AnimeDto[]> =>
    (await searchAnimeList(keyword, page, size)).content

  const fetchByGenreList = async (genre: string, page = 0, size = 20): Promise<AnimeListResponse> => {
    try {
      return await request<AnimeListResponse>(`/anime/genre/${encodeURIComponent(genre)}`, { params: { page, size } })
    } catch (error) {
      console.warn('类型筛选失败:', (error as Error).message)
      return emptyPage(size)
    }
  }

  const fetchByGenre = async (genre: string, page = 0, size = 20): Promise<AnimeDto[]> =>
    (await fetchByGenreList(genre, page, size)).content

  const fetchFilterOptions = async (): Promise<{
    genres: string[]
    types: string[]
    countries: string[]
    years: number[]
  }> => {
    try {
      return await request('/anime/filter-options')
    } catch (error) {
      console.warn('获取筛选选项失败:', (error as Error).message)
      return { genres: [], types: [], countries: [], years: [] }
    }
  }

  const fetchFilteredAnime = async (
    filters: { keyword?: string; genre?: string; type?: string; yearFrom?: number; yearTo?: number; country?: string },
    page = 0,
    size = 20,
  ): Promise<AnimeListResponse> => {
    try {
      return await request<AnimeListResponse>('/anime/filter', {
        params: {
          keyword: filters.keyword || undefined,
          genre: filters.genre || undefined,
          type: filters.type || undefined,
          yearFrom: filters.yearFrom || undefined,
          yearTo: filters.yearTo || undefined,
          country: filters.country || undefined,
          page,
          size,
        },
      })
    } catch (error) {
      console.warn('筛选查询失败:', (error as Error).message)
      return emptyPage(size)
    }
  }

  const fetchReviews = async (animeId: number, page = 0, size = 20): Promise<RatingDto[]> => {
    try { return await request<RatingDto[]>(`/ratings/anime/${animeId}/reviews`, { params: { page, size } }) || [] }
    catch (error) { console.warn('获取评论失败:', (error as Error).message); return [] }
  }

  const submitRating = async (data: {
    animeId: number
    score: number
    review?: string
    displayName?: string
    avatarUrl?: string
  }): Promise<RatingDto> => {
    const profile = currentPublicProfile()
    return await request<RatingDto>('/ratings', {
      method: 'POST', auth: true,
      body: {
        ...data,
        displayName: data.displayName || profile.displayName,
        avatarUrl: data.avatarUrl || profile.avatarUrl,
      },
    })
  }

  const updateRating = async (id: number, data: {
    animeId?: number
    score?: number
    review?: string
    displayName?: string
    avatarUrl?: string
  }): Promise<RatingDto | null> => {
    try {
      const profile = currentPublicProfile()
      return await request<RatingDto>(`/ratings/${id}`, {
        method: 'PUT', auth: true,
        body: {
          ...data,
          displayName: data.displayName || profile.displayName,
          avatarUrl: data.avatarUrl || profile.avatarUrl,
        },
      })
    } catch (error) {
      console.warn('更新评分失败:', (error as Error).message)
      return null
    }
  }

  const fetchRatingReplies = async (ratingId: number, page = 0, size = 20): Promise<RatingReplyDto[]> => {
    try { return await request<RatingReplyDto[]>(`/ratings/${ratingId}/replies`, { params: { page, size } }) || [] }
    catch (error) { console.warn('获取评论回复失败:', (error as Error).message); return [] }
  }

  const submitRatingReply = (ratingId: number, content: string) => request<RatingReplyDto>(
    `/ratings/${ratingId}/replies`,
    { method: 'POST', auth: true, body: { content, ...currentPublicProfile() } },
  )

  const likeRatingReply = (replyId: number) => request<RatingReplyDto>(`/rating-replies/${replyId}/like`, {
    method: 'POST', auth: true,
  })

  const deleteRating = (id: number) => request<void>(`/ratings/${id}`, { method: 'DELETE', auth: true })

  const deleteRatingReply = (replyId: number) => request<void>(`/rating-replies/${replyId}`, {
    method: 'DELETE', auth: true,
  })

  const voteFirefly = (animeId: number) => request<{ animeId: number; fireflyVoteCount: number }>('/votes', {
    method: 'POST', body: { animeId }, auth: true,
  })

  function currentPublicProfile() {
    const user = userStore.user
    const name = (user?.name || '').trim()
    const phone = (user?.phone || '').trim()
    const visibleName = name && name !== phone && !/^1\d{10}$/.test(name) && name !== '番舍同好'
    return { displayName: visibleName ? name : undefined, avatarUrl: user?.avatar || undefined }
  }

  return {
    fetchTopRated, fetchCommunityRated, fetchFireflyRanking, fetchAnimeList, fetchAnimeById,
    fetchAnimeByMalId, searchAnime, searchAnimeList, fetchByGenre, fetchByGenreList, fetchReviews,
    submitRating, updateRating, fetchRatingReplies, submitRatingReply, likeRatingReply,
    deleteRating, deleteRatingReply, voteFirefly, fetchFilterOptions, fetchFilteredAnime,
  }
}

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
