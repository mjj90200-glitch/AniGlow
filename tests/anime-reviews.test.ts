import { describe, expect, it } from 'vitest'
import type { Review } from '../types/review'
import {
  formatRelativeTime,
  guestGradient,
  normalizeReview,
  ratingLabel,
  resolvePublicReviewName,
  sortReviewsForDisplay,
} from '../utils/anime-reviews'

function review(overrides: Partial<Review> = {}): Review {
  return {
    id: 1,
    user: '同好',
    rating: 5,
    content: '很好看',
    date: '刚刚',
    likes: 0,
    avatarGradient: '',
    replies: [],
    ...overrides,
  }
}

describe('anime review helpers', () => {
  it('never exposes phone-like usernames as a public display name', () => {
    expect(resolvePublicReviewName('13800138000', '13800138000')).toBe('番舍同好')
    expect(resolvePublicReviewName('芙莉莲同好', '13800138000')).toBe('芙莉莲同好')
    expect(resolvePublicReviewName('', 'firefly')).toBe('firefly')
  })

  it('formats relative times against an injected clock', () => {
    const now = new Date('2026-10-02T12:00:00Z').getTime()
    expect(formatRelativeTime('2026-10-02T11:59:30Z', now)).toBe('刚刚')
    expect(formatRelativeTime('2026-10-02T11:45:00Z', now)).toBe('15分钟前')
    expect(formatRelativeTime('2026-10-02T09:00:00Z', now)).toBe('3小时前')
    expect(formatRelativeTime('2026-09-30T12:00:00Z', now)).toBe('2天前')
  })

  it('prioritizes fresh reviews, then likes and recency', () => {
    const fresh = review({ id: 1, isFresh: true, likes: 0 })
    const popular = review({ id: 2, likes: 10, createdAt: 10 })
    const recent = review({ id: 3, likes: 2, createdAt: 20 })
    expect([popular, fresh, recent].sort(sortReviewsForDisplay).map(item => item.id)).toEqual([1, 2, 3])
  })

  it('normalizes persisted reviews and keeps guest presentation deterministic', () => {
    expect(normalizeReview({ ...review(), replies: undefined as never }).replies).toEqual([])
    expect(guestGradient('same-user')).toBe(guestGradient('same-user'))
    expect(ratingLabel(5)).toBe('心头好')
  })
})
