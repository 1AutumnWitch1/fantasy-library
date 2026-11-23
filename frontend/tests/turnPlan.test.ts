import { describe, expect, it } from 'vitest'
import { measureBook } from '../src/reader/bookGeometry'
import { animeSpreads, filterReaderSpreads, readerGenres } from '../src/data/animeReader'
import { planTurn } from '../src/reader/turnPlan'

it('captures viewport coordinates from DOMRect accessors for the cover tween', () => {
  const rect = Object.create({
    get x() { return 302 },
    get y() { return 430 },
    get width() { return 149 },
    get height() { return 312 },
  }) as DOMRect
  const origin = measureBook({ getBoundingClientRect: () => rect })
  expect({ ...origin }).toEqual({ x: 302, y: 430, width: 149, height: 312 })
})

describe('reader leaf placement', () => {
  it('turns the outgoing right page over to the incoming left page', () => {
    expect(planTurn(animeSpreads, 0, 1)).toEqual({
      direction: 'forward',
      destination: 1,
      underlay: { left: animeSpreads[0].left, right: animeSpreads[1].right },
      front: animeSpreads[0].right,
      back: animeSpreads[1].left,
    })
  })

  it('turns the outgoing left page over to the incoming right page in reverse', () => {
    expect(planTurn(animeSpreads, 2, -1)).toEqual({
      direction: 'backward',
      destination: 1,
      underlay: { left: animeSpreads[1].left, right: animeSpreads[2].right },
      front: animeSpreads[2].left,
      back: animeSpreads[1].right,
    })
  })

  it('keeps the featured page and uses filtered right pages on both leaf faces', () => {
    const filtered = filterReaderSpreads(animeSpreads, 'detective')
    expect(filtered[0].left).toBe(animeSpreads[0].left)
    expect(filtered[0].right.number).toBe(2)
    expect(filtered[0].right.videos.map(video => video.title)).toEqual(['邮局里的旧风声'])
    expect(planTurn(filtered, 0, 1)?.front).toBe(filtered[0].right)
    expect(planTurn(filtered, 1, -1)?.back).toBe(filtered[0].right)
    expect(filterReaderSpreads(animeSpreads, 'all')).toBe(animeSpreads)
  })

  it('backs every genre with matching recommendations across all spreads', () => {
    expect(readerGenres).toHaveLength(30)
    for (const { id } of readerGenres) {
      if (id === 'all') continue
      for (const spread of filterReaderSpreads(animeSpreads, id)) {
        expect(spread.right.videos.length).toBeGreaterThan(0)
        expect(spread.right.videos.length).toBeLessThanOrEqual(6)
        expect(spread.right.videos.every(video => video.genres.includes(id))).toBe(true)
      }
    }
  })

  it('searches the local catalogue by title and genre even from a different spread', () => {
    const results = filterReaderSpreads(animeSpreads, 'all', '  科幻   出击  ')
    expect(results[0].right.videos.map(video => video.title)).toEqual(['晨光机甲 · 出击'])
    expect(results[2].right.videos).toEqual(results[0].right.videos)
    expect(results[2].left).toBe(animeSpreads[2].left)
  })

  it('combines the search with the selected genre and returns an empty page for no matches', () => {
    expect(filterReaderSpreads(animeSpreads, 'detective', '浑仪')[0].right.videos.map(video => video.title))
      .toEqual(['浑仪的暗号'])
    expect(filterReaderSpreads(animeSpreads, 'romance', '浑仪')[0].right.videos).toEqual([])
    expect(filterReaderSpreads(animeSpreads, 'all', '　 ')).toBe(animeSpreads)
  })

  it('stops at both ends of the book', () => {
    expect(planTurn(animeSpreads, 0, -1)).toBeNull()
    expect(planTurn(animeSpreads, animeSpreads.length - 1, 1)).toBeNull()
  })
})
