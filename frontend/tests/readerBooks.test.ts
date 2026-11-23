import { createPinia, setActivePinia } from 'pinia'
import { effectScope, nextTick } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import { readRecommendations, type RecommendationFeed } from '../src/api/recommendations'
import { findReaderBook, readerBooks } from '../src/data/readerBooks'
import { shelfBooks } from '../src/data/shelfBooks'
import { planTurn } from '../src/reader/turnPlan'
import { useReaderStore } from '../src/stores/reader'
import { useAccountStore } from '../src/stores/account'
import { useRecommendationBook } from '../src/reader/useRecommendationBook'
import { storeToRefs } from 'pinia'

vi.mock('../src/api/recommendations', () => ({ readRecommendations: vi.fn() }))

describe('multi-book reader', () => {
  it('matches every spine with a cover and theme and leaves live books to load their spreads', () => {
    expect(readerBooks.map(book => book.spine)).toEqual(shelfBooks)
    expect(new Set(readerBooks.map(book => book.theme.leather)).size).toBe(shelfBooks.length)
    expect(new Set(readerBooks.map(book => book.coverTitle)).size).toBe(shelfBooks.length)
    for (const book of readerBooks) {
      expect(findReaderBook(book.spine.id)).toBe(book)
      if (book.spine.id === 'anime' || book.spine.id === 'games') expect(book.catalog).toBe(book.spine.id)
      expect(book.spreads).toHaveLength(0)
    }
    expect(findReaderBook('unknown')).toBeUndefined()
  })

  it('numbers the recommendation spreads and discards responses from previous filters', async () => {
    setActivePinia(createPinia())
    const account = useAccountStore()
    account.status = 'guest'
    const type = storeToRefs(useReaderStore()).recommendationType
    const request = vi.mocked(readRecommendations)
    let resolveAll!: (feed: RecommendationFeed) => void
    request.mockImplementationOnce(() => new Promise(resolve => { resolveAll = resolve }))
    request.mockResolvedValueOnce({ mode: 'POPULAR', forYou: [], popular: [], newest: [], discussions: [] })
    const scope = effectScope()
    try {
      const reader = scope.run(() => useRecommendationBook(type))!
      type.value = 'GAME'
      await nextTick()
      const stale = request.mock.calls[0][2]
      expect(stale.aborted).toBe(true)
      expect(request.mock.calls[1].slice(0, 2)).toEqual(['GAME', false])
      resolveAll({ mode: 'EDITORIAL', forYou: [], popular: [], newest: [], discussions: [] })
      await nextTick()
      const spreads = reader.spreads.value
      expect(spreads.map(({ left, right }) => [left.number, right.number])).toEqual([[1, 2], [3, 4]])
      expect(spreads[0].left).toMatchObject({ mode: 'POPULAR', status: 'ready' })
      expect(planTurn(spreads, 0, 1)).toMatchObject({
        front: spreads[0].right,
        back: spreads[1].left,
        underlay: { left: spreads[0].left, right: spreads[1].right },
      })
    } finally {
      scope.stop()
      request.mockReset()
    }
  })

  it('retains each book’s progress without leaking it to other books', () => {
    setActivePinia(createPinia())
    const store = useReaderStore()
    const anime = store.stateFor('anime')
    anime.spreadIndex = 2
    anime.selectedTags = ['romance', 'comedy']
    anime.searchQuery = '雾港'
    anime.slideIndexes[5] = 1
    anime.carouselPaused = true

    expect(store.stateFor('games')).toEqual({
      spreadIndex: 0, selectedTags: [], searchQuery: '', slideIndexes: {}, carouselPaused: false,
    })
    expect(store.stateFor('anime')).toBe(anime)
    expect(store.reading.anime?.spreadIndex).toBe(2)
  })
})
