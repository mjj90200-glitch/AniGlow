import { defineStore } from 'pinia'

export type DailyTaskId = 'view-comments' | 'publish-comment' | 'browse-five-minutes'

export interface DailyTask {
  id: DailyTaskId
  title: string
  description: string
  reward: number
  accent: 'firefly' | 'sakura' | 'sky'
  target?: number
}

export interface AnimeVoteEntry {
  id: number
  title: string
  cover: string
  votes: number
}

interface DailyState {
  date: string
  completedTaskIds: DailyTaskId[]
  tickets: number
  browseSeconds: number
}

const DAILY_STATE_KEY = 'aniglow_daily_state'

const taskDefinitions: DailyTask[] = [
  {
    id: 'view-comments',
    title: '去评论区偷听萤火',
    description: '打开任意一部番剧详情，看看同好们正在怎样理解它。',
    reward: 1,
    accent: 'sky',
  },
  {
    id: 'publish-comment',
    title: '写下一句今日感想',
    description: '在任意作品下发布一条评论，把你的喜欢认真留住。',
    reward: 1,
    accent: 'sakura',
  },
  {
    id: 'browse-five-minutes',
    title: '番舍散步 5 分钟',
    description: '在网站里慢慢逛满 5 分钟，让系统给你一枚投票萤火。',
    reward: 1,
    accent: 'firefly',
    target: 300,
  },
]

function todayKey() {
  return new Date().toISOString().slice(0, 10)
}

function defaultDailyState(): DailyState {
  return {
    date: todayKey(),
    completedTaskIds: [],
    tickets: 0,
    browseSeconds: 0,
  }
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

function writeJson<T>(key: string, value: T) {
  if (import.meta.server) return
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // localStorage may be unavailable in private modes.
  }
}

export const useDailyStore = defineStore('daily', () => {
  const { request } = useApi()
  const userStore = useUserStore()
  const state = ref<DailyState>(defaultDailyState())
  const votes = ref<Record<number, AnimeVoteEntry>>({})
  const hydrated = ref(false)
  let browseTimer: ReturnType<typeof setInterval> | undefined

  const tasks = computed(() => taskDefinitions)
  const completedCount = computed(() => state.value.completedTaskIds.length)
  const totalReward = computed(() => taskDefinitions.reduce((sum, task) => sum + task.reward, 0))
  const browseProgress = computed(() => Math.min(1, state.value.browseSeconds / 300))
  const voteLeaderboard = computed(() => Object.values(votes.value).sort((a, b) => b.votes - a.votes))

  function hydrate() {
    if (hydrated.value || import.meta.server) return

    const savedState = readJson<DailyState>(DAILY_STATE_KEY, defaultDailyState())
    state.value = savedState.date === todayKey() ? savedState : defaultDailyState()
    hydrated.value = true
  }

  function persist() {
    if (import.meta.server) return
    writeJson(DAILY_STATE_KEY, state.value)
  }

  async function refreshVoteLeaderboard(limit = 30) {
    try {
      const result = await request<{ content?: any[] }>('/anime/firefly-ranking', { params: { page: 0, size: limit } })
      const entries = result?.content ?? []
      votes.value = entries.reduce<Record<number, AnimeVoteEntry>>((acc, anime) => {
        acc[anime.id] = {
          id: anime.id,
          title: anime.titleJapanese || anime.title || '番剧',
          cover: anime.coverImage,
          votes: anime.fireflyVoteCount ?? 0,
        }
        return acc
      }, {})
    } catch {
      // Keep current in-memory ranking if the network blips.
    }
  }

  function isTaskCompleted(id: DailyTaskId) {
    return state.value.completedTaskIds.includes(id)
  }

  function completeTask(id: DailyTaskId) {
    hydrate()
    if (isTaskCompleted(id)) return false

    const task = taskDefinitions.find(item => item.id === id)
    if (!task) return false

    state.value.completedTaskIds = [...state.value.completedTaskIds, id]
    state.value.tickets += task.reward
    persist()
    return true
  }

  function tickBrowseSeconds(seconds = 1) {
    hydrate()
    if (isTaskCompleted('browse-five-minutes')) return

    state.value.browseSeconds = Math.min(300, state.value.browseSeconds + seconds)
    if (state.value.browseSeconds >= 300) {
      completeTask('browse-five-minutes')
    } else {
      persist()
    }
  }

  function startBrowsingSession() {
    hydrate()
    if (browseTimer || import.meta.server) return
    browseTimer = setInterval(() => tickBrowseSeconds(1), 1000)
  }

  function stopBrowsingSession() {
    if (!browseTimer) return
    clearInterval(browseTimer)
    browseTimer = undefined
  }

  async function voteForAnime(anime: { id: number; title: string; cover: string }) {
    hydrate()
    if (state.value.tickets <= 0) return false
    const result = await request<{ fireflyVoteCount?: number }>('/votes', {
      method: 'POST', auth: true, body: { animeId: anime.id },
    })
    const nextVoteCount = result?.fireflyVoteCount ?? ((votes.value[anime.id]?.votes ?? 0) + 1)

    votes.value = {
      ...votes.value,
      [anime.id]: {
        id: anime.id,
        title: anime.title,
        cover: anime.cover,
        votes: nextVoteCount,
      },
    }
    state.value.tickets -= 1
    persist()
    return true
  }

  function getAnimeVotes(id: number) {
    return votes.value[id]?.votes ?? 0
  }

  return {
    state,
    tasks,
    votes,
    hydrated,
    completedCount,
    totalReward,
    browseProgress,
    voteLeaderboard,
    hydrate,
    persist,
    refreshVoteLeaderboard,
    isTaskCompleted,
    completeTask,
    tickBrowseSeconds,
    startBrowsingSession,
    stopBrowsingSession,
    voteForAnime,
    getAnimeVotes,
  }
})
