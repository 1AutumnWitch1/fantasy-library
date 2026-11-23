import { describe, expect, it } from 'vitest'
import { filterGames, gameGenres, games } from '../src/data/gameReader'
import type { BookSpread } from '../src/reader/bookTypes'

function entries(spreads: readonly BookSpread[]) {
  return spreads.flatMap(({ left, right }) => [left, right])
    .flatMap(page => page.kind === 'game' ? page.entries : [])
}

describe('游戏图鉴', () => {
  it('places two different covers on each full page and lists each game once', () => {
    const spreads = filterGames('all', '')
    expect(entries(spreads)).toEqual(games)
    expect(new Set(games.map(game => game.title)).size).toBe(games.length)
    expect(new Set(games.map(game => game.artwork.src + ':' + game.artwork.tile)).size).toBe(games.length)
    for (const { left, right } of spreads) {
      for (const page of [left, right]) {
        expect(page.kind === 'game' && page.entries.length).toBe(2)
      }
    }
  })

  it('backs every tag with matching games', () => {
    for (const tag of gameGenres.filter(tag => tag.id !== 'all')) {
      const result = entries(filterGames(tag.id, ''))
      expect(result.length, tag.label).toBeGreaterThan(0)
      expect(result.every(game => game.tags.includes(tag.id))).toBe(true)
    }
  })

  it('combines search terms, aliases, platforms and a game tag', () => {
    expect(entries(filterGames('动作', ' ＲＰＧ   ps5 ')).map(game => game.title)).toEqual(['余火守夜人'])
    expect(entries(filterGames('射击', '星港')).map(game => game.title)).toEqual(['零号星港'])
    const empty = filterGames('竞速', '温室')
    expect(empty).toHaveLength(1)
    expect(entries(empty)).toEqual([])
    expect(empty[0].left).toMatchObject({ number: 1, total: 0 })
  })
})
