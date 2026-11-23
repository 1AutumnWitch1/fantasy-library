<script setup lang="ts">
import type { BookDefinition } from '../../reader/bookTypes'
import BookSigil from '../shelf/BookSigil.vue'

defineProps<{ book: BookDefinition }>()
</script>

<template>
  <div class="reader-cover" :class="'reader-cover--' + book.spine.binding" aria-hidden="true">
    <div class="reader-cover__frame">
      <div class="reader-cover__seal"><BookSigil :kind="book.spine.emblem" /></div>
      <span class="reader-cover__title">{{ book.spine.title }}</span>
      <span class="reader-cover__edition">{{ book.coverTitle }}</span>
      <span class="reader-cover__rule"></span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.reader-cover {
  position: relative;
  isolation: isolate;
  display: grid;
  place-items: center;
  inline-size: 100%;
  block-size: 100%;
  overflow: hidden;
  border: 1px solid var(--brass-frame);
  border-radius: 0.46rem 0.8rem 0.8rem 0.46rem;
  color: #e5c284;
  background:
    linear-gradient(90deg, var(--binding-shade) 0%, #fbce963b 3%, transparent 10%, transparent 78%, #10090d9c 100%),
    radial-gradient(ellipse at 46% 30%, var(--binding-light), var(--binding-leather) 53%, var(--binding-shade));
  box-shadow: inset 2px 2px 3px #ffc98b50, inset -4px -3px 9px #16060dcc, 0 5px 17px #0a0505b3;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    z-index: -1;
    background: var(--leather-grain) center / 14rem auto;
    mix-blend-mode: soft-light;
    opacity: 0.65;
  }
}

.reader-cover__frame {
  width: 80%;
  height: 84%;
  display: grid;
  align-content: center;
  justify-items: center;
  row-gap: clamp(0.4rem, 2vmin, 1rem);
  border: 1px solid #d2a470be;
  border-radius: var(--frame-radius, 46% 46% 2% 2% / 14% 14% 2% 2%);
  outline: 1px solid #d2a4706b;
  outline-offset: -5px;
  box-shadow: 0 0 0 2px var(--binding-shade), inset 0 0 2rem #1412103f;
}

.reader-cover__seal {
  display: grid;
  place-items: center;
  width: min(40%, 7rem);
  aspect-ratio: 1;
  border-radius: 50%;
  border: 1px solid #d2a470ad;
  box-shadow: 0 0 0 3px #19060d50, inset 0 0 0 3px #e3b77740;
  font-size: clamp(1.25rem, 5vmin, 4rem);
}

.reader-cover__title {
  color: var(--foil);
  font-family: var(--display-font);
  font-size: clamp(1.35rem, 5.2vmin, 3.7rem);
  font-weight: 700;
  letter-spacing: 0.18em;
  text-indent: 0.18em;
  text-shadow: 0 2px var(--binding-shade);
}

.reader-cover--pine { --frame-radius: 9% 9% 3% 3% / 6% 6% 3% 3%; }
.reader-cover--indigo { --frame-radius: 6% 6% 48% 48% / 3% 3% 13% 13%; }
.reader-cover--ochre { --frame-radius: 48% 48% 48% 48% / 10% 10% 10% 10%; }
.reader-cover--plum { --frame-radius: 24% 24% 24% 24% / 12% 12% 12% 12%; }

.reader-cover__edition {
  font-size: clamp(0.65rem, 2vmin, 1.1rem);
  letter-spacing: 0.15em;
}

.reader-cover__rule {
  width: 40%;
  height: 1px;
  background: linear-gradient(90deg, transparent, #e4bf84, transparent);
}
</style>
