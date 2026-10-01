import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { computed, ref } from 'vue'

const fetchMock = vi.fn()
vi.stubGlobal('ref', ref)
vi.stubGlobal('computed', computed)
vi.stubGlobal('$fetch', fetchMock)

const { useUserStore } = await import('../stores/user')
const user = { id: '9', username: 'firefly', name: '萤火同好', credentialsInitialized: true }

describe('user session', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    fetchMock.mockReset()
  })

  it('restores the user from the HttpOnly server session', async () => {
    fetchMock.mockResolvedValueOnce({ data: user })
    const store = useUserStore()

    expect(await store.restoreSession()).toBe(true)
    expect(store.user?.name).toBe('萤火同好')
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/me')
  })

  it('stores only public user data after username login', async () => {
    fetchMock.mockResolvedValueOnce({ data: user })
    const store = useUserStore()

    await store.login('firefly', 'password123')

    expect(store.user).toEqual(user)
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', {
      method: 'POST', body: { username: 'firefly', password: 'password123' },
    })
  })

  it('clears local user state after server logout', async () => {
    fetchMock.mockResolvedValueOnce({ data: user }).mockResolvedValueOnce({ data: null })
    const store = useUserStore()
    await store.restoreSession()

    await store.logout()

    expect(store.user).toBeNull()
    expect(fetchMock).toHaveBeenLastCalledWith('/api/auth/logout', { method: 'POST' })
  })
})
