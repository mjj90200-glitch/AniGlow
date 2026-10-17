<template>
  <div ref="commentPanelRef" class="glass-card rounded-4xl p-6 sm:p-8">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-5 mb-6">
      <div>
        <p class="text-xs font-semibold tracking-widest uppercase mb-1" style="color: #00AA44;">
          Community Rating
        </p>
        <h2 class="text-2xl font-extrabold text-gray-800">评分与讨论</h2>
      </div>

      <div class="flex items-center gap-1.5">
        <button
          v-for="star in 5"
          :key="star"
          class="p-1 rounded-full transition-all duration-200 hover:scale-110"
          :aria-label="`选择 ${star} 星`"
          @click="selectRating(star)"
          @mouseenter="hoverRating = star"
          @mouseleave="hoverRating = 0"
        >
          <Star
            class="w-8 h-8 transition-all"
            :class="[
              (hoverRating || userRating) >= star
                ? 'text-[#FFD54F] fill-[#FFD54F] drop-shadow'
                : 'text-gray-200'
            ]"
          />
        </button>
      </div>
    </div>

    <div class="grid sm:grid-cols-5 gap-2 mb-6">
      <button
        v-for="item in ratingTabs"
        :key="item.star"
        class="rounded-3xl px-4 py-3 text-left transition-all duration-300"
        :class="[
          selectedRating === item.star
            ? 'bg-firefly/15 text-firefly-700 shadow-glow-sm'
            : 'bg-white/55 text-gray-500 hover:bg-white/80'
        ]"
        @click="selectedRating = item.star"
      >
        <div class="flex items-center gap-1 mb-1">
          <Star
            v-for="i in item.star"
            :key="i"
            class="w-3.5 h-3.5 text-[#FFD54F] fill-[#FFD54F]"
          />
        </div>
        <p class="text-xs font-bold">{{ item.label }}</p>
        <p class="text-[11px] text-gray-400">{{ item.count }} 条评论</p>
      </button>
    </div>

    <div v-if="topReview" class="mb-5 rounded-3xl border border-firefly/20 bg-firefly/10 p-4">
      <div class="flex items-center gap-2 mb-2">
        <Flame class="w-4 h-4 text-firefly-700" />
        <span class="text-xs font-extrabold text-firefly-700">当前点赞最高</span>
      </div>
      <p class="text-sm text-gray-700 leading-6">{{ topReview.content }}</p>
      <p class="text-xs text-gray-400 mt-2">
        {{ topReview.user }} · {{ topReview.likes }} 个同好点亮
      </p>
    </div>

    <div class="rounded-3xl bg-white/55 backdrop-blur-md p-4 mb-6">
      <div class="flex items-center gap-2 mb-3">
        <MessageCircle class="w-4 h-4 text-firefly" />
        <span class="text-sm font-extrabold text-gray-700">留下你的 {{ selectedRating }} 星理解</span>
      </div>
      <textarea
        v-model="draftComment"
        rows="3"
        class="w-full resize-none rounded-3xl border border-white/70 bg-white/70 px-4 py-3 text-sm
               text-gray-700 outline-none transition focus:border-firefly focus:ring-4 focus:ring-firefly/10"
        placeholder="写下这一星级下，你对角色、剧情、演出或情绪的理解..."
      />
      <p v-if="submitError" class="mt-2 text-xs font-bold text-sakura-dark">
        {{ submitError }}
      </p>
      <div class="flex justify-end mt-3">
        <button
          class="btn-glow px-5 py-2.5"
          :disabled="!draftComment.trim() || submitting"
          :class="(!draftComment.trim() || submitting) ? 'opacity-40 pointer-events-none' : ''"
          @click="submitReview"
        >
          {{ submitting ? '发布中...' : '发布讨论' }}
        </button>
      </div>
    </div>

    <div class="space-y-3">
      <article
        v-for="review in selectedReviews"
        :key="review.id"
        class="rounded-3xl bg-white/60 backdrop-blur-md p-4 border border-white/50"
      >
        <div class="flex items-start gap-3">
          <div
            class="w-10 h-10 rounded-full flex items-center justify-center shrink-0 text-sm font-extrabold text-white"
            :style="{ background: review.avatarGradient }"
          >
            <img
              v-if="review.avatar"
              :src="review.avatar"
              :alt="review.user"
              class="w-full h-full rounded-full object-cover"
            />
            <span v-else>{{ review.user.slice(0, 1) }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center justify-between gap-2 mb-1">
              <div class="flex items-center gap-2">
                <span class="font-extrabold text-gray-800">{{ review.user }}</span>
                <span
                  v-if="review.isLocal || review.isFresh"
                  class="rounded-full bg-firefly/10 px-2 py-0.5 text-[10px] font-bold text-firefly-700"
                >
                  刚刚参与
                </span>
                <span class="text-xs text-gray-400">{{ review.date }}</span>
              </div>
              <div class="flex items-center gap-0.5">
                <Star
                  v-for="i in review.rating"
                  :key="i"
                  class="w-3.5 h-3.5 text-[#FFD54F] fill-[#FFD54F]"
                />
              </div>
            </div>
            <p class="text-sm text-gray-600 leading-6">{{ review.content }}</p>
            <button
              class="mt-3 inline-flex items-center gap-1.5 rounded-full bg-white/65 px-3 py-1.5
                     text-xs font-bold text-gray-500 transition hover:text-sakura-dark hover:bg-sakura/15"
              @click="toggleLike(review.id)"
            >
              <Heart
                class="w-3.5 h-3.5"
                :class="likedReviewIds.includes(review.id) ? 'fill-sakura text-sakura' : ''"
              />
              {{ review.likes }} 人点亮
            </button>

            <button
              class="ml-2 mt-3 inline-flex items-center gap-1.5 rounded-full bg-firefly/10 px-3 py-1.5
                     text-xs font-bold text-firefly-700 transition hover:bg-firefly/20"
              @click="toggleReplyBox(review.id)"
            >
              <MessageCircle class="w-3.5 h-3.5" />
              回复 {{ review.replies.length ? review.replies.length : '' }}
            </button>

            <button
              v-if="isOwnReview(review.userId)"
              class="ml-2 mt-3 inline-flex items-center gap-1.5 rounded-full bg-gray-100 px-3 py-1.5
                     text-xs font-bold text-gray-400 transition hover:bg-sakura/15 hover:text-sakura-dark"
              @click="confirmDeleteReview(review)"
            >
              <Trash2 class="w-3.5 h-3.5" />
              删除
            </button>

            <div
              v-if="review.replies.length || openReplyReviewId === review.id"
              class="mt-4 rounded-3xl border border-white/60 bg-white/45 p-3 sm:p-4"
            >
              <div v-if="review.replies.length" class="space-y-3">
                <div
                  v-for="reply in review.replies"
                  :key="reply.id"
                  class="flex items-start gap-2.5 rounded-2xl bg-white/50 px-3 py-2.5"
                >
                  <div
                    class="h-8 w-8 shrink-0 overflow-hidden rounded-full text-xs font-extrabold text-white flex items-center justify-center"
                    :style="{ background: reply.avatarGradient }"
                  >
                    <img
                      v-if="reply.avatar"
                      :src="reply.avatar"
                      :alt="reply.user"
                      class="h-full w-full object-cover"
                    />
                    <span v-else>{{ reply.user.slice(0, 1) }}</span>
                  </div>
                  <div class="min-w-0 flex-1">
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="text-xs font-extrabold text-gray-800">{{ reply.user }}</span>
                      <span
                        v-if="reply.isFresh"
                        class="rounded-full bg-firefly/10 px-2 py-0.5 text-[10px] font-bold text-firefly-700"
                      >
                        刚刚回复
                      </span>
                      <span class="text-[11px] text-gray-400">{{ reply.date }}</span>
                    </div>
                    <p class="mt-1 text-xs sm:text-sm leading-6 text-gray-600">{{ reply.content }}</p>
                    <button
                      class="mt-1.5 inline-flex items-center gap-1 rounded-full px-2 py-1 text-[11px] font-bold
                             text-gray-400 transition hover:bg-sakura/10 hover:text-sakura-dark"
                      @click="toggleReplyLike(review.id, reply.id)"
                    >
                      <Heart
                        class="h-3 w-3"
                        :class="likedReplyIds.includes(reply.id) ? 'fill-sakura text-sakura' : ''"
                      />
                      {{ reply.likes }}
                    </button>
                    <button
                      v-if="isOwnReview(reply.userId)"
                      class="mt-1.5 ml-1 inline-flex items-center gap-1 rounded-full px-2 py-1 text-[11px] font-bold
                             text-gray-400 transition hover:bg-sakura/10 hover:text-sakura-dark"
                      @click="confirmDeleteReply(review.id, reply)"
                    >
                      <Trash2 class="h-3 w-3" />
                    </button>
                  </div>
                </div>
              </div>

              <div v-if="openReplyReviewId === review.id" class="mt-3 rounded-2xl bg-white/55 p-3">
                <textarea
                  v-model="replyDrafts[review.id]"
                  rows="2"
                  class="w-full resize-none rounded-2xl border border-white/70 bg-white/70 px-3 py-2 text-sm
                         text-gray-700 outline-none transition focus:border-firefly focus:ring-4 focus:ring-firefly/10"
                  :placeholder="`回复 ${review.user}，一起把这段理解接下去...`"
                />
                <p v-if="replyErrors[review.id]" class="mt-1 text-xs font-bold text-sakura-dark">
                  {{ replyErrors[review.id] }}
                </p>
                <div class="mt-2 flex justify-end gap-2">
                  <button
                    class="rounded-full bg-white/70 px-3 py-1.5 text-xs font-bold text-gray-500 transition hover:bg-white"
                    @click="toggleReplyBox(review.id)"
                  >
                    收起
                  </button>
                  <button
                    class="rounded-full bg-firefly px-4 py-1.5 text-xs font-extrabold text-white shadow-glow-sm transition hover:scale-105"
                    :disabled="!replyDrafts[review.id]?.trim() || submittingReplyIds.includes(review.id)"
                    :class="(!replyDrafts[review.id]?.trim() || submittingReplyIds.includes(review.id)) ? 'opacity-40 pointer-events-none' : ''"
                    @click="submitReply(review.id)"
                  >
                    {{ submittingReplyIds.includes(review.id) ? '回复中...' : '发布回复' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <div
        v-if="deleteTarget"
        class="fixed inset-0 z-[100] flex items-center justify-center px-4 bg-black/20 backdrop-blur-md"
        @click.self="deleteTarget = null"
      >
        <div class="glass-card rounded-3xl p-6 w-full max-w-sm shadow-glow">
          <p class="text-lg font-black text-gray-900 mb-2">确认删除</p>
          <p class="text-sm text-gray-500 mb-5">
            删除后无法恢复，确定要删除{{ deleteTarget.type === 'review' ? '这条评论' : '这条回复' }}吗？
          </p>
          <div class="flex justify-end gap-3">
            <button class="btn-glass px-5 py-2.5" @click="deleteTarget = null">取消</button>
            <button
              class="bg-sakura text-white font-extrabold px-5 py-2.5 rounded-full shadow-glow-sm transition hover:bg-sakura-dark"
              :disabled="deleting"
              :class="deleting ? 'opacity-40 pointer-events-none' : ''"
              @click="doDelete"
            >
              {{ deleting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { Flame, Heart, MessageCircle, Star, Trash2 } from 'lucide-vue-next'
import type { AnimeDto } from '~/types/anime'

const props = defineProps<{
  animeId: number
  anime: AnimeDto
}>()

const emit = defineEmits<{
  animeUpdated: [anime: AnimeDto]
}>()

const {
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
} = useAnimeReviews({
  animeId: toRef(props, 'animeId'),
  getAnime: () => props.anime,
  updateAnime: anime => emit('animeUpdated', anime),
})
</script>
