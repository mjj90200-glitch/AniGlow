import { describe, expect, it } from 'vitest'
import { ANIME_GENRE_OPTIONS, ANIME_TYPE_LABELS, ANIME_YEAR_RANGES } from '../utils/anime-filters'

describe('anime filter configuration', () => {
  it('keeps filter values unique and includes the reset option', () => {
    const values = ANIME_GENRE_OPTIONS.map(option => option.genre)
    expect(values[0]).toBe('')
    expect(new Set(values).size).toBe(values.length)
  })

  it('provides stable labels and non-overlapping historical ranges', () => {
    expect(ANIME_TYPE_LABELS.TV).toBe('TV动画')
    for (let index = 1; index < ANIME_YEAR_RANGES.length; index += 1) {
      const previous = ANIME_YEAR_RANGES[index - 1]
      const current = ANIME_YEAR_RANGES[index]
      if (current.from !== null) expect(current.to).toBeLessThan(previous.from ?? Number.POSITIVE_INFINITY)
    }
  })
})
