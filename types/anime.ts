/** 后端 API 统一响应结构 */
export interface ApiResponse<T> {
  success?: boolean
  code: number
  message: string
  data: T
}

/** 动漫 DTO（对应后端 AnimeDto） */
export interface AnimeDto {
  id: number
  malId: number
  title: string
  titleJapanese?: string
  titleEnglish?: string
  titleCn?: string
  searchAliases?: string
  synopsis?: string
  synopsisCn?: string
  coverImage: string
  trailerUrl?: string
  type?: string        // TV / Movie / OVA / ONA / Special
  status?: string       // Airing / Finished / Not yet aired
  airedFrom?: string
  airedTo?: string
  episodes?: number
  durationMinutes?: number
  rating?: string       // G / PG / PG-13 / R
  popularity?: number
  membersCount?: number
  favoritesCount?: number
  hasAgent?: boolean
  bayesianRating?: number
  meanRating?: number
  ratingCount?: number
  communityBayesianRating?: number
  communityMeanRating?: number
  communityRatingCount?: number
  fireflyVoteCount?: number
  genres: string[]
  studio?: string
  source?: string
  season?: string
  year?: number
  createdAt?: string
  updatedAt?: string
}

/** 分页列表响应 */
export interface AnimeListResponse {
  content: AnimeDto[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
  last: boolean
}
