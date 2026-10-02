<template>
  <div class="flex items-start gap-3" :class="last ? '' : 'mb-3'">
    <span class="filter-row-label">{{ label }}</span>
    <div class="flex flex-1 flex-wrap gap-2">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  last?: boolean
}>(), {
  last: false,
})
</script>

<style scoped>
.filter-row-label {
  flex: 0 0 3rem;
  padding-top: 0.55rem;
  font-size: 0.8rem;
  font-weight: 800;
  color: #8a95a1;
}
</style>
