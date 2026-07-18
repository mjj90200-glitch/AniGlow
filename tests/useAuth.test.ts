import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { computed, ref } from 'vue'

const cookies = new Map<string, ReturnType<typeof ref<string | null>>>()

vi.stubGlobal('ref', ref)
vi.stubGlobal('computed', computed)
vi.stubGlobal('useCookie', (name: string) => {
  if (!cookies.has(name)) cookies.set(name, ref<string | null>(null))
  return cookies.get(name)
})
vi.stubGlobal('$fetch', vi.fn())

const { useUserStore } = await import('../stores/user')

describe('user session', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    cookies.clear()
  })

  it('restores a persisted profile when the Authing token is present', () => {
    cookies.set('aniglow_token', ref('authing-token'))
    localStorage.setItem('aniglow_session', JSON.stringify({ id: '9', name: '萤火同好' }))
    const store = useUserStore()

    store.restoreSession()

    expect(store.user?.name).toBe('萤火同好')
  })

  it('clears cookies and local state on logout', () => {
    cookies.set('aniglow_token', ref('authing-token'))
    cookies.set('aniglow_backend_token', ref('backend-token'))
    localStorage.setItem('aniglow_session', JSON.stringify({ id: '9', name: '萤火同好' }))
    const store = useUserStore()
    store.restoreSession()

    store.logout()

    expect(store.user).toBeNull()
    expect(store.token).toBeNull()
    expect(store.backendToken).toBeNull()
    expect(localStorage.getItem('aniglow_session')).toBeNull()
  })
})
