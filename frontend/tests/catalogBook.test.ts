import { effectScope } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'
import * as api from '../src/api/catalog'
import { catalogSpread } from '../src/reader/catalogSpread'
import { useCatalogBook } from '../src/reader/useCatalogBook'
import { turnBetween } from '../src/reader/turnPlan'

function page(number: number, ids: number[], total = 3): api.CatalogPageResult {
  return {
    page: number, size: 2, total, totalPages: Math.ceil(total / 2),
    items: ids.map(id => ({
      work: { id, title: '馆藏' + id, type: 'GAME', slug: 'work-' + id, summary: '简介', releaseDate: null,
        category: { id: 1, name: '冒险', slug: 'adventure' }, cover: null }, tags: [],
    })),
  }
}
function setup() {
  const state = { spreadIndex: 0, selectedTags: [] as string[], searchQuery: '' }
  const scope = effectScope()
  const live = scope.run(() => useCatalogBook('games', state))!
  return { live, state, scope }
}
afterEach(() => { vi.restoreAllMocks(); vi.useRealTimers() })

describe('remote catalogue spreads', () => {
  it('places up to two games on each side and uses the API page for folios', () => {
    const spread = catalogSpread('games', { ...page(3, [7, 8, 9, 10]), size: 4 })
    expect(spread.left).toMatchObject({ kind: 'catalogue', number: 5, items: [{ work: { id: 7 } }, { work: { id: 8 } }] })
    expect(spread.right).toMatchObject({ number: 6, items: [{ work: { id: 9 } }, { work: { id: 10 } }] })
    const anime = catalogSpread('anime', { ...page(1, [1, 2, 3, 4]), size: 4 })
    expect(anime.left).toMatchObject({ kind: 'featured', slides: [{ workId: 1 }, { workId: 2 }, { workId: 3 }, { workId: 4 }] })
    expect(anime.right.kind === 'catalogue' && anime.right.items.map(item => item.work.id)).toEqual([1, 2, 3, 4])
    expect(turnBetween(anime, spread, 1, 2)).toMatchObject({ front: anime.right, back: spread.left })
    expect(catalogSpread('anime', page(1, [], 0), '没有匹配的作品').left).toMatchObject({ message: '没有匹配的作品', items: [] })
  })

  it('ignores an earlier request even if its transport delivers a late response', async () => {
    let finish!: (result: api.CatalogPageResult) => void
    const apiCall = vi.spyOn(api, 'listWorks').mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
      .mockResolvedValueOnce(page(1, [8], 1))
    const { live, state, scope } = setup()
    live.reload(0)
    state.searchQuery = ' 馆藏8 '
    state.selectedTags = ['adventure', 'mystery']
    live.reload(0)
    await vi.waitFor(() => expect(live.page.value?.items[0]?.work.id).toBe(8))
    expect(apiCall.mock.calls[0][2].aborted).toBe(true)
    expect(apiCall.mock.calls[1][1]).toMatchObject({ q: '馆藏8', tags: ['adventure', 'mystery'], page: 1, size: 4 })
    finish(page(1, [1, 2]))
    await Promise.resolve()
    await Promise.resolve()
    expect(live.page.value?.items[0].work.id).toBe(8)
    scope.stop()
  })

  it('keeps the old spread on a page failure and until the animation commits a retry', async () => {
    vi.spyOn(api, 'listWorks').mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(page(2, [3]))
    const { live, state, scope } = setup()
    live.commit(page(1, [1, 2]))
    expect(await live.fetchPage(1)).toBeNull()
    expect(live.error.value).toBeTruthy()
    expect(live.failedIndex.value).toBe(1)
    const target = await live.fetchPage(1)
    expect(live.page.value?.page).toBe(1)
    expect(state.spreadIndex).toBe(0)
    live.commit(target!)
    expect(state.spreadIndex).toBe(1)
    expect(live.error.value).toBe('')
    scope.stop()
  })

  it.each(['anime', 'games'] as const)('requests four works for a %s spread', async kind => {
    const call = vi.spyOn(api, 'listWorks').mockResolvedValue({ ...page(1, [1, 2, 3, 4]), size: 4 })
    const scope = effectScope()
    const state = { spreadIndex: 0, selectedTags: [] as string[], searchQuery: '' }
    const book = scope.run(() => useCatalogBook(kind, state))!
    await book.fetchPage(0)
    expect(call.mock.calls[0][1]).toMatchObject({ page: 1, size: 4 })
    scope.stop()
  })

  it('recovers from deleted pages by loading the new last page', async () => {
    const call = vi.spyOn(api, 'listWorks').mockResolvedValueOnce(page(5, [])).mockResolvedValueOnce(page(2, [3]))
    const { live, scope } = setup()
    expect((await live.fetchPage(4))?.page).toBe(2)
    expect(call.mock.calls.map(([,query]) => query.page)).toEqual([5, 2])
    scope.stop()
  })

  it('aborts after closing and discards both delayed content and delayed filter options', async () => {
    let finish!: (result: api.CatalogPageResult) => void
    const call = vi.spyOn(api, 'listWorks').mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
    const filters = vi.spyOn(api, 'getTags').mockResolvedValue([{ id: 1, slug: 'rpg', name: '角色扮演' }])
    const { live, scope } = setup()
    const loading = live.fetchPage(1)
    const metadata = live.loadFilters()
    scope.stop()
    expect(call.mock.calls[0][2].aborted).toBe(true)
    expect(filters.mock.calls[0][1].aborted).toBe(true)
    finish(page(2, [3]))
    expect(await loading).toBeNull()
    await metadata
    expect(live.tagOptions.value).toEqual([])
  })
})
