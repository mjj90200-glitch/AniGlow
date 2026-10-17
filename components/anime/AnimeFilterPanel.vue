<template>
  <section class="mb-8 rounded-[2rem] border border-white/70 bg-white/60 backdrop-blur-xl p-4 sm:p-5 shadow-[0_18px_50px_rgba(31,68,47,0.08)]">
    <div class="flex items-center justify-between gap-3 mb-4">
      <div>
        <p class="text-xs font-bold tracking-[0.22em] uppercase text-firefly-600">Filter Garden</p>
        <h2 class="text-lg font-extrabold text-gray-800 mt-1">筛选</h2>
      </div>
      <div class="flex items-center gap-2">
        <span class="hidden sm:inline-flex items-center rounded-full bg-firefly-50 px-3 py-1 text-xs font-bold text-firefly-700">
          {{ activeGenreLabel }}
        </span>
        <button
          v-if="hasActiveFilter"
          type="button"
          class="rounded-full border border-white/70 bg-white/60 px-3 py-1 text-xs font-bold
                 text-gray-500 hover:text-firefly-600 hover:border-firefly/30 transition-colors"
          @click="$emit('reset')"
        >
          重置筛选
        </button>
      </div>
    </div>

    <FilterRow label="题材">
      <button
        v-for="category in genres"
        :key="category.genre || 'all'"
        type="button"
        class="category-chip"
        :class="{ 'category-chip-active': genre === category.genre }"
        @click="genre = category.genre"
      >
        <span>{{ category.icon }}</span>
        {{ category.label }}
      </button>
    </FilterRow>

    <FilterRow label="地区">
      <button
        v-for="option in countries"
        :key="`c-${option.value || 'all'}`"
        type="button"
        class="filter-chip"
        :class="{ 'category-chip-active': country === option.value }"
        @click="country = option.value"
      >
        {{ option.label }}
      </button>
    </FilterRow>

    <FilterRow label="格式">
      <button
        v-for="option in types"
        :key="`t-${option.value || 'all'}`"
        type="button"
        class="filter-chip"
        :class="{ 'category-chip-active': type === option.value }"
        @click="type = option.value"
      >
        {{ option.label }}
      </button>
    </FilterRow>

    <FilterRow label="年份" :last="true">
      <button
        v-for="option in years"
        :key="`y-${option.label}`"
        type="button"
        class="filter-chip"
        :class="{ 'category-chip-active': yearFrom === option.from && yearTo === option.to }"
        @click="yearFrom = option.from; yearTo = option.to"
      >
        {{ option.label }}
      </button>
    </FilterRow>
  </section>
</template>

<script setup lang="ts">
import FilterRow from './FilterRow.vue'
import type { AnimeFilterOption, AnimeGenreOption, AnimeYearOption } from '~/utils/anime-filters'

defineProps<{
  genres: AnimeGenreOption[]
  countries: AnimeFilterOption[]
  types: AnimeFilterOption[]
  years: AnimeYearOption[]
  activeGenreLabel: string
  hasActiveFilter: boolean
}>()

defineEmits<{ reset: [] }>()

const genre = defineModel<string>('genre', { required: true })
const country = defineModel<string>('country', { required: true })
const type = defineModel<string>('type', { required: true })
const yearFrom = defineModel<number | null>('yearFrom', { required: true })
const yearTo = defineModel<number | null>('yearTo', { required: true })
</script>

<style scoped>
.category-chip,
.filter-chip {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background: rgba(255, 255, 255, 0.62);
  color: #596473;
  font-weight: 800;
  box-shadow: 0 10px 28px rgba(31, 68, 47, 0.06);
  transition: all 0.24s ease;
}

.category-chip {
  gap: 0.4rem;
  padding: 0.62rem 0.95rem;
  font-size: 0.85rem;
}

.filter-chip {
  padding: 0.55rem 0.9rem;
  font-size: 0.82rem;
}

.category-chip:hover,
.filter-chip:hover {
  color: #07873f;
  transform: translateY(-1px);
  box-shadow: 0 16px 34px rgba(0, 170, 68, 0.13);
}

.category-chip-active,
.filter-chip.category-chip-active {
  border-color: rgba(0, 230, 118, 0.45);
  background: linear-gradient(135deg, rgba(0, 230, 118, 0.92), rgba(134, 239, 172, 0.82));
  color: #ffffff;
  box-shadow: 0 16px 38px rgba(0, 230, 118, 0.26);
}
</style>
