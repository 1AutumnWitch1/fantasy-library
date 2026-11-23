<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FeaturedItem } from '../../reader/bookTypes'
import ReaderArtwork from './ReaderArtwork.vue'

const props = defineProps<{
  slides: readonly FeaturedItem[]
  index: number
  interactive: boolean
  reduced: boolean
  paused: boolean
}>()

const emit = defineEmits<{ select: [index: number]; hold: [value: boolean]; togglePause: [] }>()
const hovered = ref(false)
const focused = ref(false)
const previous = ref(-1)
watch(() => props.index, (_, from) => { previous.value = from })

function selectOffset(offset: number) {
  emit('select', (props.index + offset + props.slides.length) % props.slides.length)
}

function setHover(value: boolean) {
  hovered.value = value
  if (props.interactive) emit('hold', hovered.value || focused.value)
}

function setFocus(value: boolean, event: FocusEvent) {
  if (!value && event.relatedTarget instanceof Node &&
      (event.currentTarget as HTMLElement).contains(event.relatedTarget)) return
  focused.value = value
  if (props.interactive) emit('hold', hovered.value || focused.value)
}
</script>

<template>
  <section class="featured-carousel" aria-label="精选轮播"
    @mouseenter="setHover(true)" @mouseleave="setHover(false)"
    @focusin="setFocus(true, $event)" @focusout="setFocus(false, $event)"
    @keydown.left.stop.prevent="selectOffset(-1)" @keydown.right.stop.prevent="selectOffset(1)">
    <div class="featured-window">
      <div class="featured-track" :class="{ 'is-animated': interactive && !reduced }"
        :style="{ transform: 'translateX(' + (-100 * index) + '%)' }">
        <figure v-for="(slide, position) in slides" :key="slide.workId ?? slide.title" class="featured-slide"
          :aria-hidden="position !== index" :data-current-slide="position === index ? '' : undefined">
          <ReaderArtwork v-if="slide.artwork.src && (position === index || (!reduced && position === previous))" :artwork="slide.artwork" />
          <figcaption class="featured-caption">
            <strong>{{ slide.title }}</strong>
            <span>{{ slide.description }}</span>
          </figcaption>
          <RouterLink v-if="slide.workId && interactive && position === index" class="featured-detail"
            :to="{ name: 'work-detail', params: { id: slide.workId }, query: { from: 'anime' } }" :aria-label="'查看《' + slide.title + '》资料'" />
        </figure>
      </div>
    </div>

    <div class="featured-controls">
      <div class="featured-dots" aria-label="精选位置">
        <button v-for="(slide, position) in slides" :key="slide.workId ?? slide.title" type="button"
          :aria-label="'查看' + slide.title" :aria-current="index === position ? 'true' : undefined"
          :disabled="!interactive" @click="emit('select', position)"><span></span></button>
      </div>
      <div class="featured-arrows">
        <button type="button" aria-label="上一条推荐" :disabled="!interactive" @click="selectOffset(-1)">‹</button>
        <button type="button" :aria-label="paused ? '继续轮播' : '暂停轮播'" :disabled="!interactive || reduced"
          @click="emit('togglePause')"><span aria-hidden="true">{{ paused ? '▷' : 'Ⅱ' }}</span></button>
        <button type="button" aria-label="下一条推荐" :disabled="!interactive" @click="selectOffset(1)">›</button>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.featured-detail { position: absolute; z-index: 2; inset: 0; }
.featured-detail:focus-visible { outline: 3px solid #edcf9b; outline-offset: -4px; }
.featured-carousel {
  position: relative;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #8a6642;
  outline: 1px solid #94744864;
  outline-offset: 2px;
  border-radius: 0.28rem;
  background: #1c2b25;
  box-shadow: 0 0.3rem 0.9rem #58351d58;
  color: #f5e2b8;
}

.featured-window { min-height: 0; overflow: hidden; }

.featured-track {
  display: flex;
  width: 100%;
  height: 100%;

  &.is-animated { transition: transform 360ms cubic-bezier(0.27, 0.76, 0.31, 1); }
}

.featured-slide {
  position: relative;
  flex: 0 0 100%;
  display: grid;
  align-content: end;
  min-width: 0;
  height: 100%;
  margin: 0;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, #140b0526, transparent 42%, #131c16f0);
    pointer-events: none;
  }
}

.featured-caption {
  position: relative;
  z-index: 1;
  display: grid;
  gap: clamp(0.25rem, 0.9vh, 0.5rem);
  padding: clamp(0.65rem, 1.5vw, 1.5rem);
  padding-top: 2rem;

  strong {
    font: 700 clamp(1.15rem, 2.9vw, 2.9rem) / 1.2 var(--display-font);
    letter-spacing: 0.06em;
    text-shadow: 0 2px 8px #120d09;
  }

  > span {
    color: #f3e0bb;
    font-size: clamp(0.86rem, 1.12vw, 1.06rem);
    line-height: 1.48;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-shadow: 0 1px 4px #0e110d;
  }
}

.featured-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.25rem;
  padding: 0.34rem clamp(0.45rem, 1.4vw, 1.15rem);
  border-top: 1px solid #a4845c70;
  background: var(--pine);
}

.featured-dots, .featured-arrows { display: flex; align-items: center; gap: 0.24rem; }

button {
  display: grid;
  place-items: center;
  flex: none;
  min-width: 2.2rem;
  min-height: 2.2rem;
  padding: 0;
  color: #f3deb0;
  border: 1px solid #a48859;
  border-radius: 0.22rem;
  background: #19271f;
  font: 1.15rem / 1 Georgia, serif;
  cursor: pointer;

  &:hover:enabled { background: var(--pine-hover); }
  &:focus-visible { outline: 2px solid #ffe4a0; outline-offset: 1px; }
  &:disabled { cursor: default; }
}

.featured-dots button {
  min-width: 1.45rem;
  min-height: 1.8rem;
  border: 0;
  background: transparent;

  span {
    width: 0.42rem;
    height: 0.42rem;
    transform: rotate(45deg);
    border: 1px solid #c8a573;
  }

  &[aria-current='true'] span { background: #ffdc9a; }
}

@media (max-width: 46rem) {
  .featured-controls { padding: 0.38rem 0.45rem; }
  .featured-arrows, .featured-dots { gap: 0.18rem; }
  .featured-arrows button { min-width: 2.45rem; min-height: 2.45rem; font-size: 1.05rem; }
  .featured-dots button { min-width: 1.4rem; min-height: 1.8rem; }
  .featured-caption { padding: 2.2rem 0.85rem 0.8rem; }
  .featured-caption strong { font-size: clamp(1.45rem, 6.2vw, 2.15rem); }
  .featured-caption > span { font-size: 0.9rem; line-height: 1.45; }
}

@media (prefers-reduced-motion: reduce) {
  .featured-track.is-animated { transition: none; }
}
</style>
