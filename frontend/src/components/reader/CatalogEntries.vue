<script setup lang="ts">
import { coverPreview } from '../../reader/coverPreview'
import { airingLabels } from '../../api/catalog'
import type { CataloguePage } from '../../reader/bookTypes'
defineProps<{ page: CataloguePage; interactive: boolean; single: boolean }>()
</script>

<template>
  <ul v-if="page.items.length" class="game-catalogue reader-content" :class="{ 'anime-catalogue': page.catalog === 'anime', 'game-catalogue--single': single }">
    <li v-for="{ work, anime, game, tags } in page.items" :key="work.id" class="game-entry">
      <div class="game-art">
        <img v-if="work.cover" :key="work.cover.url" :src="coverPreview(work.cover.url)" :alt="work.cover.altText" loading="lazy" decoding="async"
          @error="($event.currentTarget as HTMLImageElement).hidden = true" />
        <span class="game-genre">{{ work.category.name }}</span>
        <h4 v-if="page.catalog === 'anime' || single" class="game-title">{{ work.title }}</h4>
      </div>
      <div class="game-info">
        <h4 v-if="page.catalog === 'games' && !single" class="game-info-title">{{ work.title }}</h4>
        <div v-if="game" class="platforms">{{ game.platforms.map(item => item.name).join(' · ') }}<template v-if="game.developer"> · {{ game.developer }}</template></div>
        <div v-if="anime" class="platforms">{{ anime.studio }} · {{ airingLabels[anime.airingStatus] }}</div>
        <p :title="work.summary">{{ work.summary }}</p>
        <div v-if="tags.length" class="work-tags"><span v-for="tag in tags.slice(0, 3)" :key="tag.id">{{ tag.name }}</span></div>
      </div>
      <RouterLink v-if="interactive" class="work-link" :to="{ name: 'work-detail', params: { id: work.id }, query: { from: page.catalog } }"
        :aria-label="'查看《' + work.title + '》资料'" />
    </li>
  </ul>
  <p v-else class="game-empty" role="status">{{ single ? page.message || '本页暂无其他作品' : page.number % 2 === 1 ? page.message : '' }}</p>
</template>

<style scoped lang="scss">
.game-catalogue {
  display: grid;
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: clamp(0.4rem, 1vh, 0.72rem);
  list-style: none;
  padding: 0 0.08rem 0.1rem;
}

.anime-catalogue {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, 1fr));
}

.game-entry {
  position: relative;
  display: grid;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #9672478f;
  border-radius: 0.24rem;
  background: var(--paper-card);
  box-shadow: 0 0.12rem 0.34rem #5a3c1f2e;
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;

  &:hover, &:has(.work-link:focus-visible) {
    border-color: #8b602f;
    box-shadow: 0 0.25rem 0.65rem #472c1242;
  }
}

/* Desktop anime 2x2: maximize cover area */
.anime-catalogue .game-entry {
  grid-template-rows: minmax(0, 1fr) auto;
}

/* Desktop games (2 per page, 4 per spread): cover beside title, platform & short summary */
.game-catalogue:not(.anime-catalogue):not(.game-catalogue--single) .game-entry {
  grid-template-columns: minmax(7.2rem, 44%) minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr);
}

.game-art {
  position: relative;
  isolation: isolate;
  min-height: 5.5rem;
  background: #22352c;
  overflow: hidden;

  img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: 50% 36%;
  }

  &::after {
    content: '';
    position: absolute;
    inset: 38% 0 0;
    background: linear-gradient(transparent, #0e1b18db);
    pointer-events: none;
  }
}

.game-catalogue:not(.anime-catalogue):not(.game-catalogue--single) .game-art::after {
  inset: 0;
  background: linear-gradient(180deg, #101d173d, transparent 42%, #101d1755);
}

.work-link {
  position: absolute;
  z-index: 2;
  inset: 0;
  border-radius: inherit;

  &:focus-visible {
    outline: 3px solid #8f6025;
    outline-offset: -3px;
  }
}

.game-genre {
  position: absolute;
  z-index: 1;
  top: 0.42rem;
  left: 0.46rem;
  max-width: calc(100% - 0.9rem);
  padding: 0.1rem 0.4rem;
  border: 1px solid #e6cb8f82;
  border-radius: 0.14rem;
  background: #182c24eb;
  color: #f6dfaa;
  font-size: clamp(0.68rem, 0.84vw, 0.86rem);
  line-height: 1.3;
}

.game-entry .game-art .game-title {
  position: absolute;
  z-index: 1;
  inset: auto 0.58rem 0.34rem;
  color: #ffedc6;
  font: 700 clamp(0.95rem, 1.45vw, 1.38rem) / 1.2 var(--display-font);
  text-shadow: 0 1px 4px #101c16;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.game-info {
  display: grid;
  align-content: start;
  gap: 0.2rem;
  padding: 0.42rem 0.58rem 0.46rem;
  line-height: 1.4;
  min-width: 0;

  p {
    margin: 0;
    color: var(--paper-body);
    font-size: 0.9em;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.game-catalogue:not(.anime-catalogue):not(.game-catalogue--single) .game-info {
  padding: clamp(0.5rem, 1.2vw, 0.85rem);
  align-content: center;
  gap: 0.34rem;

  .game-info-title {
    margin: 0;
    color: var(--paper-ink);
    font: 700 clamp(1rem, 1.55vw, 1.42rem) / 1.24 var(--display-font);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  p {
    -webkit-line-clamp: 3;
    line-height: 1.48;
  }
}

.platforms {
  font-size: 0.84em;
  font-weight: 600;
  color: #2c4a3c;
  overflow-wrap: anywhere;
}

.work-tags {
  display: flex;
  gap: 0.26rem;
  flex-wrap: wrap;
  margin-top: 0.1rem;

  span {
    border: 1px solid #8b744d75;
    border-radius: 0.14rem;
    background: #fff4d996;
    color: #533d26;
    padding: 0 0.28rem;
    font-size: 0.78em;
    line-height: 1.45;
  }
}

.game-empty {
  align-self: center;
  text-align: center;
  color: var(--paper-muted);
}

.game-catalogue--single {
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: none;
  grid-auto-rows: max-content;
  align-content: start;
  gap: 0.68rem;
  padding-bottom: 0.4rem;

  &.anime-catalogue {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.55rem;
  }

  .game-entry { grid-template-rows: auto 1fr; }
  .game-art { aspect-ratio: 16 / 9; }
  &.anime-catalogue .game-art { aspect-ratio: 3 / 4; }
  .game-info { padding: 0.44rem 0.52rem 0.54rem; line-height: 1.45; }
  .game-entry .game-art .game-title { inset-inline: 0.46rem; font-size: clamp(0.98rem, 4vw, 1.38rem); }
  &.anime-catalogue .game-genre { font-size: 0.69rem; inset: 0.32rem auto auto 0.32rem; }
}

@media (prefers-reduced-motion: reduce) {
  .game-entry { transition: none; }
}
</style>
