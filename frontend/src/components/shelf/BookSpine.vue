<script setup lang="ts">
import type { ShelfBook } from '../../data/shelfBooks'
import BookSigil from './BookSigil.vue'

const props = defineProps<{ book: ShelfBook; selected: boolean; lifted?: boolean }>()
const emit = defineEmits<{ select: [id: ShelfBook['id'], source: HTMLButtonElement] }>()

function handleClick(event: MouseEvent) {
  if (event.currentTarget instanceof HTMLButtonElement) {
    emit('select', props.book.id, event.currentTarget)
  }
}
</script>

<template>
  <button
    type="button"
    class="book-spine"
    :class="['book-spine--' + book.binding, { 'is-lifted': lifted }]"
    :aria-pressed="selected"
    :aria-haspopup="book.id === 'anime' ? 'dialog' : undefined"
    :data-book-id="book.id"
    @click="handleClick"
  >
    <span class="spine-fillet" aria-hidden="true"></span>
    <span class="spine-rib spine-rib--head" aria-hidden="true"></span>
    <span class="spine-rib spine-rib--tail" aria-hidden="true"></span>
    <span class="spine-label">
      <span class="spine-emblem"><BookSigil :kind="book.emblem" /></span>
      <span class="spine-title">{{ book.title }}</span>
      <span class="spine-flourish" aria-hidden="true"></span>
    </span>
  </button>
</template>

<style scoped lang="scss">
.book-spine {
  --foil: #efd196;
  position: relative;
  isolation: isolate;
  display: grid;
  place-items: center;
  block-size: calc(var(--book-height, 19rem) * var(--binding-scale, 1));
  inline-size: min(100%, var(--binding-width, 9.65rem));
  justify-self: center;
  padding: 0;
  border: 1px solid #b68b53a8;
  border-right-color: #100907;
  border-radius: 0.52rem 0.34rem 0.32rem 0.52rem / 0.92rem 0.68rem 0.56rem 0.68rem;
  background:
    linear-gradient(90deg, #070403cf 0%, #ffe4b030 9%, transparent 26%, transparent 65%, #0c070559 88%, #050303d6 100%),
    radial-gradient(ellipse at 42% 16%, #fae0ae33, transparent 62%),
    linear-gradient(var(--binding-light), var(--binding-dark));
  box-shadow:
    inset 3px 0 3px #ffe8b830,
    inset -4px -3px 6px #05030391,
    3px 10px 8px #070403e3,
    9px 12px 20px #090503c7;
  color: var(--foil);
  font: inherit;
  cursor: pointer;
  transition: transform 240ms cubic-bezier(0.22, 1, 0.36, 1), box-shadow 240ms ease, filter 240ms ease;

  &::before {
    content: '';
    position: absolute;
    z-index: -1;
    inset: 0;
    border-radius: inherit;
    background: var(--leather-grain, none) center / 12rem auto;
    mix-blend-mode: soft-light;
    opacity: 0.72;
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    inset: auto 18% -0.45rem;
    height: 3px;
    border-radius: 99px;
    background: transparent;
    transition: background-color 200ms ease, box-shadow 200ms ease;
    pointer-events: none;
  }

  &:focus-visible {
    z-index: 3;
    transform: translateY(-0.75rem) scale(1.03);
    filter: brightness(1.16);
    outline: 2px solid #f6d79d;
    outline-offset: 4px;
    box-shadow:
      inset 3px 0 4px #ffe7b55c,
      0 0 0 1px #d9b174,
      0 14px 20px #080403e0,
      0 0 18px #e8b3663d;

    &::after {
      background: #f3cf8d;
      box-shadow: 0 0 10px #f3cf8d99;
    }
  }

  @media (hover: hover) {
    &:hover {
      z-index: 2;
      transform: translateY(-0.68rem) scale(1.028);
      filter: brightness(1.15);
      box-shadow:
        inset 3px 0 4px #ffe7b557,
        0 14px 14px #090503d4,
        0 18px 28px #090503c2,
        0 0 14px #e8b36626;
    }
  }

  &[aria-pressed='true'] {
    z-index: 2;
    transform: translateY(-0.48rem);
    filter: brightness(1.12);
    box-shadow:
      inset 2px 0 3px #ffe7b557,
      0 0 0 1px #d4ab70,
      0 12px 18px #090503d9;

    &::after {
      background: #cfa262;
    }
  }
}

.book-spine.is-lifted { opacity: 0; pointer-events: none; }

/* Distinct leather tones, spine widths and heights across the 5 volumes */
.book-spine--claret { --binding-light: #7c313b; --binding-dark: #411822; --binding-scale: 0.95; --binding-width: 9.05rem; }
.book-spine--pine { --binding-light: #2b4a3d; --binding-dark: #152b23; --binding-scale: 1.04; --binding-width: 10.1rem; }
.book-spine--indigo { --binding-light: #334a6b; --binding-dark: #1b243c; --binding-scale: 0.97; --binding-width: 9.3rem; }
.book-spine--ochre { --binding-light: #7c5834; --binding-dark: #482f1b; --binding-scale: 1.01; --binding-width: 9.55rem; }
.book-spine--plum { --binding-light: #5c3b5b; --binding-dark: #2f2030; --binding-scale: 0.91; --binding-width: 10.3rem; }

.spine-fillet {
  position: absolute;
  inset: 7% 10%;
  border: 1px solid #d6a76a85;
  border-radius: 2px;
  box-shadow: inset 0 0 0 3px #150b0942, 0 0 0 1px #200e0b53;
}

.spine-rib {
  position: absolute;
  inset-inline: 2%;
  height: 4.5%;
  border-top: 1px solid #e1b779a6;
  border-bottom: 1px solid #070404c2;
  border-radius: 20% / 35%;
  background: linear-gradient(#170b084d, #f1c17d38 41%, #170b0852 64%, #120906a6);
  box-shadow: 0 -3px 0 -2px #bf956689, 0 3px 0 -2px #b78c5682;
}

.spine-rib--head { top: 13%; }
.spine-rib--tail { bottom: 13%; }

.spine-label {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: clamp(0.36rem, 1.2vw, 0.8rem);
  width: 77%;
  padding-block: 0.28rem;
  transform: translateY(-3%);
  text-align: center;
  text-shadow: 0 -1px #fff1bd38, 0 2px #110704cc;
}

.spine-emblem {
  display: grid;
  place-items: center;
  aspect-ratio: 1;
  border: 1px solid #d8ab7090;
  outline: 1px solid #382515a3;
  outline-offset: 3px;
  inline-size: min(57%, 4.1rem);
  font-size: clamp(1.16rem, 3.72vw, 2.7rem);
  box-shadow: inset 0 0 0 2px #e7b87c1e, 0 2px 5px #1408056b;
  border-radius: 48%;
  background: radial-gradient(circle at 45% 30%, #fde1a522, #190d0845);
  transition: color 220ms ease;
}

.book-spine--claret .spine-emblem { border-radius: 50% 50% 38% 38%; }
.book-spine--pine .spine-emblem { border-radius: 2px 2px 39% 39%; }
.book-spine--indigo .spine-emblem { border-radius: 42% 42% 13% 13%; }
.book-spine--ochre .spine-emblem { border-radius: 19% 19% 19% 19%; }
.book-spine--plum .spine-emblem { border-radius: 72% 72% 36% 36%; }

.spine-title {
  color: var(--foil);
  font-family: var(--display-font);
  font-size: clamp(1.04rem, 2.4vw, 1.7rem);
  font-weight: 700;
  letter-spacing: 0.075em;
  line-height: 1.35;
  white-space: nowrap;
}

.book-spine--plum .spine-title { font-size: clamp(0.77rem, 1.65vw, 1.22rem); }

.spine-flourish {
  position: relative;
  width: 61%;
  height: 0.45rem;
  background: linear-gradient(90deg, transparent, #ceaa72 42%, #ceaa72 58%, transparent) center / 100% 1px no-repeat;

  &::after {
    content: '';
    position: absolute;
    left: calc(50% - 0.17rem);
    top: calc(50% - 0.17rem);
    width: 0.34rem;
    height: 0.34rem;
    transform: rotate(45deg);
    background: #cfaa72;
  }
}

@media (max-width: 42rem) {
  .spine-title { font-size: clamp(0.98rem, 4.05vw, 1.35rem); }
  .book-spine--plum .spine-title { font-size: clamp(0.74rem, 3.05vw, 1.05rem); }
  .spine-emblem { font-size: clamp(1.18rem, 6.5vw, 2.12rem); }
}

@media (prefers-reduced-motion: reduce) {
  .book-spine, .spine-emblem, .book-spine::after { transition: none; }
  .book-spine:hover, .book-spine:focus-visible, .book-spine[aria-pressed='true'] { transform: none; }
}
</style>
