<template>
  <div class="min-h-screen pb-20">
    <div v-if="pending" class="max-w-7xl mx-auto px-6 pt-32">
      <div class="glass-card rounded-4xl p-10 animate-pulse">
        <div class="h-8 w-52 rounded-full bg-cream-200 mb-6" />
        <div class="grid md:grid-cols-[220px,1fr] gap-8">
          <div class="aspect-[3/4] rounded-3xl bg-cream-200" />
          <div class="space-y-4">
            <div class="h-10 w-2/3 rounded-full bg-cream-200" />
            <div class="h-4 w-full rounded-full bg-cream-200" />
            <div class="h-4 w-5/6 rounded-full bg-cream-200" />
            <div class="h-12 w-44 rounded-full bg-cream-200 mt-8" />
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="!anime" class="max-w-3xl mx-auto px-6 pt-36 text-center">
      <div class="glass-card-cream rounded-4xl p-10">
        <Sparkles class="w-10 h-10 mx-auto mb-4 text-firefly" />
        <h1 class="text-2xl font-extrabold text-gray-800 mb-3">没有找到这部作品</h1>
        <p class="text-gray-500 mb-6">可能是后端服务暂时不可用，或者这部番剧还没有同步进番舍。</p>
        <NuxtLink to="/anime" class="btn-glow">回到番剧列表</NuxtLink>
      </div>
    </div>

    <template v-else>
      <section class="relative overflow-hidden pt-28 pb-10">
        <div class="absolute inset-x-0 top-0 h-[520px] overflow-hidden">
          <img
            :src="anime.coverImage"
            :alt="displayTitle(anime)"
            class="w-full h-full object-cover blur-sm scale-105 opacity-60"
          />
          <div class="absolute inset-0 bg-gradient-to-b from-cream/35 via-cream/80 to-cream" />
        </div>

        <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <NuxtLink
            to="/anime"
            class="inline-flex items-center gap-2 px-4 py-2 mb-6 rounded-full bg-white/70 backdrop-blur-md
                   text-sm font-bold text-gray-600 shadow-soft hover:text-firefly-700 transition-colors"
          >
            <ArrowLeft class="w-4 h-4" />
            返回番剧
          </NuxtLink>

          <div class="grid lg:grid-cols-[280px,1fr] gap-8 lg:gap-10 items-end">
            <div class="glass-card rounded-[2rem] p-3">
              <img
                :src="anime.coverImage"
                :alt="displayTitle(anime)"
                class="w-full aspect-[3/4] object-cover rounded-[1.45rem] shadow-ambient"
              />
            </div>

            <div class="glass-card-cream rounded-4xl p-7 sm:p-9">
              <div class="flex flex-wrap items-center gap-2 mb-4">
                <span
                  v-for="tag in mappedGenres"
                  :key="tag.label"
                  class="tag"
                  :class="tagClass(tag.type)"
                >
                  {{ tag.label }}
                </span>
                <span v-if="anime.year" class="tag tag-new">{{ anime.year }}</span>
                <span v-if="anime.status" class="tag tag-healing">{{ tStatus(anime.status) }}</span>
              </div>

              <h1 class="text-3xl sm:text-5xl font-extrabold text-gray-800 leading-tight mb-3">
                {{ displayTitle(anime) }}
              </h1>
              <p v-if="anime.titleEnglish" class="text-sm text-gray-400 font-semibold mb-5">
                {{ anime.titleEnglish }}
              </p>
              <p class="text-gray-600 leading-relaxed max-w-3xl line-clamp-3 mb-6">
                {{ synopsis }}
              </p>

              <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div class="rounded-3xl bg-white/60 backdrop-blur-md px-4 py-3">
                  <p class="text-xs text-gray-400">用户评分</p>
                  <p class="text-2xl font-extrabold text-gray-800">
                    {{ scoreText }}<span class="text-sm text-gray-400"> / 10</span>
                  </p>
                </div>
                <div class="rounded-3xl bg-white/60 backdrop-blur-md px-4 py-3">
                  <p class="text-xs text-gray-400">站内共鸣</p>
                  <p class="text-2xl font-extrabold text-gray-800">{{ anime.communityRatingCount || 0 }}</p>
                </div>
                <div class="rounded-3xl bg-white/60 backdrop-blur-md px-4 py-3">
                  <p class="text-xs text-gray-400">集数</p>
                  <p class="text-2xl font-extrabold text-gray-800">{{ anime.episodes || '?' }}</p>
                </div>
                <div class="rounded-3xl bg-white/60 backdrop-blur-md px-4 py-3">
                  <p class="text-xs text-gray-400">类型</p>
                  <p class="text-lg font-extrabold text-gray-800">{{ tType(anime.type) || '未知' }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div class="grid lg:grid-cols-[1fr,340px] gap-6 lg:gap-8">
          <div class="space-y-6">
            <div class="glass-card rounded-4xl p-6 sm:p-8">
              <div class="flex items-center justify-between gap-4 mb-5">
                <div>
                  <p class="text-xs font-semibold tracking-widest uppercase mb-1" style="color: #00AA44;">
                    Story & Tags
                  </p>
                  <h2 class="text-2xl font-extrabold text-gray-800">作品介绍</h2>
                </div>
                <BookOpen class="w-7 h-7 text-firefly" />
              </div>
              <p class="text-gray-600 leading-8">{{ synopsis }}</p>
            </div>

            <div
              v-if="aigcCharacters.length"
              class="glass-card rounded-4xl p-6 sm:p-8 overflow-hidden relative"
            >
              <div
                class="absolute -right-10 -top-10 h-36 w-36 rounded-full bg-firefly/10 blur-3xl pointer-events-none"
              />
              <div class="relative flex items-center justify-between gap-4 mb-5">
                <div>
                  <p class="text-xs font-semibold tracking-widest uppercase mb-1" style="color: #00AA44;">
                    AIGC Cast
                  </p>
                  <h2 class="text-2xl font-extrabold text-gray-800">本作 AIGC 角色</h2>
                  <p class="mt-1 text-sm text-gray-500">
                    像参演角色一样，把能对话的角色收在这里，方便你看完简介直接去聊。
                  </p>
                </div>
                <Sparkles class="w-7 h-7 text-firefly" />
              </div>

              <div class="relative grid sm:grid-cols-2 gap-3">
                <NuxtLink
                  v-for="character in aigcCharacters"
                  :key="character.code"
                  :to="{ path: '/agent', query: { role: character.code } }"
                  class="group rounded-3xl border border-white/60 bg-white/58 p-4 backdrop-blur-md
                         transition-all duration-300 hover:-translate-y-1 hover:bg-white/78 hover:shadow-glow-sm"
                >
                  <div class="flex items-center gap-3">
                    <div class="relative h-14 w-14 shrink-0 overflow-hidden rounded-2xl bg-firefly/10 ring-2 ring-white/70">
                      <img
                        v-if="character.avatarUrl"
                        :src="character.avatarUrl"
                        :alt="character.displayName"
                        class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
                      />
                      <div v-else class="flex h-full w-full items-center justify-center">
                        <Bot class="h-6 w-6 text-firefly-600" />
                      </div>
                      <span class="absolute bottom-1 right-1 h-2.5 w-2.5 rounded-full bg-firefly ring-2 ring-white" />
                    </div>
                    <div class="min-w-0 flex-1">
                      <div class="flex items-center gap-2">
                        <h3 class="truncate text-base font-extrabold text-gray-800 group-hover:text-firefly-700">
                          {{ character.displayName }}
                        </h3>
                        <span class="rounded-full bg-firefly/10 px-2 py-0.5 text-[10px] font-black text-firefly-700">
                          AIGC
                        </span>
                      </div>
                      <p class="mt-1 truncate text-xs font-semibold text-gray-400">
                        来自《{{ character.sourceTitle || displayTitle(anime) }}》
                      </p>
                      <p class="mt-2 text-xs font-bold text-gray-500">
                        点击去和 TA 聊聊
                      </p>
                    </div>
                  </div>
                </NuxtLink>
              </div>
            </div>

            <div class="glass-card rounded-4xl p-6 sm:p-8">
              <div class="mb-5 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p class="mb-1 text-xs font-semibold uppercase tracking-widest text-firefly-600">
                    Community Stories
                  </p>
                  <h2 class="text-2xl font-extrabold text-gray-800">相关社区讨论</h2>
                  <p class="mt-1 text-sm leading-6 text-gray-500">从作品介绍继续走进同好们正在聊的话题。</p>
                </div>
                <NuxtLink
                  :to="relatedCommunityPath"
                  class="inline-flex shrink-0 items-center gap-1.5 text-sm font-extrabold text-firefly-700 transition hover:text-firefly-600"
                >
                  去社区聊聊
                  <ArrowRight class="h-4 w-4" />
                </NuxtLink>
              </div>

              <div v-if="relatedCommunityPosts.length" class="grid gap-3 sm:grid-cols-2">
                <NuxtLink
                  v-for="post in relatedCommunityPosts"
                  :key="post.id"
                  :to="`/community/${post.communitySlug}/posts/${post.id}`"
                  class="group rounded-3xl border border-white/65 bg-white/55 p-4 backdrop-blur-md transition hover:-translate-y-0.5 hover:bg-white/80 hover:shadow-soft"
                >
                  <div class="mb-3 flex items-center gap-2">
                    <div class="h-8 w-8 overflow-hidden rounded-full bg-firefly/10">
                      <img v-if="post.avatarUrl" :src="post.avatarUrl" :alt="post.displayName || '番舍同好'" class="h-full w-full object-cover" />
                      <div v-else class="flex h-full w-full items-center justify-center text-xs font-black text-firefly-700">
                        {{ (post.displayName || '萤')[0] }}
                      </div>
                    </div>
                    <div class="min-w-0">
                      <p class="truncate text-xs font-extrabold text-gray-700">{{ post.displayName || '番舍同好' }}</p>
                      <p class="truncate text-[11px] font-semibold text-gray-400">{{ post.communityName }}</p>
                    </div>
                  </div>
                  <h3 class="line-clamp-2 text-sm font-extrabold leading-6 text-gray-800 transition group-hover:text-firefly-700">
                    {{ post.title }}
                  </h3>
                  <p class="mt-2 line-clamp-2 text-xs leading-5 text-gray-500">{{ post.content }}</p>
                  <div class="mt-3 flex items-center gap-4 text-[11px] font-bold text-gray-400">
                    <span class="inline-flex items-center gap-1"><MessageCircle class="h-3.5 w-3.5" />{{ post.replyCount }}</span>
                    <span class="inline-flex items-center gap-1"><Heart class="h-3.5 w-3.5" />{{ post.likeCount }}</span>
                  </div>
                </NuxtLink>
              </div>

              <div v-else class="rounded-3xl border border-dashed border-firefly/20 bg-firefly/5 px-5 py-6 text-center">
                <Sparkles class="mx-auto h-6 w-6 text-firefly" />
                <p class="mt-2 text-sm font-extrabold text-gray-700">这部番的第一场社区讨论，正在等你点亮</p>
                <p class="mt-1 text-xs text-gray-400">可以去番剧社区发帖，把想说的话变成同好的相遇。</p>
              </div>
            </div>

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
                    @click="selectRating(star)"
                    @mouseenter="hoverRating = star"
                    @mouseleave="hoverRating = 0"
                    :aria-label="`选择 ${star} 星`"
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
            </div>
          </div>

          <aside class="space-y-6">
            <div class="glass-card overflow-hidden rounded-4xl p-6">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <p class="text-[11px] font-black uppercase tracking-[0.2em] text-firefly-600">Firefly Vote</p>
                  <h3 class="mt-1 text-lg font-extrabold text-gray-800">为这部番投一票</h3>
                </div>
                <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-firefly/10 text-firefly-700">
                  <TicketCheck class="h-5 w-5" />
                </div>
              </div>

              <div class="my-5 grid grid-cols-2 gap-3">
                <div class="rounded-3xl bg-white/60 p-3">
                  <p class="text-[11px] font-bold text-gray-400">当前萤火票</p>
                  <p class="mt-1 text-2xl font-black text-gray-800">{{ currentFireflyVotes }}</p>
                </div>
                <div class="rounded-3xl bg-white/60 p-3">
                  <p class="text-[11px] font-bold text-gray-400">我的可用票</p>
                  <p class="mt-1 text-2xl font-black text-firefly-700">{{ dailyStore.state.tickets }}</p>
                </div>
              </div>

              <button
                v-if="dailyStore.state.tickets > 0"
                type="button"
                class="btn-glow w-full justify-center px-5 py-3"
                :disabled="voting"
                :class="voting ? 'pointer-events-none opacity-50' : ''"
                @click="voteForCurrentAnime"
              >
                {{ voting ? '萤火飞行中...' : '投出 1 张萤火票' }}
              </button>
              <NuxtLink v-else to="/daily" class="btn-glow flex w-full justify-center px-5 py-3">
                去做每日任务领票
              </NuxtLink>

              <p v-if="voteMessage" class="mt-3 text-center text-xs font-extrabold text-firefly-700">{{ voteMessage }}</p>
              <p v-if="voteError" class="mt-3 text-center text-xs font-bold text-sakura-dark">{{ voteError }}</p>
            </div>

            <div class="glass-card rounded-4xl p-6">
              <h3 class="text-lg font-extrabold text-gray-800 mb-4">基本信息</h3>
              <div class="space-y-3 text-sm">
                <div v-for="item in infoRows" :key="item.label" class="flex justify-between gap-4">
                  <span class="text-gray-400">{{ item.label }}</span>
                  <span class="text-gray-700 font-bold text-right">{{ item.value }}</span>
                </div>
              </div>
            </div>

            <div class="glass-card rounded-4xl p-6">
              <h3 class="text-lg font-extrabold text-gray-800 mb-4">同好也在看</h3>
              <div class="space-y-4">
                <NuxtLink
                  v-for="item in relatedAnime"
                  :key="item.id"
                  :to="`/anime/${item.id}`"
                  class="flex gap-3 group"
                >
                  <div class="w-16 h-20 rounded-2xl overflow-hidden shrink-0 shadow-soft">
                    <img
                      :src="item.coverImage"
                      :alt="displayTitle(item)"
                      class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                    />
                  </div>
                  <div class="min-w-0 flex-1">
                    <h4 class="text-sm font-extrabold text-gray-800 line-clamp-2 group-hover:text-firefly-700">
                      {{ displayTitle(item) }}
                    </h4>
                    <p class="text-xs text-gray-400 mt-1">{{ tType(item.type) || item.year || '番剧' }}</p>
                    <div class="flex items-center gap-1 mt-1">
                      <Star class="w-3.5 h-3.5 text-[#FFD54F] fill-[#FFD54F]" />
                      <span class="text-xs font-bold text-gray-600">
                        {{ formatScore(communityScore(item)) }}
                      </span>
                    </div>
                  </div>
                </NuxtLink>
              </div>
            </div>
          </aside>
        </div>
      </section>
    </template>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="deleteTarget" class="fixed inset-0 z-[100] flex items-center justify-center px-4 bg-black/20 backdrop-blur-md" @click.self="deleteTarget = null">
        <div class="glass-card rounded-3xl p-6 w-full max-w-sm shadow-glow">
          <p class="text-lg font-black text-gray-900 mb-2">确认删除</p>
          <p class="text-sm text-gray-500 mb-5">删除后无法恢复，确定要删除{{ deleteTarget.type === 'review' ? '这条评论' : '这条回复' }}吗？</p>
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
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Bot,
  Flame,
  Heart,
  MessageCircle,
  Sparkles,
  Star,
  TicketCheck,
  Trash2,
} from 'lucide-vue-next'
import type { AnimeDto } from '~/types/anime'
import type { CommunityPostDto } from '~/composables/useCommunity'
import {
  displaySynopsis,
  displayTitle,
  communityScore,
  formatNumber,
  mapGenres,
  tagClass,
  tStatus,
  tType,
} from '~/composables/useAnimeI18n'

interface Review {
  id: number
  userId?: number
  user: string
  avatar?: string
  rating: number
  content: string
  date: string
  likes: number
  avatarGradient: string
  isLocal?: boolean
  isFresh?: boolean
  createdAt?: number
  replies: ReviewReply[]
}

interface ReviewReply {
  id: number
  userId?: number
  user: string
  avatar?: string
  content: string
  date: string
  likes: number
  avatarGradient: string
  isFresh?: boolean
  createdAt?: number
}

interface AigcCharacter {
  code: string
  displayName: string
  sourceTitle?: string | null
  avatarUrl?: string | null
}

const route = useRoute()
const animeId = computed(() => Number(route.params.id))
const {
  fetchAnimeById,
  fetchTopRated,
  fetchReviews,
  submitRating,
  fetchRatingReplies,
  submitRatingReply,
  likeRatingReply,
  deleteRating,
  deleteRatingReply,
} = useAnime()
const dailyStore = useDailyStore()
const userStore = useUserStore()
const { fetchPostsByAnime } = useCommunity()
const config = useRuntimeConfig()
const apiBase = config.public.apiBase || '/api'
const REVIEW_STORAGE_PREFIX = 'aniglow_reviews_'
const REVIEW_LIKES_PREFIX = 'aniglow_review_likes_'

const { data: animeData, pending } = await useAsyncData(
  `anime-detail-${animeId.value}`,
  () => fetchAnimeById(animeId.value),
  { server: true, watch: [animeId] }
)

const { data: relatedData } = await useAsyncData(
  'anime-related-top',
  () => fetchTopRated(0, 8),
  { server: true }
)

const { data: aigcCharactersData } = await useAsyncData(
  `anime-aigc-characters-${animeId.value}`,
  async () => {
    if (!animeId.value) return []
    try {
      const res = await $fetch<{ data?: AigcCharacter[] }>(`${apiBase}/agent/characters/anime/${animeId.value}`)
      return res?.data ?? []
    } catch {
      return []
    }
  },
  { server: true, watch: [animeId] }
)

const { data: relatedCommunityData } = await useAsyncData(
  `anime-community-posts-${animeId.value}`,
  () => fetchPostsByAnime(animeId.value, 0, 4),
  { server: true, watch: [animeId] }
)

const anime = computed(() => animeData.value)
const synopsis = computed(() => anime.value ? displaySynopsis(anime.value) || '这部作品还没有同步到简介，但同好们的讨论已经在发光。' : '')
const mappedGenres = computed(() => anime.value ? mapGenres(anime.value.genres ?? []) : [])
const scoreText = computed(() => anime.value ? formatScore(communityScore(anime.value)) : '0.0')
const relatedAnime = computed(() => (relatedData.value ?? []).filter(item => item.id !== animeId.value).slice(0, 4))
const aigcCharacters = computed(() => aigcCharactersData.value ?? [])
const relatedCommunityPosts = computed<CommunityPostDto[]>(() => relatedCommunityData.value?.items ?? [])
const relatedCommunityPath = computed(() => {
  const slug = relatedCommunityPosts.value[0]?.communitySlug
  return slug ? `/community/${slug}` : '/community/anime'
})
const currentFireflyVotes = computed(() => anime.value?.fireflyVoteCount ?? dailyStore.getAnimeVotes(animeId.value))

const selectedRating = ref(5)
const userRating = ref(0)
const hoverRating = ref(0)
const draftComment = ref('')
const submitError = ref('')
const reviews = ref<Review[]>([])
const likedReviewIds = ref<number[]>([])
const likedReplyIds = ref<number[]>([])
const openReplyReviewId = ref<number | null>(null)
const replyDrafts = ref<Record<number, string>>({})
const replyErrors = ref<Record<number, string>>({})
const submittingReplyIds = ref<number[]>([])
const commentPanelRef = ref<HTMLElement | null>(null)
const submitting = ref(false)
const voting = ref(false)
const voteMessage = ref('')
const voteError = ref('')
const deleteTarget = ref<{ type: 'review'; id: number } | { type: 'reply'; reviewId: number; replyId: number } | null>(null)
const deleting = ref(false)
let commentObserver: IntersectionObserver | undefined

watch(anime, async (value) => {
  if (!value) {
    reviews.value = []
    likedReviewIds.value = []
    return
  }

  // 从后端拉取真实评论
  const backendReviews = await loadBackendReviews(value.id)
  // 合并本地评论（离线或刚提交的）
  const localReviews = readStoredReviews(value.id)
  reviews.value = mergeReviews(backendReviews, localReviews)
  likedReviewIds.value = readStoredLikes(value.id)
  likedReplyIds.value = readStoredReplyLikes(value.id)
}, { immediate: true })

async function loadBackendReviews(id: number): Promise<Review[]> {
  try {
    const dtos = await fetchReviews(id, 0, 50)
    const mappedReviews = dtos
      .filter(dto => dto.review && dto.review.trim())
      .map(dto => mapRatingDtoToReview(dto))
    await Promise.all(mappedReviews.map(async (review) => {
      review.replies = await loadReviewReplies(review.id)
    }))
    return mappedReviews
  } catch {
    return []
  }
}

async function loadReviewReplies(ratingId: number): Promise<ReviewReply[]> {
  if (ratingId < 0) return []
  try {
    const replies = await fetchRatingReplies(ratingId, 0, 30)
    return replies.map(reply => mapReplyDtoToReply(reply))
  } catch {
    return []
  }
}

function mapRatingDtoToReview(dto: any): Review {
  const starRating = dto.score ? Math.round(dto.score / 2) : 5
  const ownAuthor = isOwnReview(dto.userId) ? resolveReviewAuthor() : null
  const backendName = resolvePublicReviewName(dto.displayName, dto.username)
  const displayName = ownAuthor?.hasPublicName ? ownAuthor.name : backendName
  const avatar = ownAuthor?.avatar || dto.avatarUrl || undefined
  return {
    id: dto.id,
    userId: dto.userId,
    user: displayName,
    avatar,
    rating: Math.min(5, Math.max(1, starRating)),
    content: dto.review || '',
    date: formatRelativeTime(dto.createdAt),
    likes: dto.likeCount || 0,
    avatarGradient: avatar ? 'linear-gradient(135deg, #00E676, #7BC4E0)' : guestGradient(String(dto.userId || dto.id)),
    createdAt: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
    isFresh: isOwnReview(dto.userId),
    replies: [],
  }
}

function mapReplyDtoToReply(dto: any): ReviewReply {
  const ownAuthor = isOwnReview(dto.userId) ? resolveReviewAuthor() : null
  const backendName = resolvePublicReviewName(dto.displayName, dto.username)
  const displayName = ownAuthor?.hasPublicName ? ownAuthor.name : backendName
  const avatar = ownAuthor?.avatar || dto.avatarUrl || undefined

  return {
    id: dto.id,
    userId: dto.userId,
    user: displayName,
    avatar,
    content: dto.content || '',
    date: formatRelativeTime(dto.createdAt),
    likes: dto.likeCount || 0,
    avatarGradient: avatar ? 'linear-gradient(135deg, #00E676, #7BC4E0)' : guestGradient(String(dto.userId || dto.id)),
    createdAt: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
    isFresh: isOwnReview(dto.userId),
  }
}

function resolvePublicReviewName(displayName?: string, username?: string) {
  const name = (displayName || '').trim()
  const rawUsername = (username || '').trim()
  if (name && name !== rawUsername && !/^1\d{10}$/.test(name)) return name
  if (rawUsername && !/^1\d{10}$/.test(rawUsername)) return rawUsername
  return '番舍同好'
}

function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const now = Date.now()
  const date = new Date(dateStr).getTime()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const ratingTabs = computed(() => [5, 4, 3, 2, 1].map(star => ({
  star,
  label: ratingLabel(star),
  count: reviews.value.filter(review => review.rating === star).length,
})))

const selectedReviews = computed(() => reviews.value
  .filter(review => review.rating === selectedRating.value)
  .slice()
  .sort(sortReviewsForDisplay)
)

const topReview = computed(() => reviews.value.slice().sort((a, b) => b.likes - a.likes)[0])

const infoRows = computed(() => {
  if (!anime.value) return []
  return [
    { label: '首播时间', value: anime.value.airedFrom || anime.value.year || '未知' },
    { label: '集数', value: anime.value.episodes ? `${anime.value.episodes} 集` : '未知' },
    { label: '类型', value: tType(anime.value.type) || '未知' },
    { label: '制作公司', value: anime.value.studio || '未知' },
    { label: '原作来源', value: anime.value.source || '未知' },
    { label: '人气收藏', value: formatNumber(anime.value.favoritesCount) },
  ]
})

const { requireAuth } = useAuthModal()

function voteForCurrentAnime() {
  requireAuth(() => doVoteForCurrentAnime())
}

async function doVoteForCurrentAnime() {
  if (!anime.value || voting.value || dailyStore.state.tickets <= 0) return
  voting.value = true
  voteMessage.value = ''
  voteError.value = ''

  try {
    const voted = await dailyStore.voteForAnime({
      id: anime.value.id,
      title: displayTitle(anime.value),
      cover: anime.value.coverImage,
    })
    if (!voted) return
    const latestAnime = await fetchAnimeById(anime.value.id)
    if (latestAnime) animeData.value = latestAnime
    voteMessage.value = '投票成功，你的萤火已经计入全站排行'
  } catch (e: any) {
    voteError.value = e?.message || '投票暂时没有送达，请稍后再试'
  } finally {
    voting.value = false
  }
}

function selectRating(star: number) {
  requireAuth(() => {
    userRating.value = star
    selectedRating.value = star
  })
}

function submitReview() {
  requireAuth(() => {
    doSubmitReview()
  })
}

async function doSubmitReview() {
  if (!draftComment.value.trim() || submitting.value) return
  submitting.value = true
  submitError.value = ''
  const submittedContent = draftComment.value.trim()
  const submittedRating = selectedRating.value
  const optimisticReview = createLocalReview(submittedContent, submittedRating)

  applyOptimisticCommunityScore(submittedRating * 2)
  upsertReview(optimisticReview)
  userRating.value = submittedRating
  draftComment.value = ''

  try {
    // 提交到后端
    const result = await submitRating({
      animeId: animeId.value,
      score: submittedRating * 2,  // 前端1-5星 → 后端1-10分
      review: submittedContent,
    })

    if (result) {
      // 用后端返回的真实评论替换本地乐观评论
      const review = mapRatingDtoToReview(result)
      review.isFresh = true
      review.date = '刚刚'
      upsertReview(review, optimisticReview.id)

      const latestAnime = await fetchAnimeById(animeId.value)
      if (latestAnime) animeData.value = latestAnime
      dailyStore.completeTask('publish-comment')
    }
  } catch (e: any) {
    reviews.value = reviews.value.filter(review => review.id !== optimisticReview.id)
    draftComment.value = submittedContent
    submitError.value = e?.message || '评论暂时没有送达，请稍后再试'
    const latestAnime = await fetchAnimeById(animeId.value)
    if (latestAnime) animeData.value = latestAnime
  }

  persistLocalReviews()
  submitting.value = false
}

function toggleLike(id: number) {
  requireAuth(() => {
    doToggleLike(id)
  })
}

function doToggleLike(id: number) {
  const review = reviews.value.find(item => item.id === id)
  if (!review) return

  if (likedReviewIds.value.includes(id)) {
    likedReviewIds.value = likedReviewIds.value.filter(reviewId => reviewId !== id)
    review.likes = Math.max(0, review.likes - 1)
  } else {
    likedReviewIds.value = [...likedReviewIds.value, id]
    review.likes += 1
  }
  persistLocalReviews()
  persistStoredLikes()
}

function toggleReplyBox(reviewId: number) {
  openReplyReviewId.value = openReplyReviewId.value === reviewId ? null : reviewId
  if (!replyDrafts.value[reviewId]) {
    replyDrafts.value = { ...replyDrafts.value, [reviewId]: '' }
  }
  replyErrors.value = { ...replyErrors.value, [reviewId]: '' }
}

function submitReply(reviewId: number) {
  requireAuth(() => {
    doSubmitReply(reviewId)
  })
}

async function doSubmitReply(reviewId: number) {
  const content = (replyDrafts.value[reviewId] || '').trim()
  if (!content || submittingReplyIds.value.includes(reviewId)) return

  const review = reviews.value.find(item => item.id === reviewId)
  if (!review) return

  const optimisticReply = createLocalReply(content)
  review.replies = [...review.replies, optimisticReply]
  replyDrafts.value = { ...replyDrafts.value, [reviewId]: '' }
  replyErrors.value = { ...replyErrors.value, [reviewId]: '' }
  submittingReplyIds.value = [...submittingReplyIds.value, reviewId]

  try {
    const result = await submitRatingReply(reviewId, content)
    if (result) {
      const savedReply = mapReplyDtoToReply(result)
      savedReply.isFresh = true
      savedReply.date = '刚刚'
      replaceReply(reviewId, optimisticReply.id, savedReply)
      openReplyReviewId.value = null
    }
  } catch (e: any) {
    removeReply(reviewId, optimisticReply.id)
    replyDrafts.value = { ...replyDrafts.value, [reviewId]: content }
    replyErrors.value = { ...replyErrors.value, [reviewId]: e?.message || '回复暂时没有送达，请稍后再试' }
  } finally {
    submittingReplyIds.value = submittingReplyIds.value.filter(id => id !== reviewId)
  }
}

function createLocalReply(content: string): ReviewReply {
  const author = resolveReviewAuthor()
  return {
    id: -Date.now(),
    userId: Number(author.id) || undefined,
    user: author.name,
    avatar: author.avatar,
    content,
    date: '刚刚',
    likes: 0,
    avatarGradient: author.gradient,
    isFresh: true,
    createdAt: Date.now(),
  }
}

function replaceReply(reviewId: number, optimisticId: number, reply: ReviewReply) {
  const review = reviews.value.find(item => item.id === reviewId)
  if (!review) return
  review.replies = review.replies.map(item => item.id === optimisticId ? reply : item)
}

function removeReply(reviewId: number, replyId: number) {
  const review = reviews.value.find(item => item.id === reviewId)
  if (!review) return
  review.replies = review.replies.filter(item => item.id !== replyId)
}

function toggleReplyLike(reviewId: number, replyId: number) {
  requireAuth(() => {
    doToggleReplyLike(reviewId, replyId)
  })
}

async function doToggleReplyLike(reviewId: number, replyId: number) {
  const review = reviews.value.find(item => item.id === reviewId)
  const reply = review?.replies.find(item => item.id === replyId)
  if (!reply) return

  if (likedReplyIds.value.includes(replyId)) {
    likedReplyIds.value = likedReplyIds.value.filter(id => id !== replyId)
    reply.likes = Math.max(0, reply.likes - 1)
    persistStoredReplyLikes()
    return
  }

  likedReplyIds.value = [...likedReplyIds.value, replyId]
  reply.likes += 1
  persistStoredReplyLikes()

  try {
    if (replyId > 0) {
      const updated = await likeRatingReply(replyId)
      if (updated) {
        reply.likes = updated.likeCount || reply.likes
      }
    }
  } catch {
    likedReplyIds.value = likedReplyIds.value.filter(id => id !== replyId)
    reply.likes = Math.max(0, reply.likes - 1)
    persistStoredReplyLikes()
  }
}

function confirmDeleteReview(review: Review) {
  requireAuth(() => {
    deleteTarget.value = { type: 'review', id: review.id }
  })
}

function confirmDeleteReply(reviewId: number, reply: ReviewReply) {
  requireAuth(() => {
    deleteTarget.value = { type: 'reply', reviewId, replyId: reply.id }
  })
}

async function doDelete() {
  if (!deleteTarget.value || deleting.value) return
  const target = deleteTarget.value
  deleting.value = true

  try {
    if (target.type === 'review') {
      await deleteRating(target.id)
      reviews.value = reviews.value.filter(r => r.id !== target.id)
    } else {
      await deleteRatingReply(target.replyId)
      const review = reviews.value.find(r => r.id === target.reviewId)
      if (review) {
        review.replies = review.replies.filter(r => r.id !== target.replyId)
      }
    }
    deleteTarget.value = null
    persistLocalReviews()
  } catch (e: any) {
    console.warn('删除失败:', e?.message || e)
  } finally {
    deleting.value = false
  }
}

function formatScore(score?: number) {
  return score ? (Math.round(score * 10) / 10).toFixed(1) : '0.0'
}

function ratingLabel(star: number) {
  return ['想聊聊遗憾', '保留一点距离', '值得继续看', '很喜欢', '心头好'][star - 1]
}

function sortReviewsForDisplay(a: Review, b: Review) {
  if ((a.isFresh || a.isLocal) !== (b.isFresh || b.isLocal)) {
    return (a.isFresh || a.isLocal) ? -1 : 1
  }
  if (b.likes !== a.likes) return b.likes - a.likes
  return (b.createdAt ?? 0) - (a.createdAt ?? 0)
}

function createLocalReview(content: string, rating: number): Review {
  const author = resolveReviewAuthor()
  return {
    id: -Date.now(),
    userId: Number(author.id) || undefined,
    user: author.name,
    avatar: author.avatar,
    rating,
    content,
    date: '刚刚',
    likes: 0,
    avatarGradient: author.gradient,
    isFresh: true,
    createdAt: Date.now(),
    replies: [],
  }
}

function upsertReview(review: Review, optimisticId?: number) {
  reviews.value = [
    review,
    ...reviews.value.filter(item => {
      if (optimisticId && item.id === optimisticId) return false
      if (item.id === review.id) return false
      if (review.userId && item.userId === review.userId) return false
      return true
    }),
  ]
}

function applyOptimisticCommunityScore(score: number) {
  if (!animeData.value) return

  const previousCount = animeData.value.communityRatingCount ?? 0
  const previousMean = animeData.value.communityMeanRating ?? 0
  const previousOwnReview = findOwnReview()
  const oldScore = previousOwnReview ? previousOwnReview.rating * 2 : undefined
  const nextCount = previousOwnReview ? Math.max(previousCount, 1) : previousCount + 1
  const nextMean = previousOwnReview && previousCount > 0
    ? ((previousMean * previousCount) - (oldScore ?? 0) + score) / previousCount
    : ((previousMean * previousCount) + score) / nextCount

  animeData.value = {
    ...animeData.value,
    communityMeanRating: Math.round(nextMean * 10) / 10,
    communityRatingCount: nextCount,
  }
}

function findOwnReview() {
  const currentUserId = Number(userStore.user?.id)
  if (!currentUserId) return undefined
  return reviews.value.find(review => review.userId === currentUserId)
}

function isOwnReview(userId?: number) {
  const currentUserId = Number(userStore.user?.id)
  return Boolean(currentUserId && userId === currentUserId)
}

function reviewStorageKey(id = animeId.value) {
  return `${REVIEW_STORAGE_PREFIX}${id}`
}

function reviewLikesKey(id = animeId.value) {
  return `${REVIEW_LIKES_PREFIX}${id}`
}

function replyLikesKey(id = animeId.value) {
  return `aniglow_reply_likes_${id}`
}

function readStoredReviews(id: number): Review[] {
  if (import.meta.server) return []
  try {
    const raw = localStorage.getItem(reviewStorageKey(id))
    return raw ? JSON.parse(raw).map(normalizeReview) : []
  } catch {
    return []
  }
}

function readStoredLikes(id: number): number[] {
  if (import.meta.server) return []
  try {
    const raw = localStorage.getItem(reviewLikesKey(id))
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function readStoredReplyLikes(id: number): number[] {
  if (import.meta.server) return []
  try {
    const raw = localStorage.getItem(replyLikesKey(id))
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function persistLocalReviews() {
  if (import.meta.server) return
  try {
    // 只持久化本地评论（isLocal: true 或没有 userId 的评论）
    const localOnly = reviews.value.filter(r => r.isLocal)
    localStorage.setItem(reviewStorageKey(), JSON.stringify(localOnly))
  } catch {
    // localStorage may be unavailable in private modes.
  }
}

function persistStoredLikes() {
  if (import.meta.server) return
  try {
    localStorage.setItem(reviewLikesKey(), JSON.stringify(likedReviewIds.value))
  } catch {
    // localStorage may be unavailable in private modes.
  }
}

function persistStoredReplyLikes() {
  if (import.meta.server) return
  try {
    localStorage.setItem(replyLikesKey(), JSON.stringify(likedReplyIds.value))
  } catch {
    // localStorage may be unavailable in private modes.
  }
}

function mergeReviews(backendReviews: Review[], localReviews: Review[]) {
  const backendIds = new Set(backendReviews.map(r => r.id))
  // 过滤掉已经在后端列表中的本地评论（避免重复）
  const uniqueLocal = localReviews.filter(r => !backendIds.has(r.id)).map(normalizeReview)
  return [...uniqueLocal, ...backendReviews.map(normalizeReview)]
}

function normalizeReview(review: Review): Review {
  return {
    ...review,
    replies: Array.isArray(review.replies) ? review.replies : [],
  }
}

function resolveReviewAuthor() {
  const user = userStore.user
  const fallbackId = getOrCreateGuestId()
  const fallbackName = getOrCreateGuestName(fallbackId)
  const savedName = (user?.name || '').trim()
  const isPublicName = savedName
    && savedName !== user?.phone
    && !/^1\d{10}$/.test(savedName)

  return {
    id: user?.id || fallbackId,
    name: isPublicName ? savedName : fallbackName,
    avatar: user?.avatar || '',
    hasPublicName: Boolean(isPublicName),
    gradient: user?.avatar
      ? 'linear-gradient(135deg, #00E676, #7BC4E0)'
      : guestGradient(user?.id || fallbackId),
  }
}

function getOrCreateGuestId() {
  if (import.meta.server) return 'guest'
  const key = 'aniglow_guest_id'
  const existing = localStorage.getItem(key)
  if (existing) return existing
  const id = `guest-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`
  localStorage.setItem(key, id)
  return id
}

function getOrCreateGuestName(id: string) {
  if (import.meta.server) return '路过的番舍同好'
  const key = 'aniglow_guest_name'
  const existing = localStorage.getItem(key)
  if (existing) return existing
  const names = ['路过的萤火', '刚入坑的同好', '深夜补番人', '弹幕旁听生', '小小观测员']
  const name = names[Math.abs(hashText(id)) % names.length]
  localStorage.setItem(key, name)
  return name
}

function guestGradient(seed: string) {
  const gradients = [
    'linear-gradient(135deg, #00E676, #7BC4E0)',
    'linear-gradient(135deg, #FFC0CB, #A0D8EF)',
    'linear-gradient(135deg, #FFD54F, #00C853)',
    'linear-gradient(135deg, #C5B9E8, #FFB37E)',
  ]
  return gradients[Math.abs(hashText(seed)) % gradients.length]
}

function hashText(text: string) {
  return text.split('').reduce((hash, char) => ((hash << 5) - hash + char.charCodeAt(0)) | 0, 0)
}

useHead(() => ({
  title: anime.value ? `${displayTitle(anime.value)} - 萤火番舍` : '作品详情 - 萤火番舍',
  meta: [
    {
      name: 'description',
      content: synopsis.value.slice(0, 100),
    },
  ],
}))

onMounted(() => {
  dailyStore.hydrate()
  dailyStore.refreshVoteLeaderboard()

  if (!commentPanelRef.value) return
  commentObserver = new IntersectionObserver((entries) => {
    if (entries.some(entry => entry.isIntersecting)) {
      dailyStore.completeTask('view-comments')
      commentObserver?.disconnect()
      commentObserver = undefined
    }
  }, { threshold: 0.35 })
  commentObserver.observe(commentPanelRef.value)
})

onBeforeUnmount(() => {
  commentObserver?.disconnect()
})
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
