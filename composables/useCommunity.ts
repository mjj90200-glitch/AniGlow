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
  createdAt: string
  updatedAt?: string
}

export interface PostPageResponse {
  items: CommunityPostDto[]
  total: number
  page: number
  size: number
}

interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
}

export function useCommunity() {
  const config = useRuntimeConfig()
  const userStore = useUserStore()
  const base = config.public.apiBase || '/api'
  const api = (path: string) => `${base}${path}`
  const authHeaders = () => userStore.backendToken
    ? { Authorization: `Bearer ${userStore.backendToken}` }
    : undefined

  const fetchCommunities = async (): Promise<CommunityDto[]> => {
    try {
      const res = await $fetch<ApiResponse<CommunityDto[]>>(api('/communities'))
      return res?.data ?? []
    } catch (e) {
      console.warn('获取社区盒子失败:', (e as Error).message)
      return []
    }
  }

  const fetchCommunity = async (slug: string): Promise<CommunityDto | null> => {
    try {
      const res = await $fetch<ApiResponse<CommunityDto>>(api(`/communities/${slug}`))
      return res?.data ?? null
    } catch (e) {
      console.warn('获取社区详情失败:', (e as Error).message)
      return null
    }
  }

  const fetchPosts = async (slug: string, sort = 'latest', page = 0, size = 20): Promise<PostPageResponse> => {
    try {
      const res = await $fetch<ApiResponse<PostPageResponse>>(api(`/communities/${slug}/posts`), {
        params: { sort, page, size },
      })
      return res?.data ?? { items: [], total: 0, page: 0, size: 20 }
    } catch (e) {
      console.warn('获取社区帖子失败:', (e as Error).message)
      return { items: [], total: 0, page: 0, size: 20 }
    }
  }

  const fetchPostsByAnime = async (animeId: number, page = 0, size = 5): Promise<PostPageResponse> => {
    try {
      const res = await $fetch<ApiResponse<PostPageResponse>>(api(`/communities/posts/by-anime/${animeId}`), {
        params: { page, size },
      })
      return res?.data ?? { items: [], total: 0, page, size }
    } catch (e) {
      console.warn('获取番剧相关社区帖子失败:', (e as Error).message)
      return { items: [], total: 0, page, size }
    }
  }

  const uploadImages = async (files: File[]): Promise<string[]> => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再上传')

    const formData = new FormData()
    files.forEach(f => formData.append('files', f))

    const res = await $fetch<ApiResponse<string[]>>(api('/communities/upload/images'), {
      method: 'POST',
      headers: authHeaders(),
      body: formData,
    })
    if (res?.success === false || !res?.data) {
      throw new Error(res?.message || '图片上传失败，请稍后再试')
    }
    return res.data
  }

  const createPost = async (slug: string, data: { title: string; content: string; coverImage?: string; images?: string[] }) => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再发帖')

    const res = await $fetch<ApiResponse<CommunityPostDto>>(api(`/communities/${slug}/posts`), {
      method: 'POST',
      headers: authHeaders(),
      body: { ...data, ...currentPublicProfile() },
    })
    if (res?.success === false || !res?.data) {
      throw new Error(res?.message || '发帖失败，请稍后再试')
    }
    return res.data
  }

  const fetchPost = async (postId: number): Promise<CommunityPostDto | null> => {
    try {
      const res = await $fetch<ApiResponse<CommunityPostDto>>(api(`/communities/posts/${postId}`))
      return res?.data ?? null
    } catch (e) {
      console.warn('获取帖子详情失败:', (e as Error).message)
      return null
    }
  }

  const fetchMyPosts = async (page = 0, size = 20): Promise<PostPageResponse> => {
    try {
      const apiReady = await userStore.ensureBackendToken()
      if (!apiReady) throw new Error('需要登录')
      const res = await $fetch<ApiResponse<PostPageResponse>>(api('/communities/posts/my'), {
        params: { page, size },
        headers: authHeaders(),
      })
      return res?.data ?? { items: [], total: 0, page: 0, size: 20 }
    } catch (e) {
      console.warn('获取我的帖子失败:', (e as Error).message)
      return { items: [], total: 0, page: 0, size: 20 }
    }
  }

  const deletePost = async (postId: number) => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再操作')

    const res = await $fetch<ApiResponse<string>>(api(`/communities/posts/${postId}`), {
      method: 'DELETE',
      headers: authHeaders(),
    })
    if (res?.success === false) {
      throw new Error(res?.message || '删除帖子失败')
    }
  }

  const likePost = async (postId: number) => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再点亮')

    const res = await $fetch<ApiResponse<CommunityPostDto>>(api(`/communities/posts/${postId}/like`), {
      method: 'POST',
      headers: authHeaders(),
    })
    if (res?.success === false || !res?.data) {
      throw new Error(res?.message || '点亮失败，请稍后再试')
    }
    return res.data
  }

  const fetchReplies = async (postId: number, page = 0, size = 50): Promise<CommunityReplyDto[]> => {
    try {
      const res = await $fetch<ApiResponse<CommunityReplyDto[]>>(api(`/communities/posts/${postId}/replies`), {
        params: { page, size },
      })
      return res?.data ?? []
    } catch (e) {
      console.warn('获取帖子回复失败:', (e as Error).message)
      return []
    }
  }

  const createReply = async (postId: number, content: string) => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再回复')

    const res = await $fetch<ApiResponse<CommunityReplyDto>>(api(`/communities/posts/${postId}/replies`), {
      method: 'POST',
      headers: authHeaders(),
      body: { content, ...currentPublicProfile() },
    })
    if (res?.success === false || !res?.data) {
      throw new Error(res?.message || '回复失败，请稍后再试')
    }
    return res.data
  }

  const deleteReply = async (replyId: number) => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再操作')

    const res = await $fetch<ApiResponse<string>>(api(`/communities/replies/${replyId}`), {
      method: 'DELETE',
      headers: authHeaders(),
    })
    if (res?.success === false) {
      throw new Error(res?.message || '删除回复失败')
    }
  }

  const likeReply = async (replyId: number) => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('登录状态需要刷新，请重新登录后再点亮')

    const res = await $fetch<ApiResponse<CommunityReplyDto>>(api(`/communities/replies/${replyId}/like`), {
      method: 'POST',
      headers: authHeaders(),
    })
    if (res?.success === false || !res?.data) {
      throw new Error(res?.message || '点亮失败，请稍后再试')
    }
    return res.data
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
    fetchCommunities,
    fetchCommunity,
    fetchPosts,
    fetchPostsByAnime,
    uploadImages,
    createPost,
    fetchPost,
    fetchMyPosts,
    deletePost,
    likePost,
    fetchReplies,
    createReply,
    deleteReply,
    likeReply,
  }
}
