export interface BookRect {
  x: number
  y: number
  width: number
  height: number
}

export function measureBook(target: Pick<HTMLElement, 'getBoundingClientRect'>): BookRect {
  const { x, y, width, height } = target.getBoundingClientRect()
  return { x, y, width, height }
}
