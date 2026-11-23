<script setup lang="ts">
import { ref } from 'vue'
import type { BookTurn } from '../../reader/turnPlan'
import BookPage from './BookPage.vue'

defineProps<{
  turn: BookTurn
  slideIndexes: Readonly<Record<number, number>>
  reduced: boolean
  paused: boolean
  search: string
}>()
const element = ref<HTMLElement | null>(null)

function surfaces() {
  return element.value ? [...element.value.querySelectorAll<HTMLElement>('.leaf__shade')] : []
}

defineExpose({ element, surfaces })
</script>

<template>
  <div ref="element" class="leaf" :class="'leaf--' + turn.direction" aria-hidden="true" data-turning-leaf>
    <div class="leaf__face leaf__front">
      <BookPage :page="turn.front" :side="turn.direction === 'forward' ? 'right' : 'left'"
        :slide-index="slideIndexes[turn.front.number]" :reduced="reduced" :paused="paused" :search="search" />
      <span class="leaf__shade"></span>
    </div>
    <div class="leaf__face leaf__back">
      <BookPage :page="turn.back" :side="turn.direction === 'forward' ? 'left' : 'right'"
        :slide-index="slideIndexes[turn.back.number]" :reduced="reduced" :paused="paused" :search="search" />
      <span class="leaf__shade"></span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.leaf {
  position: absolute;
  inset-block: 0;
  width: 50%;
  transform-style: preserve-3d;
  will-change: transform;
  pointer-events: none;
}

.leaf--forward { left: 50%; transform-origin: left center; }
.leaf--backward { left: 0; transform-origin: right center; }

.leaf__face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  box-shadow: 0.14rem 0.1rem 0.22rem #5a392966;
}

.leaf__front { transform: translateZ(0.2px); }
.leaf__back { transform: rotateY(180deg) translateZ(0.2px); }

.leaf__shade {
  position: absolute;
  inset: 0;
  opacity: 0;
  background: linear-gradient(90deg, #4c2f24a6, #76513005 80%);
}

.leaf--forward .leaf__back .leaf__shade,
.leaf--backward .leaf__front .leaf__shade {
  background: linear-gradient(270deg, #4c2f24a6, #76513005 80%);
}
</style>
