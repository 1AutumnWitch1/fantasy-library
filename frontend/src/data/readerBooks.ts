import { shelfBooks, type ShelfBookId } from './shelfBooks'
import type { BookDefinition } from '../reader/bookTypes'

const editions: Record<ShelfBookId, Omit<BookDefinition, 'spine'>> = {
  anime: {
    coverTitle: '番剧图鉴',
    theme: { light: '#a0524b', leather: '#682c39', shade: '#3a161e' },
    catalog: 'anime',
    spreads: [],
  },
  games: {
    coverTitle: '冒险图册',
    theme: { light: '#56785a', leather: '#284f40', shade: '#142b25' },
    catalog: 'games',
    spreads: [],
  },
  community: {
    coverTitle: '馆友来信',
    theme: { light: '#5c7191', leather: '#364963', shade: '#19283b' },
    spreads: [],
  },
  recommendations: {
    coverTitle: '馆员荐读',
    theme: { light: '#af7545', leather: '#754729', shade: '#44271a' },
    spreads: [],
  },
  personal: {
    coverTitle: '私人藏书志',
    theme: { light: '#845e83', leather: '#593650', shade: '#311a30' },
    spreads: [],
  },
}

export const readerBooks = shelfBooks.map((spine): BookDefinition => ({ spine, ...editions[spine.id] }))

export function findReaderBook(id: unknown) {
  return readerBooks.find(book => book.spine.id === id)
}
