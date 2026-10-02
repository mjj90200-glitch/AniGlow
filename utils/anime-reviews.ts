import type { Review } from '~/types/review'

export function resolvePublicReviewName(displayName?: string, username?: string) {
  const name = (displayName || '').trim()
  const rawUsername = (username || '').trim()
  if (name && name !== rawUsername && !/^1\d{10}$/.test(name)) return name
  if (rawUsername && !/^1\d{10}$/.test(rawUsername)) return rawUsername
  return '番舍同好'
}

export function formatRelativeTime(dateStr: string, now = Date.now()): string {
  if (!dateStr) return ''
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

export function ratingLabel(star: number) {
  return ['想聊聊遗憾', '保留一点距离', '值得继续看', '很喜欢', '心头好'][star - 1]
}

export function sortReviewsForDisplay(a: Review, b: Review) {
  if ((a.isFresh || a.isLocal) !== (b.isFresh || b.isLocal)) {
    return (a.isFresh || a.isLocal) ? -1 : 1
  }
  if (b.likes !== a.likes) return b.likes - a.likes
  return (b.createdAt ?? 0) - (a.createdAt ?? 0)
}

export function normalizeReview(review: Review): Review {
  return {
    ...review,
    replies: Array.isArray(review.replies) ? review.replies : [],
  }
}

export function guestGradient(seed: string) {
  const gradients = [
    'linear-gradient(135deg, #00E676, #7BC4E0)',
    'linear-gradient(135deg, #FFC0CB, #A0D8EF)',
    'linear-gradient(135deg, #FFD54F, #00C853)',
    'linear-gradient(135deg, #C5B9E8, #FFB37E)',
  ]
  return gradients[Math.abs(hashText(seed)) % gradients.length]
}

export function hashText(text: string) {
  return text.split('').reduce((hash, char) => ((hash << 5) - hash + char.charCodeAt(0)) | 0, 0)
}
