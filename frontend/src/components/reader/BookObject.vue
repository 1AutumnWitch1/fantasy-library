<script setup lang="ts">
import { ref } from 'vue'
import type { BookDefinition, BookSpread } from '../../reader/bookTypes'
import type { PageSide } from '../../reader/pagePosition'
import BookSigil from '../shelf/BookSigil.vue'
import type { BookTurn } from '../../reader/turnPlan'
import BookCover from './BookCover.vue'
import BookPage from './BookPage.vue'
import TurningLeaf from './TurningLeaf.vue'

defineProps<{
  definition: BookDefinition
  spread: BookSpread
  turn: BookTurn | null
  slideIndexes: Readonly<Record<number, number>>
  interactive: boolean
  reduced: boolean
  paused: boolean
  search: string
  marking?: boolean
  singlePage: boolean
  side: PageSide
}>()

const emit = defineEmits<{
  selectFeatured: [index: number]
  holdFeatured: [value: boolean]
  toggleFeaturedPause: []
  search: [query: string]
  markRead: [id?: number]
}>()

const layout = ref<HTMLElement | null>(null)
const book = ref<HTMLElement | null>(null)
const board = ref<HTMLElement | null>(null)
const cover = ref<HTMLElement | null>(null)
const leaf = ref<InstanceType<typeof TurningLeaf> | null>(null)
const paper = ref<HTMLElement | null>(null)

defineExpose({ layout, book, board, cover, leaf, paper })
</script>

<template>
  <div ref="layout" class="book-layout" :class="{ 'book-layout--single': singlePage }" data-book-layout>
    <div ref="book" class="book-object">
      <div ref="board" class="book-board">
        <div class="paper-stack" aria-hidden="true"></div>
        <div ref="paper" class="paper-plane paper-plane--static" :aria-hidden="turn !== null" data-visible-spread>
          <BookPage v-if="!singlePage || side === 'left'" :key="spread.left.number" :page="spread.left" side="left" :single="singlePage" :slide-index="slideIndexes[spread.left.number]"
            :interactive="interactive" :marking="marking" :reduced="reduced" :paused="paused" @select="emit('selectFeatured', $event)"
            @hold="emit('holdFeatured', $event)" @toggle-pause="emit('toggleFeaturedPause')"
            @mark-read="emit('markRead', $event)" />
          <BookPage v-if="!singlePage || side === 'right'" :key="spread.right.number" :page="spread.right" side="right" :single="singlePage" :interactive="interactive" :search="search" :marking="marking"
            @search="emit('search', $event)" @mark-read="emit('markRead', $event)" />
          <div v-if="!singlePage" class="paper-gutter" aria-hidden="true"></div>
        </div>
      </div>
      <div v-if="turn && !singlePage" class="paper-plane paper-plane--turn">
        <TurningLeaf ref="leaf" :turn="turn" :slide-indexes="slideIndexes" :reduced="reduced" :paused="paused" :search="search" />
      </div>

      <div ref="cover" class="book-hinge" aria-hidden="true" data-book-hinge>
        <div class="hinge-face hinge-face--front">
          <BookCover :book="definition" />
        </div>
        <div class="hinge-face hinge-face--inside">
          <div class="hinge-paper"><BookSigil :kind="definition.spine.emblem" /></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.book-layout {
  --paper-edge-block-start: 0.6rem;
  --paper-edge-block-end: 0.91rem;
  --paper-edge-inline: 0.8rem;
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
  perspective: max(1580px, 147vw);
}

.book-object {
  position: absolute;
  visibility: hidden;
  inset: 0;
  transform-style: preserve-3d;
}

.book-board {
  position: absolute;
  inset: 0;
  border: 1px solid var(--brass-frame);
  border-radius: 0.72rem 0.72rem 0.58rem 0.58rem;
  background:
    linear-gradient(90deg, #100b1044, transparent 41%, var(--binding-shade) 50%, transparent 57%, #15080e66),
    linear-gradient(var(--binding-light), var(--binding-leather) 75%, var(--binding-shade));
  box-shadow: inset 0 2px #ffc89689, inset 0 -3px var(--binding-shade), inset 0 -5px #9970526b;
}

.paper-stack {
  position: absolute;
  inset: var(--paper-edge-block-start) var(--paper-edge-inline) 0.36rem;
  border-radius: 0.18rem 0.18rem 0.34rem 0.32rem;
  background:
    linear-gradient(90deg, transparent 44%, #9a694536 48%, #4d3019b0 50%, #9a694536 52%, transparent 56%),
    repeating-linear-gradient(to bottom, #ae8a58 0 1px, #edd1a0 1px 2px, #c29d68 2px 3px);
  box-shadow: 0 0.18rem 0.37rem #150907bd;
}

.paper-plane {
  position: absolute;
  inset: var(--paper-edge-block-start) var(--paper-edge-inline) var(--paper-edge-block-end);
}

.paper-plane--static {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.paper-plane--turn {
  z-index: 3;
  transform-style: preserve-3d;
  pointer-events: none;
}

.paper-gutter {
  position: absolute;
  z-index: 1;
  inset-block: 0;
  left: 50%;
  width: clamp(1.3rem, 6.45vw, 5.4rem);
  transform: translateX(-50%);
  background: linear-gradient(90deg,
    transparent 0%, #5f432617 24%, #5c3a266e 47.5%, #50301dad 49.5%, #4f3219c2 50%,
    #563a2877 52%, #69482a29 67%, transparent 100%);
  pointer-events: none;
}

.book-hinge {
  position: absolute;
  z-index: 4;
  inset: 0 0 0 50%;
  transform-origin: left center;
  transform-style: preserve-3d;
  pointer-events: none;
}

.hinge-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
}

.hinge-face--front { transform: translateZ(0.3px); }

.hinge-face--inside {
  transform: rotateY(180deg) translateZ(0.3px);
  border: 1px solid #b48b62;
  border-radius: 0.55rem 0.33rem 0.33rem 0.55rem;
  background:
    linear-gradient(90deg, #f4d5a030, #9a66424d),
    var(--paper-grain) center / 38rem auto,
    #d5af78;
  box-shadow: inset 0 0 1.2rem #713d2d80, 0 0.26rem 1.25rem #1c0c0d8f;
}

.hinge-paper {
  position: absolute;
  inset: 5%; display: grid; place-items: center; color: #845b3b64;
  border: 1px double #9a764767; font-size: clamp(2rem, 8vw, 6rem);
}

.book-layout--single {
  --paper-edge-block-start: 0.36rem;
  --paper-edge-block-end: 0.69rem;
  --paper-edge-inline: 0.39rem;
  perspective: none;

  .book-object { transform-style: flat; }
  .paper-plane--static { grid-template-columns: minmax(0, 1fr); }
  .paper-stack { background: repeating-linear-gradient(to bottom, #ad875d 0 1px, #edcca0 1px 2px, #c29d68 2px 3px); }
  .book-board { background: var(--binding-leather); }
}
</style>
