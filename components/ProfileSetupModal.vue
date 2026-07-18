<template>
  <Transition
    enter-active-class="transition-all duration-500 ease-out"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition-all duration-300 ease-in"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div
      v-if="visible"
      class="fixed inset-0 z-[90] flex items-center justify-center px-4 py-8 bg-gray-900/20 backdrop-blur-sm"
      @click.self="() => {}"
    >
      <div class="glass-card-cream relative w-full max-w-md rounded-4xl overflow-hidden animate-scale-in">
        <!-- 顶部装饰光斑 -->
        <div class="absolute -top-12 -right-12 w-32 h-32 rounded-full pointer-events-none"
          style="background: rgba(0, 230, 118, 0.08); filter: blur(40px);" />
        <div class="absolute -bottom-8 -left-8 w-24 h-24 rounded-full pointer-events-none"
          style="background: rgba(255, 192, 203, 0.1); filter: blur(35px);" />

        <!-- 头部 -->
        <div class="relative px-8 pt-8 pb-4 text-center">
          <div class="w-16 h-16 mx-auto mb-4 rounded-2xl flex items-center justify-center animate-float"
            style="background: linear-gradient(135deg, #00E676 0%, #00C853 100%); box-shadow: 0 8px 32px rgba(0, 230, 118, 0.25);">
            <Sparkles class="w-8 h-8 text-white" />
          </div>
          <h2 class="text-xl font-extrabold text-gray-800 mb-1">欢迎来到萤火番舍</h2>
          <p class="text-sm text-gray-500 mb-2">设置你的专属形象，让番舍小伙伴认识你吧</p>
          <div class="flex items-center justify-center gap-1.5">
            <span class="firefly-dot" />
            <span class="text-xs text-gray-400">{{ uploadedImage ? '拖动图片调整位置' : '点击下方区域上传头像' }}</span>
            <span class="firefly-dot" style="animation-delay: 0.5s;" />
          </div>
        </div>

        <!-- ═══ 头像上传区 ═══ -->
        <div class="relative px-8 py-4">
          <label class="block text-xs font-semibold text-gray-500 mb-3 tracking-wide">
            设置你的头像
          </label>

          <!-- 未上传：上传区域 -->
          <div
            v-if="!uploadedImage"
            class="relative flex flex-col items-center justify-center gap-3 p-8 rounded-3xl
                   border-2 border-dashed cursor-pointer transition-all duration-300 group"
            :class="[
              isDragging
                ? 'border-firefly bg-firefly/5 scale-[1.02]'
                : 'border-cream-300 hover:border-sky hover:bg-sky/5'
            ]"
            style="background: rgba(255, 255, 255, 0.5);"
            @click="triggerFileInput"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="handleDrop"
          >
            <!-- 隐藏的 file input -->
            <input
              ref="fileInput"
              type="file"
              accept="image/png,image/jpeg,image/webp,image/gif"
              class="hidden"
              @change="handleFileSelect"
            />

            <!-- 上传图标 -->
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300
                        group-hover:scale-110 group-hover:shadow-glow"
              :style="{ background: isDragging
                ? 'linear-gradient(135deg, #00E676, #00C853)'
                : 'linear-gradient(135deg, rgba(160,216,239,0.3), rgba(123,196,224,0.2))' }">
              <Upload class="w-6 h-6 transition-colors duration-300"
                :class="isDragging ? 'text-white' : 'text-sky-dark'" />
            </div>

            <div class="text-center">
              <p class="text-sm font-semibold text-gray-600 group-hover:text-gray-800 transition-colors">
                点击上传或拖拽图片到此处
              </p>
              <p class="text-xs text-gray-400 mt-1">支持 PNG / JPEG / WebP，建议 200×200 以上</p>
            </div>
          </div>

          <!-- 已上传：圆形裁剪预览 -->
          <div v-else class="flex flex-col items-center gap-4">
            <!-- 圆形预览框 -->
            <div
              ref="cropContainer"
              class="relative w-40 h-40 rounded-full overflow-hidden shadow-glaze-lg ring-4 ring-white/80 cursor-grab active:cursor-grabbing"
              @mousedown="startDrag"
              @mousemove="onDrag"
              @mouseup="endDrag"
              @mouseleave="endDrag"
              @touchstart.prevent="startDragTouch"
              @touchmove.prevent="onDragTouch"
              @touchend="endDrag"
            >
              <img
                ref="previewImage"
                :src="uploadedImage"
                alt="头像预览"
                class="absolute max-w-none select-none pointer-events-none"
                :style="imageStyle"
                @load="onImageLoad"
              />
              <!-- 裁剪参考线（淡色十字） -->
              <div class="absolute inset-0 pointer-events-none rounded-full"
                style="box-shadow: inset 0 0 0 4px rgba(255,255,255,0.6);" />
            </div>

            <!-- 操作按钮 -->
            <div class="flex items-center gap-2">
              <button
                class="tag tag-fantasy text-xs cursor-pointer px-3 py-1.5 transition-all hover:scale-105"
                @click="triggerFileInput"
              >
                <Upload class="w-3 h-3 inline mr-1" />
                重新选择
              </button>
              <button
                class="tag text-xs cursor-pointer px-3 py-1.5 transition-all hover:scale-105"
                style="background: rgba(255,192,203,0.15); color: #D0707F;"
                @click="clearImage"
              >
                <X class="w-3 h-3 inline mr-1" />
                移除
              </button>
            </div>
            <p class="text-xs text-gray-400">拖动图片可调整裁剪位置</p>
          </div>
        </div>

        <!-- ═══ 用户名输入 ═══ -->
        <div class="relative px-8 py-2">
          <label class="block text-xs font-semibold text-gray-500 mb-3 tracking-wide">
            设置你的昵称
          </label>
          <div class="relative">
            <input
              v-model="username"
              type="text"
              maxlength="16"
              placeholder="取一个好听的名字吧..."
              class="input-field pr-12 text-center text-base font-semibold"
              @keydown.enter="handleSubmit"
            />
            <span class="absolute right-4 top-1/2 -translate-y-1/2 text-xs"
              :class="username.length > 12 ? 'text-sakura-dark' : 'text-gray-400'">
              {{ username.length }}/16
            </span>
          </div>
          <!-- 昵称建议 -->
          <div v-if="!username" class="flex flex-wrap gap-1.5 mt-3 justify-center">
            <button
              v-for="suggestion in nameSuggestions"
              :key="suggestion"
              class="tag text-xs cursor-pointer transition-all duration-200 hover:scale-105 tag-fantasy"
              @click="username = suggestion">
              {{ suggestion }}
            </button>
          </div>
        </div>

        <!-- ═══ 提交按钮 ═══ -->
        <div class="relative px-8 pt-6 pb-8">
          <button
            class="btn-glow w-full text-base py-3.5 gap-2 group"
            :disabled="!canSubmit"
            :class="{ 'opacity-40 cursor-not-allowed': !canSubmit }"
            @click="handleSubmit">
            <Sparkles class="w-4.5 h-4.5 transition-transform group-hover:rotate-12 duration-300" />
            <span>开启番舍之旅</span>
            <ChevronRight class="w-4.5 h-4.5 transition-transform group-hover:translate-x-1 duration-300" />
          </button>
          <p class="text-center text-xs text-gray-400 mt-3">别担心，随时可以在设置中修改</p>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { Sparkles, Upload, X, ChevronRight } from 'lucide-vue-next'

const emit = defineEmits<{
  complete: [data: { name: string; avatar: string }]
}>()

const visible = defineModel<boolean>('visible', { default: false })

// ─── 昵称 ────────────────────────────────────────────────────
const username = ref('')
const nameSuggestions = ['萤火虫', '小樱花', '海风盐', '星之卡比', '月光兔', '抹茶团子']

// ─── 头像上传与裁剪 ──────────────────────────────────────────
const fileInput = ref<HTMLInputElement>()
const cropContainer = ref<HTMLDivElement>()
const previewImage = ref<HTMLImageElement>()
const uploadedImage = ref('')       // 原始图片 data URL
const isDragging = ref(false)

// 裁剪状态
const cropSize = 200                // 裁剪圆直径
const imgNatural = reactive({ w: 0, h: 0 })
const imgDisplay = reactive({ w: 0, h: 0 })  // 容器内显示尺寸
const offset = reactive({ x: 0, y: 0 })       // 图片偏移（px）
const dragging = ref(false)
const dragStart = reactive({ x: 0, y: 0, ox: 0, oy: 0 })

const imageStyle = computed(() => ({
  width: `${imgDisplay.w}px`,
  height: `${imgDisplay.h}px`,
  left: `${offset.x}px`,
  top: `${offset.y}px`,
}))

const canSubmit = computed(() =>
  username.value.trim().length >= 1 &&
  username.value.trim().length <= 16 &&
  !!uploadedImage.value
)

// ─── 文件选择 ────────────────────────────────────────────────

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) processFile(file)
}

const handleDrop = (e: DragEvent) => {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) processFile(file)
}

const processFile = (file: File) => {
  if (!file.type.startsWith('image/')) return

  const reader = new FileReader()
  reader.onload = (e) => {
    uploadedImage.value = e.target?.result as string
    // 重置裁剪偏移
    offset.x = 0
    offset.y = 0
  }
  reader.readAsDataURL(file)
}

const clearImage = () => {
  uploadedImage.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

// ─── 图片加载后计算初始尺寸 ─────────────────────────────────

const onImageLoad = () => {
  const img = previewImage.value
  if (!img) return
  imgNatural.w = img.naturalWidth
  imgNatural.h = img.naturalHeight

  // 计算短边撑满 cropSize 的缩放
  const scale = cropSize / Math.min(imgNatural.w, imgNatural.h)
  imgDisplay.w = Math.round(imgNatural.w * scale)
  imgDisplay.h = Math.round(imgNatural.h * scale)

  // 居中
  offset.x = Math.round((cropSize - imgDisplay.w) / 2)
  offset.y = Math.round((cropSize - imgDisplay.h) / 2)
}

// ─── 拖拽裁剪 ────────────────────────────────────────────────

const clampOffset = () => {
  // 图片必须覆盖整个圆形，不能露出空白边
  const maxX = 0
  const minX = cropSize - imgDisplay.w
  const maxY = 0
  const minY = cropSize - imgDisplay.h

  offset.x = Math.min(maxX, Math.max(minX, offset.x))
  offset.y = Math.min(maxY, Math.max(minY, offset.y))
}

const startDrag = (e: MouseEvent) => {
  dragging.value = true
  dragStart.x = e.clientX
  dragStart.y = e.clientY
  dragStart.ox = offset.x
  dragStart.oy = offset.y
}

const onDrag = (e: MouseEvent) => {
  if (!dragging.value) return
  offset.x = dragStart.ox + (e.clientX - dragStart.x)
  offset.y = dragStart.oy + (e.clientY - dragStart.y)
  clampOffset()
}

const endDrag = () => {
  dragging.value = false
}

const startDragTouch = (e: TouchEvent) => {
  if (e.touches.length !== 1) return
  dragging.value = true
  dragStart.x = e.touches[0].clientX
  dragStart.y = e.touches[0].clientY
  dragStart.ox = offset.x
  dragStart.oy = offset.y
}

const onDragTouch = (e: TouchEvent) => {
  if (!dragging.value || e.touches.length !== 1) return
  offset.x = dragStart.ox + (e.touches[0].clientX - dragStart.x)
  offset.y = dragStart.oy + (e.touches[0].clientY - dragStart.y)
  clampOffset()
}

// ─── 提交：Canvas 圆形裁剪 ────────────────────────────────────

const cropToCircle = (): Promise<string> => {
  return new Promise((resolve) => {
    const img = previewImage.value
    if (!img) return resolve(uploadedImage.value)

    const canvas = document.createElement('canvas')
    canvas.width = cropSize
    canvas.height = cropSize
    const ctx = canvas.getContext('2d')
    if (!ctx) return resolve(uploadedImage.value)

    // 圆形裁剪路径
    ctx.beginPath()
    ctx.arc(cropSize / 2, cropSize / 2, cropSize / 2, 0, Math.PI * 2)
    ctx.clip()

    // 计算实际图片在原始图中的位置
    const scaleX = imgNatural.w / imgDisplay.w
    const scaleY = imgNatural.h / imgDisplay.h
    const sx = -offset.x * scaleX
    const sy = -offset.y * scaleY
    const sw = cropSize * scaleX
    const sh = cropSize * scaleY

    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, cropSize, cropSize)

    resolve(canvas.toDataURL('image/png', 0.9))
  })
}

const handleSubmit = async () => {
  if (!canSubmit.value) return
  const croppedAvatar = await cropToCircle()
  emit('complete', {
    name: username.value.trim(),
    avatar: croppedAvatar,
  })
}
</script>

<style scoped>
.input-field {
  width: 100%;
  padding: 0.75rem 1rem;
  border-radius: 1rem;
  border: 2px solid rgba(160, 216, 239, 0.4);
  background: rgba(255, 255, 255, 0.8);
  font-family: 'Nunito', system-ui, sans-serif;
  font-size: 0.95rem;
  outline: none;
  transition: all 0.3s ease;
}

.input-field:focus {
  border-color: #A0D8EF;
  box-shadow: 0 0 0 3px rgba(160, 216, 239, 0.2);
  background: white;
}
</style>
