<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { measureBook, type BookRect } from '../reader/bookGeometry'
import { saveDataEnabled } from '../reader/preferences'
import Bookshelf from '../components/shelf/Bookshelf.vue'
import LibraryAtmosphere from '../components/shelf/LibraryAtmosphere.vue'
import ShelfMascot from '../components/shelf/ShelfMascot.vue'

const mascotAvailable = import.meta.env.VITE_LIVE2D_AVAILABLE === 'true'
import { findReaderBook } from '../data/readerBooks'
import { shelfBooks, type ShelfBookId } from '../data/shelfBooks'
import type { BookDefinition } from '../reader/bookTypes'
import { useShelfStore } from '../stores/shelf'

const loadReader = () => import('../components/reader/Book.vue')
const Book = defineAsyncComponent(loadReader)

function warmReader() {
  if (!reader.value && !saveDataEnabled()) void loadReader().catch(() => {})
}

const shelf = useShelfStore()
const route = useRoute()
const router = useRouter()
const library = ref<HTMLElement | null>(null)
const reader = shallowRef<{
  book: BookDefinition; origin: BookRect; source: HTMLButtonElement
  animateOpening: boolean; restoreCatalog: boolean
} | null>(null)
const requestedBook = computed(() => route.name === 'book' ? findReaderBook(route.params.bookId) : undefined)

function openRequestedBook() {
  const book = requestedBook.value
  if (!book || reader.value) return

  const source = library.value?.querySelector<HTMLButtonElement>('[data-book-id="' + book.spine.id + '"]')
  if (!source) return
  const animateOpening = route.meta.animateBookOpening !== false
  if (animateOpening) source.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: 'instant' })
  reader.value = { book, origin: measureBook(source), source, animateOpening, restoreCatalog: route.meta.restoreCatalog === true }
  shelf.selectBook(book.spine.id)
}

watch(requestedBook, openRequestedBook, { flush: 'post' })
onMounted(openRequestedBook)

function selectBook(id: ShelfBookId) {
  if (!reader.value) void router.push({ name: 'book', params: { bookId: id } })
}

function returnToShelf() {
  // Deep links may have been opened outside the library.
  if (router.options.history.state.back === router.resolve({ name: 'shelf' }).fullPath) {
    router.back()
  } else {
    void router.replace({ name: 'shelf' })
  }
}

async function restoreBook() {
  const source = reader.value?.source
  reader.value = null
  // Wait for the old dialog to release focus and scroll before measuring the next spine.
  await nextTick()
  if (requestedBook.value) openRequestedBook()
  else source?.focus({ preventScroll: true })
}
</script>

<template>
  <main ref="library" class="library-home">
    <LibraryAtmosphere />
    <div class="shelf-layout">
      <div class="shelf-content">
        <div class="shelf-heading">
          <div class="heading-rule" aria-hidden="true"><span></span></div>
          <h1>馆藏书架</h1>
          <p>入夜开馆 · 从木架上取下一册翻阅</p>
        </div>

        <Bookshelf :books="shelfBooks" :selected-id="shelf.selectedBookId"
          :lifted-id="reader?.book.spine.id" @pointerenter="warmReader" @focusin="warmReader" @select="selectBook" />

        <p class="shelf-selection" role="status">
          <template v-if="shelf.selectedBook">
            <span aria-hidden="true"></span>已选中《{{ shelf.selectedBook.title }}》
          </template>
        </p>
      </div>
      <ShelfMascot v-if="mascotAvailable" :active="!requestedBook && !reader" />
    </div>

    <Book v-if="reader" :key="reader.book.spine.id" :book="reader.book"
      :origin="reader.origin" :source="reader.source" :close-requested="requestedBook !== reader.book"
      :animate-opening="reader.animateOpening" :restore-catalog="reader.restoreCatalog"
      @request-close="returnToShelf" @closed="restoreBook" />
  </main>
</template>

<style scoped lang="scss">
.library-home {
  position: relative;
  isolation: isolate;
  display: grid;
  align-items: center;
  min-height: calc(100vh - var(--library-header-height));
  min-height: calc(100svh - var(--library-header-height));
  background: var(--wood-deep);
}

.shelf-layout {
  width: min(100%, 96rem);
  margin-inline: auto;
}

@media (min-width: 72rem) {
  .shelf-layout:has(.library-mascot--shown) {
    display: grid;
    grid-template-columns: minmax(0, 1fr) clamp(14rem, 23vw, 24rem);
    align-items: center;

    .shelf-content {
      padding-inline-end: clamp(.9rem, 1.6vw, 1.6rem);
    }
  }
}

.shelf-content {
  width: min(100%, 75rem);
  margin-inline: auto;
  padding: clamp(1.35rem, 3.6vh, 2.75rem) clamp(1rem, 4.5vw, 4rem) clamp(1.35rem, 3vh, 2.2rem);
}

.shelf-heading {
  margin-bottom: clamp(1.15rem, 3.2vh, 2.15rem);
  text-align: center;

  h1 {
    margin: 0.28rem 0 0;
    padding-left: 0.14em;
    font-family: var(--display-font);
    font-weight: 700;
    font-size: clamp(1.85rem, 4.4vw, 3.6rem);
    letter-spacing: 0.14em;
    line-height: 1.22;
    color: var(--foil);
    text-shadow: 0 2px #2c180f, 0 4px 16px #070403e6;
  }

  p {
    margin: 0.5rem 0 0;
    color: #dec9a8;
    font-size: clamp(0.84rem, 1.3vw, 0.98rem);
    letter-spacing: 0.14em;
    text-shadow: 0 1px 5px #100905;
  }
}

.heading-rule {
  position: relative;
  width: 5.75rem;
  height: 1px;
  margin-inline: auto;
  background: linear-gradient(90deg, transparent, var(--brass-frame), transparent);

  > span {
    position: absolute;
    top: -0.18rem;
    left: calc(50% - 0.21rem);
    width: 0.42rem;
    height: 0.42rem;
    transform: rotate(45deg);
    border: 1px solid #edc28f;
    background: #63472c;
  }
}

.shelf-selection {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.55rem;
  min-height: 1.45rem;
  margin: clamp(0.95rem, 2.4vh, 1.55rem) 0 0;
  color: #f2d6a5;
  font-size: 0.92rem;
  letter-spacing: 0.06em;

  > span {
    width: 0.35rem;
    height: 0.35rem;
    transform: rotate(45deg);
    background: var(--brass);
  }
}

@media (max-width: 42rem) {
  .shelf-layout {
  width: min(100%, 96rem);
  margin-inline: auto;
}

@media (min-width: 72rem) {
  .shelf-layout:has(.library-mascot--shown) {
    display: grid;
    grid-template-columns: minmax(0, 1fr) clamp(14rem, 23vw, 24rem);
    align-items: center;

    .shelf-content {
      padding-inline-end: clamp(.9rem, 1.6vw, 1.6rem);
    }
  }
}

.shelf-content {
    padding: 1.1rem 0.85rem 1.4rem;
  }
  .shelf-heading {
    margin-bottom: 1.15rem;
    p { letter-spacing: 0.08em; }
  }
}
</style>
