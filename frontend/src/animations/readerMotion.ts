import { gsap } from 'gsap'
import { measureBook, type BookRect } from '../reader/bookGeometry'

export type TurnDirection = 'forward' | 'backward'

export interface ReaderMotionElements {
  backdrop: HTMLElement
  controls: HTMLElement[]
  book: HTMLElement
  board: HTMLElement
  cover: HTMLElement
  flight: HTMLElement
  spine: HTMLElement
}

const openBoard = 'inset(0% 0% 0% 0%)'
const closedBoard = 'inset(0% 0% 0% 50%)'

export function closedPosition(layout: HTMLElement): BookRect {
  const { x, y, width, height } = measureBook(layout)
  return { x: x + width / 4, y, width: width / 2, height }
}

function at(rect: BookRect, base: BookRect) {
  return { x: rect.x, y: rect.y, scaleX: rect.width / Math.max(1, base.width), scaleY: rect.height / Math.max(1, base.height) }
}

export function prepareReader(elements: ReaderMotionElements) {
  const { backdrop, controls, book, board, cover, flight } = elements
  gsap.set(backdrop, { autoAlpha: 0 })
  gsap.set(controls, { autoAlpha: 0 })
  gsap.set(book, { autoAlpha: 0, xPercent: -25 })
  gsap.set(board, { clipPath: closedBoard })
  gsap.set(cover, { autoAlpha: 0, rotationY: 0 })
  gsap.set(flight, { autoAlpha: 0 })
}

export function showOpenReader({ backdrop, controls, book, board, cover, flight }: ReaderMotionElements) {
  gsap.set([backdrop, ...controls], { autoAlpha: 1 })
  gsap.set(book, { autoAlpha: 1, xPercent: 0 })
  gsap.set(board, { clearProps: 'clipPath' })
  gsap.set(cover, { autoAlpha: 0, rotationY: -180 })
  gsap.set(flight, { autoAlpha: 0 })
}

export function openBook(
  elements: ReaderMotionElements,
  origin: BookRect,
  destination: BookRect,
  reduced: boolean,
  onComplete: () => void,
) {
  const { backdrop, controls, book, board, cover, flight, spine } = elements
  const motion = gsap.timeline({ onComplete })

  if (reduced) {
    return motion
      .set(book, { xPercent: 0 })
      .set(board, { clearProps: 'clipPath' })
      .to(backdrop, { autoAlpha: 1, duration: 0.14 }, 0)
      .to(book, { autoAlpha: 1, duration: 0.16 }, 0)
      .to(controls, { autoAlpha: 1, duration: 0.14 }, 0.03)
  }

  const unfold = 0.42
  return motion
    .set(flight, { width: destination.width, height: destination.height, ...at(origin, destination), autoAlpha: 1, rotation: 0 })
    .set(spine, { autoAlpha: 1 }, 0)
    .to(backdrop, { autoAlpha: 1, duration: 0.28 }, 0)
    .to(flight, { ...at(destination, destination), duration: unfold, ease: 'power3.inOut' }, 0)
    .to(flight, { rotation: -3.2, duration: 0.15, ease: 'sine.out' }, 0)
    .to(flight, { rotation: 0, duration: unfold - 0.15, ease: 'sine.inOut' }, 0.15)
    .to(spine, { autoAlpha: 0, duration: 0.22 }, 0.08)
    .set(cover, { autoAlpha: 1 }, unfold)
    .set(book, { autoAlpha: 1 }, unfold)
    .set(flight, { autoAlpha: 0 }, unfold)
    .to(book, { xPercent: 0, duration: 0.46, ease: 'power2.inOut' }, unfold)
    .to(cover, { rotationY: -180, duration: 0.5, ease: 'power2.inOut' }, unfold)
    .to(board, { clipPath: openBoard, duration: 0.36, ease: 'power2.out' }, unfold + 0.14)
    .set(board, { clearProps: 'clipPath' }, unfold + 0.5)
    .set(cover, { autoAlpha: 0 }, unfold + 0.5)
    .to(controls, { autoAlpha: 1, duration: 0.18 }, unfold + 0.34)
}

export function closeBook(
  elements: ReaderMotionElements,
  closed: BookRect,
  origin: BookRect,
  reduced: boolean,
  onComplete: () => void,
) {
  const { backdrop, controls, book, board, cover, flight, spine } = elements
  const motion = gsap.timeline({ onComplete })

  if (reduced) {
    return motion
      .to(controls, { autoAlpha: 0, duration: 0.11 }, 0)
      .to(book, { autoAlpha: 0, duration: 0.15 }, 0)
      .to(backdrop, { autoAlpha: 0, duration: 0.16 }, 0)
  }

  const returnAt = 0.42
  return motion
    .set(cover, { autoAlpha: 1, rotationY: -180 })
    .set(board, { clipPath: openBoard }, 0)
    .to(controls, { autoAlpha: 0, duration: 0.15 }, 0)
    .to(cover, { rotationY: 0, duration: returnAt, ease: 'power2.inOut' }, 0)
    .to(book, { xPercent: -25, duration: returnAt, ease: 'power2.inOut' }, 0)
    .to(board, { clipPath: closedBoard, duration: 0.25, ease: 'power2.inOut' }, 0.14)
    .set(flight, { width: closed.width, height: closed.height, ...at(closed, closed), autoAlpha: 1, rotation: 0 }, returnAt)
    .set(spine, { autoAlpha: 0 }, returnAt)
    .set(book, { autoAlpha: 0 }, returnAt)
    .to(flight, { ...at(origin, closed), duration: 0.39, ease: 'power3.inOut' }, returnAt)
    .to(flight, { rotation: -2.8, duration: 0.14, ease: 'sine.out' }, returnAt)
    .to(flight, { rotation: 0, duration: 0.25, ease: 'sine.inOut' }, returnAt + 0.14)
    .to(spine, { autoAlpha: 1, duration: 0.16 }, returnAt + 0.2)
    .to(backdrop, { autoAlpha: 0, duration: 0.32 }, returnAt + 0.04)
}

export function turnLeaf(
  leaf: HTMLElement,
  shades: HTMLElement[],
  direction: TurnDirection,
  onComplete: () => void,
) {
  const sign = direction === 'forward' ? -1 : 1
  return gsap.timeline({ onComplete })
    .to(leaf, { rotationY: sign * 180, duration: 0.58, ease: 'power2.inOut' }, 0)
    .to(shades, { opacity: 0.46, duration: 0.24, ease: 'sine.in' }, 0.04)
    .to(shades, { opacity: 0, duration: 0.24, ease: 'sine.out' }, 0.3)
}

export function fadePage(page: HTMLElement, swap: () => void, onComplete: () => void) {
  return gsap.timeline({ onComplete })
    .to(page, { opacity: 0, duration: 0.08, ease: 'none' })
    .call(swap)
    .to(page, { opacity: 1, duration: 0.14, ease: 'sine.out' })
    .set(page, { clearProps: 'opacity' })
}
