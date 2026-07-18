<template>
  <Transition enter-active-class="transition duration-200" enter-from-class="opacity-0" leave-active-class="transition duration-150" leave-to-class="opacity-0">
    <div
      v-if="visible"
      class="fixed inset-0 z-[100] flex items-center justify-center px-4 py-8 bg-black/20 backdrop-blur-md"
      @click.self="$emit('close')"
    >
      <div class="glass-card-cream rounded-4xl p-6 sm:p-8 w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <!-- 头部 -->
        <div class="text-center mb-6">
          <h2 class="text-xl sm:text-2xl font-black text-gray-900 mb-1">
            下周新增哪个 AIGC 角色？
          </h2>
          <p class="text-sm text-gray-500">
            每周四根据投票结果更新，每人每周一票
          </p>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="flex justify-center py-12">
          <span class="w-8 h-8 border-2 border-firefly/30 border-t-firefly rounded-full animate-spin" />
        </div>

        <!-- 候选角色卡片 -->
        <div v-else-if="candidates.length" class="grid grid-cols-2 gap-4 mb-6">
          <div
            v-for="c in candidates"
            :key="c.id"
            class="rounded-2xl p-4 text-center transition-all duration-300"
            :class="status.hasVoted && status.votedCandidateId === c.id
              ? 'bg-firefly/10 ring-2 ring-firefly/30'
              : 'bg-white/60 hover:bg-white/80 hover:shadow-md'"
          >
            <!-- 头像 -->
            <div class="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl overflow-hidden bg-cream-100 mx-auto mb-3">
              <img
                v-if="c.avatarUrl"
                :src="c.avatarUrl"
                :alt="c.displayName"
                class="w-full h-full object-cover"
                @error="hideBrokenAvatar"
              />
              <div v-if="!c.avatarUrl" class="w-full h-full flex items-center justify-center text-2xl font-black text-firefly/30">
                {{ c.displayName.slice(0, 1) }}
              </div>
            </div>

            <!-- 名字 -->
            <h3 class="text-sm sm:text-base font-extrabold text-gray-800 mb-0.5">
              {{ c.displayName }}
            </h3>
            <p class="text-xs text-gray-400 mb-3">{{ c.sourceTitle }}</p>

            <!-- 票数 -->
            <div class="flex items-center justify-center gap-1 text-xs font-bold text-firefly-700 mb-3">
              <Heart class="w-3.5 h-3.5 fill-firefly text-firefly" />
              {{ c.voteCount || 0 }} 票
            </div>

            <!-- 投票按钮 -->
            <button
              v-if="!status.hasVoted"
              class="btn-glow w-full py-2 text-sm gap-1.5"
              :class="voting ? 'opacity-50 pointer-events-none' : ''"
              @click.stop="handleVote(c.id)"
            >
              {{ voting && votingId === c.id ? '投票中...' : '投她一票' }}
            </button>
            <div
              v-else-if="status.votedCandidateId === c.id"
              class="rounded-full bg-firefly/15 px-3 py-1.5 text-xs font-extrabold text-firefly-700"
            >
              已投票
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <p class="text-gray-400 text-sm">本周还没有候选角色，敬请期待</p>
        </div>

        <!-- 底部提示 -->
        <div v-if="status.hasVoted" class="text-center text-xs text-gray-400 bg-white/50 rounded-full py-2 px-4">
          你本周已投票
          <template v-if="votedCandidate">
            给「<strong class="text-firefly-700">{{ votedCandidate.displayName }}</strong>」
          </template>
          ，感谢参与！
        </div>

        <!-- 关闭 -->
        <button
          class="absolute top-4 right-4 rounded-full bg-white/70 p-2 text-gray-500 hover:text-gray-800 transition"
          @click="$emit('close')"
        >
          <X class="w-5 h-5" />
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { Heart, X } from 'lucide-vue-next'
import type { AgentVoteCandidateDto, AgentVoteStatusResponse } from '~/composables/useAgent'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const { fetchCandidates, voteForCandidate } = useAgentVote()
const { requireAuth } = useAuthModal()

const loading = ref(true)
const voting = ref(false)
const votingId = ref<number | null>(null)
const status = ref<AgentVoteStatusResponse>({ hasVoted: false, votedCandidateId: null, candidates: [] })

const candidates = computed(() => status.value.candidates ?? [])
const votedCandidate = computed(() =>
  status.value.votedCandidateId
    ? candidates.value.find(c => c.id === status.value.votedCandidateId) ?? null
    : null
)

async function loadData() {
  loading.value = true
  status.value = await fetchCandidates()
  loading.value = false
}

async function handleVote(candidateId: number) {
  requireAuth(async () => {
    if (voting.value || status.value.hasVoted) return
    voting.value = true
    votingId.value = candidateId
    try {
      status.value = await voteForCandidate(candidateId)
    } catch (e: any) {
      // silently handle
    } finally {
      voting.value = false
      votingId.value = null
    }
  })
}

function hideBrokenAvatar(event: Event) {
  const image = event.target as HTMLImageElement | null
  if (image) image.style.display = 'none'
}

watch(() => props.visible, (v) => {
  if (v) loadData()
})
</script>
