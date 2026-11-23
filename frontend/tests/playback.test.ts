import { describe, expect, it } from 'vitest'
import { restorePosition } from '../src/player/resume'
import { timeLabel } from '../src/api/playback'

describe('player position', () => {
  it('preserves position on a node switch and clamps to the new source', () => {
    expect(restorePosition(4.25, 14, true)).toBe(4.25)
    expect(restorePosition(14.5, 14, true)).toBe(13.9)
  })

  it('starts a completed episode again and rejects invalid timing metadata', () => {
    expect(restorePosition(13.8, 14, false)).toBe(0)
    expect(restorePosition(-1, 14, false)).toBe(0)
    expect(restorePosition(3, Infinity, false)).toBe(0)
    expect(timeLabel(3855)).toBe('1:04:15')
  })
})
