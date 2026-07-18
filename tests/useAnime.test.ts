import { beforeEach, describe, expect, it, vi } from 'vitest'

const request = vi.fn()

vi.stubGlobal('useApi', () => ({ request }))
vi.stubGlobal('useUserStore', () => ({ user: null }))

const { useAnime } = await import('../composables/useAnime')

describe('useAnime', () => {
  beforeEach(() => {
    request.mockReset()
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)
  })

  it('unwraps the paginated anime response through the shared API client', async () => {
    const response = {
      content: [{ id: 1, title: 'Sousou no Frieren' }],
      totalElements: 1,
      totalPages: 1,
      pageNumber: 0,
      pageSize: 20,
      last: true,
    }
    request.mockResolvedValue(response)

    await expect(useAnime().fetchAnimeList()).resolves.toEqual(response)
    expect(request).toHaveBeenCalledWith('/anime', {
      params: { page: 0, size: 20, sortBy: 'bayesianRating', direction: 'desc' },
    })
  })

  it('returns an empty page when the backend is unavailable', async () => {
    request.mockImplementation(async () => { throw new Error('offline') })

    const result = await useAnime().fetchAnimeList(0, 60)
    expect(result).toMatchObject({
      content: [],
      pageSize: 60,
      totalElements: 0,
    })
  })
})
