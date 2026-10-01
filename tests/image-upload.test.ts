import { afterEach, describe, expect, it, vi } from 'vitest'
import { optimizeCommunityImage } from '../utils/image-upload'

describe('community image optimization', () => {
  afterEach(() => vi.restoreAllMocks())

  it('keeps small files and animated GIFs unchanged', async () => {
    const small = new File([new Uint8Array(128)], 'small.png', { type: 'image/png' })
    const gif = new File([new Uint8Array(900_000)], 'animated.gif', { type: 'image/gif' })

    expect(await optimizeCommunityImage(small)).toBe(small)
    expect(await optimizeCommunityImage(gif)).toBe(gif)
  })

  it('resizes a large photo before upload', async () => {
    const revokeObjectUrl = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test-photo')
    class MockImage {
      width = 4000
      height = 3000
      onload?: () => void
      onerror?: () => void
      set src(_value: string) { queueMicrotask(() => this.onload?.()) }
    }
    vi.stubGlobal('Image', MockImage)
    const context = { fillStyle: '', fillRect: vi.fn(), drawImage: vi.fn() }
    vi.spyOn(document, 'createElement').mockReturnValue({
      width: 0,
      height: 0,
      getContext: () => context,
      toDataURL: () => `data:image/jpeg;base64,${window.btoa('compressed-photo')}`,
    } as unknown as HTMLCanvasElement)
    const original = new File([new Uint8Array(2_000_000)], 'phone-photo.png', { type: 'image/png' })

    const optimized = await optimizeCommunityImage(original)

    expect(optimized).not.toBe(original)
    expect(optimized.type).toBe('image/jpeg')
    expect(optimized.name).toBe('phone-photo.jpg')
    expect(context.drawImage).toHaveBeenCalledWith(expect.anything(), 0, 0, 1600, 1200)
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:test-photo')
  })
})
