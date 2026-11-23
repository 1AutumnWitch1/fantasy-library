export type BookEmblem = 'moon' | 'swords' | 'rings' | 'compass' | 'key'

interface BookDefinition {
  id: string
  title: string
  binding: 'claret' | 'pine' | 'indigo' | 'ochre' | 'plum'
  emblem: BookEmblem
}

// Local navigation configuration for the shelf.
export const shelfBooks = [
  { id: 'anime', title: '追番', binding: 'claret', emblem: 'moon' },
  { id: 'games', title: '游戏', binding: 'pine', emblem: 'swords' },
  { id: 'community', title: '社区', binding: 'indigo', emblem: 'rings' },
  { id: 'recommendations', title: '推荐', binding: 'ochre', emblem: 'compass' },
  { id: 'personal', title: '个人书架', binding: 'plum', emblem: 'key' },
] as const satisfies readonly BookDefinition[]

export type ShelfBook = (typeof shelfBooks)[number]
export type ShelfBookId = ShelfBook['id']
