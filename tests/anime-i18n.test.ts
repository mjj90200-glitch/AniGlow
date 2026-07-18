import { describe, expect, it } from 'vitest'

import {
  communityScore,
  displaySynopsis,
  displayTitle,
  formatNumber,
  mapGenres,
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
})
