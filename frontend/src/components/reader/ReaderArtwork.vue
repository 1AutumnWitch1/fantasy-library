<script setup lang="ts">
import type { Artwork } from '../../reader/bookTypes'

defineProps<{ artwork: Artwork }>()
</script>

<template>
  <div class="reader-art" aria-hidden="true">
    <picture v-if="artwork.tile === undefined">
      <source v-if="artwork.portrait" :srcset="artwork.portrait" media="(max-width: 46rem)" />
      <img :src="artwork.src" alt="" draggable="false" decoding="async"
        @error="($event.target as HTMLImageElement).hidden = true" />
    </picture>
    <span v-else class="reader-art__tile" :style="{
      backgroundImage: 'url(' + artwork.src + ')',
      backgroundPosition: (artwork.tile % 3) * 50 + '% ' + Math.floor(artwork.tile / 3) * 100 + '%',
    }"></span>
  </div>
</template>

<style scoped lang="scss">
.reader-art {
  position: absolute;
  inset: 0;
  container-type: size;
  overflow: hidden;
  background: radial-gradient(ellipse at 64% 34%, #637061, #29352c 49%, #1d231e);
}

img[hidden] { display: none; }

img, .reader-art__tile {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.reader-art__tile {
  position: absolute;
  top: 50%;
  left: 50%;
  width: max(100cqw, 100cqh);
  height: max(100cqw, 100cqh);
  transform: translate(-50%, -50%);
  background-size: 300% 200%;
  background-repeat: no-repeat;
}
</style>
