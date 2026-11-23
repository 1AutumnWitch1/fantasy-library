import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '../src/api/client'
import { useHealthStore } from '../src/stores/health'

beforeEach(() => setActivePinia(createPinia()))
afterEach(() => vi.restoreAllMocks())

describe('health connection status', () => {
  it.each(['NOT_CONFIGURED', 'DOWN', 'UP'] as const)(
    'preserves Oracle status %s independently of API availability',
    async (database) => {
      const payload = { application: 'UP', database }
      const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: payload })
      const store = useHealthStore()

      await store.refresh()

      expect(get).toHaveBeenCalledWith('/health')
      expect(store.phase).toBe('ready')
      expect(store.health).toEqual(payload)
    },
  )

  it('clears the previous success if the API stops responding', async () => {
    vi.spyOn(apiClient, 'get')
      .mockResolvedValueOnce({ data: { application: 'UP', database: 'UP' } })
      .mockRejectedValueOnce(new Error('Network error'))
    const store = useHealthStore()

    await store.refresh()
    expect(store.phase).toBe('ready')
    await store.refresh()

    expect(store.phase).toBe('failed')
    expect(store.health).toBeNull()
  })

  it('rejects an HTML fallback page served in place of the API', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({ data: '<!doctype html>' })
    const store = useHealthStore()

    await store.refresh()

    expect(store.phase).toBe('failed')
    expect(store.health).toBeNull()
  })
})
