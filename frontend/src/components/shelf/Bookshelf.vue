<script setup lang="ts">
import type { ShelfBook, ShelfBookId } from '../../data/shelfBooks'
import BookSpine from './BookSpine.vue'

defineProps<{
  books: readonly ShelfBook[]
  selectedId: ShelfBookId | null
  liftedId?: ShelfBookId
}>()

const emit = defineEmits<{ select: [id: ShelfBookId, source: HTMLButtonElement] }>()

function selectBook(id: ShelfBookId, source: HTMLButtonElement) {
  emit('select', id, source)
}
</script>

<template>
  <section class="shelf" aria-label="馆藏分类">
    <div class="shelf-crown" aria-hidden="true">
      <span class="shelf-crown__inlay"><span></span></span>
    </div>
    <div class="shelf-frame">
      <div class="shelf-pillar" aria-hidden="true"></div>
      <div class="shelf-bay">
        <BookSpine
          v-for="book in books"
          :key="book.id"
          :book="book"
          :selected="book.id === selectedId"
          :lifted="book.id === liftedId"
          @select="selectBook"
        />
      </div>
      <div class="shelf-pillar" aria-hidden="true"></div>
    </div>
    <div class="shelf-base" aria-hidden="true"></div>
  </section>
</template>

<style scoped lang="scss">
.shelf {
  --pillar-width: clamp(1.15rem, 4.3vw, 3.75rem);
  --book-height: clamp(13rem, 26.5vw, 20.75rem);
  --cavity-padding: clamp(0.6rem, 2.8vw, 2.3rem);
  --cavity-headroom: clamp(1.35rem, 3.4vw, 2.85rem);
  --wood: var(--walnut-grain, none);
  position: relative;
  width: calc(100% - 1.25rem);
  max-width: 66rem;
  margin-inline: auto;
  filter: drop-shadow(0 24px 26px #060302db);
}

.shelf-crown {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  height: clamp(2.45rem, 4.5vw, 3.9rem);
  margin-inline: -0.65rem;
  background:
    linear-gradient(to bottom, #e5b26f3d, #482a1914 18%, #311a111a 52%, #090504e6 95%),
    var(--wood) center 42% / 65rem auto,
    #462919;
  box-shadow:
    inset 0 2px #c28e548a,
    inset 0 -5px #140b07,
    0 6px 12px #080402cc;
  border: 1px solid #65422b;
  border-bottom-color: #a67549;
  border-radius: 0.55rem 0.55rem 0.14rem 0.14rem;

  &::before {
    content: '';
    position: absolute;
    inset: 17% 2.5% 22%;
    border: 1px solid #be8e5e80;
    border-radius: 3px;
    box-shadow: 0 1px #1e110a, inset 0 1px #1e110a;
  }
}

.shelf-crown__inlay, .shelf-crown__inlay::before, .shelf-crown__inlay::after {
  display: block;
  background: linear-gradient(90deg, transparent, #b68b53, #f2cf94, #b68b53, transparent);
}

.shelf-crown__inlay {
  position: relative;
  width: clamp(3.9rem, 11.5vw, 8.6rem);
  height: 1px;

  &::before, &::after {
    content: '';
    position: absolute;
    inset-inline: 14%;
    height: 1px;
  }

  &::before { top: -3px; }
  &::after { top: 3px; }

  > span {
    position: absolute;
    z-index: 1;
    top: -0.31rem;
    left: calc(50% - 0.31rem);
    width: 0.62rem;
    height: 0.62rem;
    transform: rotate(45deg);
    background: linear-gradient(135deg, #f1c882, #8d653c);
    border: 1px solid #ecc48d;
    box-shadow: 0 0 0 3px #40241a;
  }
}

.shelf-frame {
  display: grid;
  grid-template-columns: var(--pillar-width) minmax(0, 1fr) var(--pillar-width);
  isolation: isolate;
  background:
    radial-gradient(ellipse 75% 48% at 50% 10%, #f0b86b24 0%, transparent 72%),
    linear-gradient(90deg, #0e0907 0%, #21120c6e 18%, transparent 50%, #21120c6e 82%, #0e0907),
    radial-gradient(ellipse at 50% 34%, #3c1c0d36, #0b0605e3 94%),
    var(--wood) center 53% / 60rem auto,
    #24160f;
  box-shadow: 0 0 0 1px #4c2d1d;
}

.shelf-pillar {
  position: relative;
  z-index: 2;
  background:
    linear-gradient(90deg, #160d09, #8d5e38b0 14%, #5a351f22 30%, #26150d85 79%, #110906),
    var(--wood) 14% center / 18rem auto,
    #573321;
  box-shadow: 0 0 9px #070402d9, inset 1px 0 #d5985b57;
  border-inline: 1px solid #6b4128;

  &::before {
    content: '';
    position: absolute;
    inset: 0.65rem 21% 0.55rem;
    border: 1px solid #b68b5399;
    border-radius: 44% 44% 40% 40% / 2% 2% 2% 2%;
    box-shadow: inset 0 0 0 2px #260d06ad, 1px 0 #130a07;
  }

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
      linear-gradient(90deg, #291607, #744d32a8 20%, #c2885766 35%, #2d160bc9),
      linear-gradient(#8b5c3b, #442718);
    background-size: 100% clamp(0.32rem, 1.15vw, 0.78rem);
    background-position: center top, center bottom;
    background-repeat: no-repeat;
  }
}

.shelf-bay {
  position: relative;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: end;
  column-gap: clamp(0.35rem, 1.35vw, 1.25rem);
  padding: var(--cavity-headroom) var(--cavity-padding) 0.14rem;
  box-shadow:
    inset 0 14px 22px #090503f0,
    inset 0 -16px 26px #0b0604bf;

  &::before {
    content: '';
    position: absolute;
    inset: auto 0 0;
    height: 0.32rem;
    background: linear-gradient(to bottom, #160c07, #5e3b24 45%, #2b180e);
    border-top: 1px solid #9e71464d;
    pointer-events: none;
  }
}

.shelf-base {
  position: relative;
  z-index: 1;
  height: clamp(1.7rem, 3.6vw, 3rem);
  margin-inline: -0.65rem;
  border: 1px solid #633e26;
  border-top-color: #b17f52;
  border-radius: 0.12rem 0.12rem 0.6rem 0.6rem;
  box-shadow:
    inset 0 3px #cc976480,
    inset 0 6px #5a3a26,
    inset 0 -5px #160b07,
    0 5px 0 #1b0f09;
  background:
    linear-gradient(#d49d564f, #361e1078 27%, #160b0766 66%, #160b07f0),
    var(--wood) center 61% / 65rem auto,
    #45281a;

  &::after {
    content: '';
    position: absolute;
    inset: 49% 3% auto;
    height: 1px;
    background: linear-gradient(90deg, transparent, #c7985d82 10%, #c7985d82 90%, transparent);
    box-shadow: 0 1px #17100d;
  }
}

@media (max-width: 42rem) {
  .shelf {
    --book-height: clamp(7.4rem, 39vw, 12.25rem);
    --cavity-headroom: 1.2rem;
    --cavity-padding: clamp(0.45rem, 2.4vw, 1.1rem);
    width: calc(100% - 0.85rem);
  }

  .shelf-crown, .shelf-base { margin-inline: -0.35rem; }

  .shelf-bay {
    --row-height: calc(var(--book-height) * 1.055);
    --divider-height: 1.15rem;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    grid-auto-rows: var(--row-height);
    gap: var(--divider-height) 0.42rem;

    &::after {
      content: '';
      position: absolute;
      z-index: 1;
      top: calc(var(--cavity-headroom) + var(--row-height));
      inset-inline: 0;
      height: var(--divider-height);
      border-block: 1px solid #845b3d;
      background:
        linear-gradient(#d29e607d, #2a150b8f 44%, #160b07e6),
        var(--wood) center 39% / 28rem auto,
        #4a2b1c;
      box-shadow: inset 0 2px #b6855c6e, 0 6px 12px #0e0806d9;
      pointer-events: none;
    }
  }

  .shelf-bay > * { grid-column: span 2; }
  .shelf-bay > :nth-child(4) { grid-column: 2 / span 2; }
  .shelf-bay > :nth-child(5) { grid-column: 4 / span 2; }
}
</style>
