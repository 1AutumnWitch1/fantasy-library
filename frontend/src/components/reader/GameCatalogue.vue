<script setup lang="ts">
import type { GamePage } from '../../reader/bookTypes'
import ReaderArtwork from './ReaderArtwork.vue'

defineProps<{ page: GamePage }>()
</script>

<template>
  <ul v-if="page.entries.length" class="game-catalogue reader-content">
    <li v-for="game in page.entries" :key="game.title" class="game-entry">
      <div class="game-art">
        <ReaderArtwork :artwork="game.artwork" />
        <span class="game-genre">{{ game.genre }}</span>
        <h4 class="game-title">{{ game.title }}</h4>
      </div>
      <div class="game-info">
        <p>{{ game.description }}</p>
        <div class="platforms"><strong>平台</strong> {{ game.platforms.join(' · ') }}</div>
      </div>
    </li>
  </ul>
  <p v-else class="game-empty">
    {{ page.total ? '已到馆藏末页' : page.number === 1 ? '没有匹配的游戏' : '可清空搜索或切换标签' }}
  </p>
</template>

<style scoped lang="scss">
.game-catalogue {
  display: grid;
  grid-auto-rows: minmax(min-content, 1fr);
  gap: clamp(0.3rem, 0.9vh, 0.65rem);
  list-style: none;
  padding: 0 0.08rem 0.1rem;
}

.game-entry {
  display: grid;
  grid-template-rows: minmax(3.5rem, 1fr) auto;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #8967448a;
  border-radius: 0.22rem;
  background: #f7dfb55e;
  box-shadow: 0 0.1rem 0.28rem #61432238;
}

.game-art {
  position: relative;
  isolation: isolate;
  background: #23342d;

  &::after {
    content: '';
    position: absolute;
    inset: 35% 0 0;
    background: linear-gradient(transparent, #0e1b18da);
    pointer-events: none;
  }
}

.game-art :deep(.reader-art__tile) {
  top: 38%;
  transform: translate(-50%, -38%);
}

.game-genre {
  position: absolute;
  z-index: 1;
  top: 0.46rem;
  left: 0.52rem;
  max-width: calc(100% - 1rem);
  padding: 0.12rem 0.43rem;
  border: 1px solid #e6cb8f82;
  border-radius: 0.12rem;
  background: #182c24e8;
  color: #f6dfaa;
  font-size: clamp(0.7rem, 0.87vw, 0.9rem);
}

.game-entry .game-art .game-title {
  position: absolute;
  z-index: 1;
  inset: auto 0.67rem 0.37rem;
  color: #ffedc6;
  font: 700 clamp(1rem, 1.9vw, 1.85rem) / 1.16 var(--display-font);
  text-shadow: 0 1px 4px #132019;
}

.game-info {
  padding: 0.34rem 0.62rem 0.37rem;
  line-height: 1.36;
  p { margin: 0; }
}

.platforms {
  margin-top: 0.15rem;
  font-size: 0.89em;
  overflow-wrap: anywhere;
  strong { color: var(--entry-accent); margin-inline-end: 0.24em; }
}

.game-empty {
  align-self: center;
  text-align: center;
  color: #654728;
}

@media (max-width: 46rem) {
  .game-info { padding: 0.23rem 0.32rem; }
  .game-genre { top: 0.22rem; left: 0.23rem; padding: 0.03rem 0.22rem; font-size: 0.65rem; }
  .game-entry .game-art .game-title { inset-inline: 0.33rem; bottom: 0.26rem; }
}
</style>
