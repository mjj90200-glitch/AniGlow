<template>
  <div
    ref="containerRef"
    class="ptr-container"
    :class="{ 'ptr-pulling': pulling, 'ptr-refreshing': refreshing }"
    @touchstart.passive="onStart"
    @touchmove="onMove"
    @touchend="onEnd"
  >
    <!-- 下拉指示器 -->
    <div class="ptr-indicator" :style="{ height: pullDistance + 'px', opacity: pullProgress }">
      <div class="ptr-indicator-inner">
        <span v-if="!refreshing" class="ptr-arrow" :style="{ transform: 'rotate(' + (Math.min(pullProgress, 1) * 180) + 'deg)' }">↓</span>
        <span v-else class="ptr-spinner" />
        <span class="ptr-text">{{ refreshing ? '刷新中...' : statusText }}</span>
      </div>
    </div>

    <!-- 内容 -->
    <slot />
  </div>
</template>

<script setup lang="ts">
const containerRef = ref<HTMLElement>()
const pulling = ref(false)
const refreshing = ref(false)
const pullDistance = ref(0)
const pullProgress = ref(0)
const statusText = ref('下拉刷新')

const THRESHOLD = 64
const MAX_PULL = 96

let startY = 0
let currentY = 0

function onStart(e: TouchEvent) {
  if (refreshing.value) return
  // 只在页面顶部才触发下拉刷新
  if (window.scrollY > 5) return
  startY = e.touches[0].clientY
  currentY = startY
  pulling.value = true
}

function onMove(e: TouchEvent) {
  if (!pulling.value) return
  currentY = e.touches[0].clientY
  const delta = currentY - startY
  if (delta <= 0) {
    pullDistance.value = 0
    pullProgress.value = 0
    return
  }
  // 阻尼效果
  const damped = Math.min(delta * 0.5, MAX_PULL)
  pullDistance.value = damped
  pullProgress.value = Math.min(damped / THRESHOLD, 1)
  statusText.value = damped >= THRESHOLD ? '释放刷新' : '下拉刷新'

  if (delta > 10) {
    e.preventDefault()
  }
}

async function onEnd() {
  if (!pulling.value) return
  pulling.value = false

  if (pullDistance.value >= THRESHOLD) {
    refreshing.value = true
    pullDistance.value = THRESHOLD * 0.6
    pullProgress.value = 0.6

    try {
      await refreshNuxtData()
      await nextTick()
    } catch {}

    // 短暂显示完成状态
    statusText.value = '已刷新'
    await new Promise(r => setTimeout(r, 400))
  }

  // 收起
  refreshing.value = false
  pullDistance.value = 0
  pullProgress.value = 0
  statusText.value = '下拉刷新'
}

// 清除旧的原生刷新样式
onMounted(() => {
  document.documentElement.style.overscrollBehaviorY = 'auto'
})
</script>

<style scoped>
.ptr-container {
  position: relative;
  min-height: 100%;
  overscroll-behavior-y: auto;
  -webkit-overflow-scrolling: touch;
}

.ptr-indicator {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  overflow: hidden;
  transition: height 0.15s ease-out;
}

.ptr-indicator-inner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-bottom: 0.5rem;
  color: #94a3b8;
}

.ptr-arrow {
  display: inline-block;
  font-size: 1.1rem;
  transition: transform 0.15s ease;
}

.ptr-spinner {
  width: 1.1rem;
  height: 1.1rem;
  border: 2px solid #cbd5e1;
  border-top-color: #0e7490;
  border-radius: 50%;
  animation: ptr-spin 0.7s linear infinite;
}

@keyframes ptr-spin { to { transform: rotate(360deg); } }

.ptr-text {
  font-size: 0.75rem;
  font-weight: 700;
}
</style>
