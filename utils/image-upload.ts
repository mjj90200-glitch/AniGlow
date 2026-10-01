const MAX_LONG_EDGE = 1600
const JPEG_QUALITY = 0.8
const SKIP_COMPRESSION_BELOW = 768 * 1024
const PROCESSING_TIMEOUT_MS = 10_000

export async function optimizeCommunityImage(file: File, previewUrl?: string): Promise<File> {
  if (file.type === 'image/gif' || file.size <= SKIP_COMPRESSION_BELOW) return file

  let source: HTMLImageElement | null = null
  let objectUrl = ''
  try {
    const imageUrl = previewUrl || (objectUrl = URL.createObjectURL(file))
    source = await withTimeout(loadImage(imageUrl), PROCESSING_TIMEOUT_MS)

    const scale = Math.min(1, MAX_LONG_EDGE / Math.max(source.width, source.height))
    const width = Math.max(1, Math.round(source.width * scale))
    const height = Math.max(1, Math.round(source.height * scale))
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    if (!context) return file

    // JPEG 不支持透明通道，统一使用白色背景，避免 PNG 透明区域变黑。
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, width, height)
    context.drawImage(source, 0, 0, width, height)
    const blob = canvasToBlob(canvas, 'image/jpeg', JPEG_QUALITY)
    if (!blob || blob.size >= file.size) return file

    const name = file.name.replace(/\.[^.]+$/, '') || 'community-image'
    return new File([blob], `${name}.jpg`, { type: 'image/jpeg', lastModified: file.lastModified })
  } catch {
    // 个别浏览器或特殊图片无法在前端解码时，回退到后端处理原图。
    return file
  } finally {
    if (objectUrl) URL.revokeObjectURL(objectUrl)
  }
}

function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('无法解析图片'))
    image.src = url
  })
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number): Blob | null {
  try {
    const dataUrl = canvas.toDataURL(type, quality)
    const commaIndex = dataUrl.indexOf(',')
    if (commaIndex < 0) return null
    const binary = window.atob(dataUrl.slice(commaIndex + 1))
    const bytes = new Uint8Array(binary.length)
    for (let index = 0; index < binary.length; index++) bytes[index] = binary.charCodeAt(index)
    return new Blob([bytes], { type })
  } catch {
    return null
  }
}

function withTimeout<T>(promise: Promise<T>, timeoutMs: number): Promise<T> {
  return new Promise((resolve, reject) => {
    const timer = window.setTimeout(() => reject(new Error('图片处理超时')), timeoutMs)
    promise.then(
      value => {
        window.clearTimeout(timer)
        resolve(value)
      },
      error => {
        window.clearTimeout(timer)
        reject(error)
      },
    )
  })
}
