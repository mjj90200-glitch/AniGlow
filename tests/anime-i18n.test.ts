import { describe, expect, it } from 'vitest'

import {
  communityScore,
  displaySynopsis,
  displayTitle,
  formatNumber,
  mapGenres,
  tagClass,
  tStatus,
  tType,
} from '../composables/useAnimeI18n'

describe('anime display helpers', () => {
  it('prefers localized title and synopsis', () => {
    expect(displayTitle({ titleCn: '葬送的芙莉莲', title: 'Sousou no Frieren' }))
      .toBe('葬送的芙莉莲')
    expect(displaySynopsis({ synopsisCn: '中文简介', synopsis: 'English synopsis' }))
      .toBe('中文简介')
  })

  it('shows only real community ratings', () => {
    expect(communityScore({ communityMeanRating: 9.26, communityRatingCount: 3 })).toBe(9.3)
    expect(communityScore({ communityMeanRating: 9.8, communityRatingCount: 0 })).toBe(0)
  })

  it('maps genres and formats audience counts', () => {
    expect(mapGenres(['Fantasy', 'Adventure', 'Drama', 'Comedy'])).toEqual([
      { label: '奇幻', type: 'fantasy' },
      { label: '冒险', type: 'adventure' },
      { label: '剧情', type: 'healing' },
    ])
    expect(formatNumber(12_400)).toBe('1.2万')
  })

  it('falls back through Japanese, original and empty titles', () => {
    expect(displayTitle({ titleJapanese: '葬送のフリーレン', title: 'Frieren' })).toBe('葬送のフリーレン')
    expect(displayTitle({ title: 'Frieren' })).toBe('Frieren')
    expect(displayTitle({})).toBe('')
  })

  it('falls back through source and empty synopsis', () => {
    expect(displaySynopsis({ synopsis: 'Source synopsis' })).toBe('Source synopsis')
    expect(displaySynopsis({})).toBe('')
  })

  it('handles missing community mean and count', () => {
    expect(communityScore({ communityRatingCount: 2 })).toBe(0)
    expect(communityScore({ communityMeanRating: 9.9 })).toBe(0)
  })

  it('translates known metadata and preserves unknown values', () => {
    expect(tType('TV')).toBe('TV动画')
    expect(tType('Podcast')).toBe('Podcast')
    expect(tType()).toBe('')
    expect(tStatus('Finished Airing')).toBe('已完结')
    expect(tStatus('Paused')).toBe('Paused')
    expect(tStatus()).toBe('')
  })

  it('maps known and fallback tag styles', () => {
    expect(tagClass('romance')).toBe('tag-romance')
    expect(tagClass('unknown')).toBe('tag-fantasy')
  })

  it('formats zero, thousands and plain values', () => {
    expect(formatNumber()).toBe('0')
    expect(formatNumber(1_500)).toBe('1.5k')
    expect(formatNumber(999)).toBe('999')
  })

  it('keeps unknown genres visible and limits cards to three tags', () => {
    expect(mapGenres(['Unknown', 'Action', 'Comedy', 'Drama'])).toEqual([
      { label: 'Unknown', type: 'fantasy' },
      { label: '热血', type: 'action' },
      { label: '搞笑', type: 'daily' },
    ])
  })
})
