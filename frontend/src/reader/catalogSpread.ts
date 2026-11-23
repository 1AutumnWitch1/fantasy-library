import type { CatalogKind, CatalogPageResult } from '../api/catalog'
import type { BookSpread, CataloguePage } from './bookTypes'

export const WORKS_PER_SPREAD = 4

export function catalogSpread(kind: CatalogKind, result: CatalogPageResult | null, message = ''): BookSpread {
  const entries = result?.items ?? []
  const first = ((result?.page ?? 1) - 1) * 2 + 1
  function page(number: number, items: typeof entries, title: string): CataloguePage {
    return { kind: 'catalogue', catalog: kind, number, title, items, message }
  }
  if (kind === 'anime' && entries.length) {
    return {
      left: {
        kind: 'featured', title: '精选番剧', number: first,
        slides: entries.map(({ work }) => ({
          workId: work.id, title: work.title, description: work.summary,
          artwork: { src: work.cover?.url ?? '' },
        })),
      },
      right: page(first + 1, entries, '番剧推荐'),
    }
  }
  const split = Math.ceil(entries.length / 2)
  return {
    left: page(first, entries.slice(0, split), kind === 'games' ? '游戏图鉴' : '精选番剧'),
    right: page(first + 1, entries.slice(split), kind === 'games' ? '馆藏检索' : '番剧推荐'),
  }
}
