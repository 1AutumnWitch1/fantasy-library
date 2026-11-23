<script setup lang="ts">
import type { BookEmblem } from '../../data/shelfBooks'

defineProps<{ kind: BookEmblem }>()
</script>

<template>
  <span class="sigil" :class="'sigil--' + kind" aria-hidden="true">
    <span></span>
    <span></span>
    <span></span>
  </span>
</template>

<style scoped lang="scss">
.sigil {
  position: relative;
  display: inline-block;
  flex: none;
  width: 1em;
  height: 1em;
  vertical-align: middle;

  > span { position: absolute; display: block; }
}

.sigil--moon {
  > :first-child {
    inset: 15% 19% 16% 12%;
    border-radius: 50%;
    background: currentColor;
    mask-image: radial-gradient(circle at 73% 30%, transparent 54%, #000 56%);
  }

  > :nth-child(2) {
    inset: 8% 6% 64% 67%;
    background: currentColor;
    clip-path: polygon(50% 0, 61% 39%, 100% 50%, 61% 61%, 50% 100%, 39% 61%, 0 50%, 39% 39%);
  }

  > :last-child { inset: 45% 17% 47% 75%; border-radius: 50%; background: currentColor; }
}

.sigil--swords {
  > :first-child, > :nth-child(2) {
    top: 7%;
    left: 38%;
    width: 24%;
    height: 86%;
    background: currentColor;
    clip-path: polygon(50% 0, 75% 17%, 75% 66%, 100% 66%, 100% 76%, 65% 76%, 65% 92%, 80% 92%, 80% 100%, 20% 100%, 20% 92%, 35% 92%, 35% 76%, 0 76%, 0 66%, 25% 66%, 25% 17%);
    transform: rotate(40deg);
  }

  > :nth-child(2) { transform: rotate(-40deg); }
  > :last-child {
    inset: 8% 46% 84%;
    transform: rotate(45deg);
    background: currentColor;
  }
}

.sigil--rings {
  > span {
    width: 51%;
    aspect-ratio: 1;
    border-radius: 50%;
    border: max(1px, 0.027em) solid currentColor;
  }

  > :first-child { top: 11%; left: 24.5%; }
  > :nth-child(2) { top: 40%; left: 9%; }
  > :last-child { top: 40%; right: 9%; }
}

.sigil--compass {
  > :first-child {
    inset: 19%;
    border-radius: 50%;
    border: max(1px, 0.022em) solid currentColor;
  }

  > :nth-child(2) {
    inset: 5%;
    background: currentColor;
    clip-path: polygon(50% 0, 57% 38%, 77% 23%, 62% 43%, 100% 50%, 62% 57%, 77% 77%, 57% 62%, 50% 100%, 43% 62%, 23% 77%, 38% 57%, 0 50%, 38% 43%, 23% 23%, 43% 38%);
  }
}

.sigil--key {
  > :first-child {
    inset: 9% 29% 50%;
    border-radius: 50%;
    border: max(1px, 0.036em) solid currentColor;
  }

  > :nth-child(2) {
    inset: 49% 46% 12%;
    border-radius: 0.05em;
    background: currentColor;
  }

  > :last-child {
    inset: 61% 30% 17% 50%;
    background: currentColor;
    clip-path: polygon(0 0, 100% 0, 100% 31%, 52% 31%, 52% 63%, 90% 63%, 90% 100%, 0 100%);
  }
}
</style>
