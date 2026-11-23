export type PageSide = 'left' | 'right'

export function movePage(index: number, side: PageSide, step: -1 | 1, single: boolean) {
  if (!single) return { index: index + step, side: 'left' as const }

  const page = index * 2 + Number(side === 'right') + step
  return { index: Math.floor(page / 2), side: (page % 2 === 0 ? 'left' : 'right') as PageSide }
}
