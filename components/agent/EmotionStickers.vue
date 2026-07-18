<template>
  <div class="bubble-sticker">
    <img
      v-if="imageAvailable"
      :src="src"
      :alt="alt"
      class="h-full w-full object-cover"
      @error="onError"
    />
    <span v-else class="text-2xl">{{ fallbackEmoji || '👤' }}</span>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  src: string
  fallbackSrc: string
  alt: string
  fallbackEmoji?: string
  imageAvailable: boolean
}>()

function onError(event: Event) {
  const image = event.target as HTMLImageElement
  if (image.src.endsWith(props.fallbackSrc)) return
  image.src = props.fallbackSrc
}
</script>
