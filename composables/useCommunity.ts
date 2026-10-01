export interface CommunityDto {
  id: number
  slug: string
  name: string
  description: string
  coverImage: string
  category: string
  tags: string[]
  relatedAnimeId?: number
  relatedAnimeTitle?: string
  postCount: number
  memberCount: number
  heatScore: number
  featured: boolean
  creatorId?: number
  createdAt: string
}

export interface CommunityPostDto {
  id: number
  communityId: number
  communitySlug: string
  communityName: string
  userId: number
  username: string
  displayName?: string
  avatarUrl?: string
  title: string
  content: string
  coverImage?: string
  images?: string[]
  likeCount: number
  likedByMe?: boolean
  replyCount: number
  viewCount: number
  pinned: boolean
  featured: boolean
  lastRepliedAt?: string
  createdAt: string
  updatedAt?: string
}

export interface CommunityReplyDto {
  id: number
  postId: number
  userId: number
  username: string
  displayName?: string
  avatarUrl?: string
  content: string
  likeCount: number
  likedByMe?: boolean
  createdAt: string
  updatedAt?: string
}

export interface PostPageResponse {
  items: CommunityPostDto[]
  total: number
  page: number
  size: number
}

export function useCommunity() {
  const { request } = useApi()
  const emptyPage = (page = 0, size = 20): PostPageResponse => ({ items: [], total: 0, page, size })

  const fetchCommunities = async (): Promise<CommunityDto[]> => {
    try { return await request<CommunityDto[]>('/communities') || [] }
    catch (error) { console.warn('获取社区盒子失败:', (error as Error).message); return [] }
  }

  const fetchCommunity = async (slug: string): Promise<CommunityDto | null> => {
    try { return await request<CommunityDto>(`/communities/${slug}`) }
    catch (error) { console.warn('获取社区详情失败:', (error as Error).message); return null }
  }

  const fetchPosts = async (slug: string, sort = 'latest', page = 0, size = 20): Promise<PostPageResponse> => {
    try { return await request<PostPageResponse>(`/communities/${slug}/posts`, { params: { sort, page, size } }) }
    catch (error) { console.warn('获取社区帖子失败:', (error as Error).message); return emptyPage(page, size) }
  }

  const fetchPostsByAnime = async (animeId: number, page = 0, size = 5): Promise<PostPageResponse> => {
    try { return await request<PostPageResponse>(`/communities/posts/by-anime/${animeId}`, { params: { page, size } }) }
    catch (error) { console.warn('获取番剧社区帖子失败:', (error as Error).message); return emptyPage(page, size) }
  }

  const uploadImages = async (files: File[]): Promise<string[]> => {
    const body = new FormData()
    files.forEach(file => body.append('files', file))
    return await request<string[]>('/communities/upload/images', { method: 'POST', body, auth: true })
  }

  const createPost = (slug: string, data: {
    title: string
    content: string
    coverImage?: string
    images?: string[]
  }) => request<CommunityPostDto>(`/communities/${slug}/posts`, {
    method: 'POST', body: { ...data, ...currentPublicProfile() }, auth: true,
  })

  const fetchPost = async (postId: number): Promise<CommunityPostDto | null> => {
    try { return await request<CommunityPostDto>(`/communities/posts/${postId}`) }
    catch (error) { console.warn('获取帖子详情失败:', (error as Error).message); return null }
  }

  const fetchMyPosts = async (page = 0, size = 20): Promise<PostPageResponse> => {
    try { return await request<PostPageResponse>('/communities/posts/my', { params: { page, size }, auth: true }) }
    catch (error) { console.warn('获取我的帖子失败:', (error as Error).message); return emptyPage(page, size) }
  }

  const deletePost = (postId: number) => request<void>(`/communities/posts/${postId}`, {
    method: 'DELETE', auth: true,
  })

  const likePost = (postId: number) => request<CommunityPostDto>(`/communities/posts/${postId}/like`, {
    method: 'POST', auth: true,
  })

  const fetchReplies = async (postId: number, page = 0, size = 50): Promise<CommunityReplyDto[]> => {
    try { return await request<CommunityReplyDto[]>(`/communities/posts/${postId}/replies`, { params: { page, size } }) || [] }
    catch (error) { console.warn('获取帖子回复失败:', (error as Error).message); return [] }
  }

  const createReply = (postId: number, content: string) => request<CommunityReplyDto>(
    `/communities/posts/${postId}/replies`,
    { method: 'POST', body: { content, ...currentPublicProfile() }, auth: true },
  )

  const deleteReply = (replyId: number) => request<void>(`/communities/replies/${replyId}`, {
    method: 'DELETE', auth: true,
  })

  const likeReply = (replyId: number) => request<CommunityReplyDto>(`/communities/replies/${replyId}/like`, {
    method: 'POST', auth: true,
  })

  function currentPublicProfile() {
    const user = useUserStore().user
    const name = (user?.name || '').trim()
    const phone = (user?.phone || '').trim()
    const visibleName = name && name !== phone && !/^1\d{10}$/.test(name) && name !== '番舍同好'
    return { displayName: visibleName ? name : undefined, avatarUrl: user?.avatar || undefined }
  }

  return {
    fetchCommunities, fetchCommunity, fetchPosts, fetchPostsByAnime, uploadImages, createPost,
    fetchPost, fetchMyPosts, deletePost, likePost, fetchReplies, createReply, deleteReply, likeReply,
  }
}
