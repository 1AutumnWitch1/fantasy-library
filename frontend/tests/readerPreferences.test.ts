import { describe, expect, it } from 'vitest'
import { movePage } from '../src/reader/pagePosition'
import { simplifyReader } from '../src/reader/preferences'

describe('reader layout and motion', () => {
  it('shows both sides in order on narrow screens and steps across spreads only at their edges', () => {
    expect(movePage(0, 'left', 1, true)).toEqual({ index: 0, side: 'right' })
    expect(movePage(0, 'right', 1, true)).toEqual({ index: 1, side: 'left' })
    expect(movePage(1, 'left', -1, true)).toEqual({ index: 0, side: 'right' })
    expect(movePage(0, 'left', -1, true).index).toBe(-1)
    expect(movePage(0, 'left', 1, false)).toEqual({ index: 1, side: 'left' })
  })

  it('honors the operating system and narrow layout even when full motion was saved', () => {
    expect(simplifyReader('full', true, false, false)).toBe(true)
    expect(simplifyReader('full', false, true, false)).toBe(true)
    expect(simplifyReader('auto', false, false, true)).toBe(true)
    expect(simplifyReader('full', false, false, true)).toBe(false)
    expect(simplifyReader('simple', false, false, false)).toBe(true)
    expect(simplifyReader('auto', false, false, false)).toBe(false)
  })
})
