export interface AgentVoteCandidateDto {
  id: number
  code: string
  displayName: string
  sourceTitle: string
  avatarUrl: string
  voteCount: number
  weekKey: string
}

export interface AgentVoteStatusResponse {
  hasVoted: boolean
  votedCandidateId: number | null
  candidates: AgentVoteCandidateDto[]
}

interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
}

export function useAgentVote() {
  const config = useRuntimeConfig()
  const userStore = useUserStore()
  const base = config.public.apiBase || '/api'
  const api = (path: string) => `${base}${path}`
  const authHeaders = () =>
    userStore.backendToken
      ? { Authorization: `Bearer ${userStore.backendToken}` }
      : undefined

  const fetchCandidates = async (): Promise<AgentVoteStatusResponse> => {
    try {
      const headers = authHeaders()
      const res = await $fetch<ApiResponse<AgentVoteStatusResponse>>(
        api('/agent/vote/candidates'),
        { headers }
      )
      return res?.data ?? { hasVoted: false, votedCandidateId: null, candidates: [] }
    } catch (e) {
      console.warn('获取候选角色失败:', (e as Error).message)
      return { hasVoted: false, votedCandidateId: null, candidates: [] }
    }
  }

  const voteForCandidate = async (candidateId: number): Promise<AgentVoteStatusResponse> => {
    const apiReady = await userStore.ensureBackendToken()
    if (!apiReady) throw new Error('请先登录后再投票')

    const res = await $fetch<ApiResponse<AgentVoteStatusResponse>>(
      api('/agent/vote'),
      { method: 'POST', headers: authHeaders(), body: { candidateId } }
    )
    if (res?.success === false) {
      throw new Error(res?.message || '投票失败，请稍后再试')
    }
    return res?.data ?? { hasVoted: true, votedCandidateId: candidateId, candidates: [] }
  }

  const fetchVoteStatus = async (): Promise<AgentVoteStatusResponse> => {
    try {
      const apiReady = await userStore.ensureBackendToken()
      if (!apiReady) return { hasVoted: false, votedCandidateId: null, candidates: [] }

      const res = await $fetch<ApiResponse<AgentVoteStatusResponse>>(
        api('/agent/vote/status'),
        { headers: authHeaders() }
      )
      return res?.data ?? { hasVoted: false, votedCandidateId: null, candidates: [] }
    } catch {
      return { hasVoted: false, votedCandidateId: null, candidates: [] }
    }
  }

  return { fetchCandidates, voteForCandidate, fetchVoteStatus }
}
