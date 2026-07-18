import { defineStore } from 'pinia'

export interface Anime {
  id: string
  title: string
  cover: string
  rating: number
  episodes: number
  genres: string[]
  description?: string
  airDate?: string
  type?: string
  studio?: string
  director?: string
}

export const useAnimeStore = defineStore('anime', () => {
  // State
  const favorites = ref<Anime[]>([])
  const recentlyViewed = ref<Anime[]>([])

  // Getters
  const favoritesCount = computed(() => favorites.value.length)
  const isFavorite = (id: string) => favorites.value.some(a => a.id === id)

  // Actions
  const addToFavorites = (anime: Anime) => {
    if (!isFavorite(anime.id)) {
      favorites.value.unshift(anime)
    }
  }

  const removeFromFavorites = (id: string) => {
    const index = favorites.value.findIndex(a => a.id === id)
    if (index > -1) {
      favorites.value.splice(index, 1)
    }
  }

  const toggleFavorite = (anime: Anime) => {
    if (isFavorite(anime.id)) {
      removeFromFavorites(anime.id)
    } else {
      addToFavorites(anime)
    }
  }

  const addToRecentlyViewed = (anime: Anime) => {
    const index = recentlyViewed.value.findIndex(a => a.id === anime.id)
    if (index > -1) {
      recentlyViewed.value.splice(index, 1)
    }
    recentlyViewed.value.unshift(anime)
    if (recentlyViewed.value.length > 20) {
      recentlyViewed.value = recentlyViewed.value.slice(0, 20)
    }
  }

  return {
    favorites,
    recentlyViewed,
    favoritesCount,
    isFavorite,
    addToFavorites,
    removeFromFavorites,
    toggleFavorite,
    addToRecentlyViewed
  }
})
