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

export function useAgentVote() {
  const { request } = useApi()
  const empty = (): AgentVoteStatusResponse => ({ hasVoted: false, votedCandidateId: null, candidates: [] })

  const fetchCandidates = async (): Promise<AgentVoteStatusResponse> => {
    try { return await request<AgentVoteStatusResponse>('/agent/vote/candidates') }
    catch (error) { console.warn('获取候选角色失败:', (error as Error).message); return empty() }
  }

  const voteForCandidate = (candidateId: number) => request<AgentVoteStatusResponse>('/agent/vote', {
    method: 'POST', body: { candidateId }, auth: true,
  })

  const fetchVoteStatus = async (): Promise<AgentVoteStatusResponse> => {
    try { return await request<AgentVoteStatusResponse>('/agent/vote/status', { auth: true }) }
    catch { return empty() }
  }

  return { fetchCandidates, voteForCandidate, fetchVoteStatus }
}
