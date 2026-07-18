<template>
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
</template>

<script setup lang="ts">
import { ArrowRight, BookOpen, Bot, Heart, MessageCircle, Sparkles } from 'lucide-vue-next'
import type { AnimeDto } from '~/types/anime'
import type { CommunityPostDto } from '~/composables/useCommunity'
import { displayTitle } from '~/composables/useAnimeI18n'

interface AigcCharacter {
  code: string
  displayName: string
  sourceTitle?: string | null
  avatarUrl?: string | null
}

defineProps<{
  anime: AnimeDto
  synopsis: string
  aigcCharacters: AigcCharacter[]
  relatedCommunityPosts: CommunityPostDto[]
  relatedCommunityPath: string
}>()
</script>

