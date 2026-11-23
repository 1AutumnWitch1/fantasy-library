import type { BookPageData, BookSpread } from './bookTypes'
import type { TurnDirection } from '../animations/readerMotion'

export interface BookTurn {
  direction: TurnDirection
  destination: number
  underlay: BookSpread
  front: BookPageData
  back: BookPageData
}

export function planTurn(
  spreads: readonly BookSpread[],
  current: number,
  step: -1 | 1,
): BookTurn | null {
  const destination = current + step
  const visible = spreads[current]
  const target = spreads[destination]
  if (!visible || !target) return null

  return turnBetween(visible, target, step, destination)
}

export function turnBetween(visible: BookSpread, target: BookSpread, step: -1 | 1, destination: number): BookTurn {
  // The turning leaf covers the already-mounted incoming page until the hinge reveals it.
  if (step === 1) {
    return {
      direction: 'forward',
      destination,
      underlay: { left: visible.left, right: target.right },
      front: visible.right,
      back: target.left,
    }
  }

  return {
    direction: 'backward',
    destination,
    underlay: { left: target.left, right: visible.right },
    front: visible.left,
    back: target.right,
  }
}
