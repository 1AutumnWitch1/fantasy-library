import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { shelfBooks, type ShelfBookId } from '../data/shelfBooks'

export const useShelfStore = defineStore('shelf', () => {
  const selectedBookId = ref<ShelfBookId | null>(null)
  const selectedBook = computed(() => shelfBooks.find(book => book.id === selectedBookId.value))

  function selectBook(bookId: ShelfBookId) {
    selectedBookId.value = bookId
  }

  return { selectedBookId, selectedBook, selectBook }
})
