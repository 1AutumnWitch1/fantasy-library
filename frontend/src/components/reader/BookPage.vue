<script setup lang="ts">
import { computed } from 'vue'
import type { BookPageData } from '../../reader/bookTypes'
import FeaturedCarousel from './FeaturedCarousel.vue'
import CatalogEntries from './CatalogEntries.vue'
import GameCatalogue from './GameCatalogue.vue'
import CommunityBookPage from './CommunityBookPage.vue'
import PersonalBookPage from './PersonalBookPage.vue'
import ReaderCollections from './ReaderCollections.vue'
import RecommendationBookPage from './RecommendationBookPage.vue'
import ReaderNotes from './ReaderNotes.vue'
import ReaderSearch from './ReaderSearch.vue'
import VideoRecommendations from './VideoRecommendations.vue'

const props = withDefaults(defineProps<{
  page: BookPageData
  side: 'left' | 'right'
  slideIndex?: number
  interactive?: boolean
  reduced?: boolean
  paused?: boolean
  search?: string
  marking?: boolean
  single?: boolean
}>(), { slideIndex: 0, interactive: false, reduced: false, paused: false, search: '', marking: false, single: false })

const emit = defineEmits<{ select: [index: number]; hold: [value: boolean]; togglePause: []; search: [query: string]; markRead: [id?: number] }>()
const title = computed(() => {
  const { page } = props
  if (page.kind === 'featured') return page.title ?? '精选推荐'
  if (page.kind === 'videos') return '视频推荐'
  return page.title
})
const catalogSearch = computed(() => props.page.kind === 'catalogue' && props.side === 'right' && !props.single)
const searchQuery = computed({ get: () => props.search, set: (query: string) => emit('search', query) })
</script>

<template>
  <article class="reader-page" :class="['reader-page--' + side, { 'reader-page--single': single, 'reader-page--anime': page.kind === 'personal' && page.section === 'anime', 'reader-page--game': page.kind === 'game' || page.kind === 'catalogue' }]" :data-page-number="page.number">
    <div class="reader-page__heading" :class="{ 'reader-page__heading--search': (page.kind === 'videos' && !single) || catalogSearch }">
      <h3>{{ title }}</h3>
      <ReaderSearch v-if="catalogSearch" v-model="searchQuery" label="搜索作品" placeholder="搜作品名称或简介" :disabled="!interactive" />
      <ReaderSearch v-else-if="page.kind === 'videos' && !single" v-model="searchQuery" :disabled="!interactive" />
    </div>

    <FeaturedCarousel v-if="page.kind === 'featured'" :slides="page.slides" :index="Math.min(slideIndex, page.slides.length - 1)"
      :interactive="interactive" :reduced="reduced" :paused="paused" @select="emit('select', $event)"
      @hold="emit('hold', $event)" @toggle-pause="emit('togglePause')" />
    <VideoRecommendations v-else-if="page.kind === 'videos'" :videos="page.videos" :searching="Boolean(search.trim())" />
    <CatalogEntries v-else-if="page.kind === 'catalogue'" :page="page" :interactive="interactive" :single="single" />
    <GameCatalogue v-else-if="page.kind === 'game'" :page="page" />
    <ReaderNotes v-else-if="page.kind === 'notes'" :page="page" />
    <CommunityBookPage v-else-if="page.kind === 'community'" :page="page" :interactive="interactive" />
    <PersonalBookPage v-else-if="page.kind === 'personal'" :page="page" :interactive="interactive"
      :marking="marking" @mark-read="emit('markRead', $event)" />
    <RecommendationBookPage v-else-if="page.kind === 'recommendations'" :page="page" :interactive="interactive" />
    <ReaderCollections v-else :page="page" />

    <span class="page-folio" aria-hidden="true">{{ page.number }}</span>
  </article>
</template>

<style scoped lang="scss">
.reader-page {
  position: relative;
  display: grid;
  grid-template: auto minmax(0, 1fr) / minmax(0, 1fr);
  gap: clamp(0.42rem, 1.35vh, 0.9rem);
  min-width: 0;
  min-height: 0;
  width: 100%;
  height: 100%;
  padding: clamp(0.75rem, 2.1vh, 1.45rem) clamp(0.75rem, 1.7vw, 1.85rem) clamp(1.35rem, 3.1vh, 2.25rem);
  color: var(--paper-ink);
  color-scheme: light;
  background: var(--paper-surface);

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    box-shadow: inset 0 0 1.4rem #95653e3d, inset 0 1px #ffefc5;
    pointer-events: none;
  }
}

.reader-page--left {
  padding-right: clamp(0.9rem, 1.85vw, 1.95rem);
  border-radius: 0.19rem 0.44rem 0.1rem 0.25rem;
  box-shadow: inset -0.55rem 0 1.25rem -0.55rem #523318b0;
}

.reader-page--right {
  padding-left: clamp(0.9rem, 1.85vw, 1.95rem);

  &:where(:not(.reader-page--anime)) {
    gap: clamp(0.35rem, 1vh, 0.68rem);
    padding-right: clamp(0.65rem, 1.3vw, 1.45rem);
  }
  border-radius: 0.44rem 0.19rem 0.25rem 0.1rem;
  box-shadow: inset 0.55rem 0 1.25rem -0.55rem #523318b0;
}

.reader-page--game {
  padding-top: clamp(0.65rem, 1.5vh, 1.15rem);
  gap: clamp(0.35rem, 0.95vh, 0.65rem);
}

.reader-page__heading {
  display: flex;
  align-items: center;
  gap: clamp(0.32rem, 1vw, 0.85rem);

  h3 {
    flex: 0 1 auto;
    margin: 0;
    color: var(--paper-ink);
    overflow-wrap: anywhere;
    font: 700 clamp(0.96rem, 1.9vw, 1.85rem) / 1.2 var(--display-font);
    letter-spacing: 0.08em;
  }

  &::after {
    content: '';
    flex: 1;
    min-width: 1.2rem;
    height: 1px;
    background: linear-gradient(90deg, #84603d9b, #84603d14);
  }
}

.reader-page__heading--search {
  justify-content: space-between;

  &::after { display: none; }
  .reader-search { width: min(58%, 16.5rem); }
}

@media (max-width: 52rem) {
  .reader-page__heading--search {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 0.32rem;

    .reader-search { width: 100%; }
  }
}

.reader-page :deep(.reader-content) {
  --entry-padding: clamp(0.5rem, 1.2vw, 1rem);
  --entry-accent: var(--binding-leather);
  min-width: 0;
  min-height: 0;
  margin: 0;
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: #9e7750 transparent;
  color: var(--paper-body);
  font: clamp(0.84rem, 1.02vw, 1rem) / 1.55 var(--reading-font);
  overscroll-behavior: contain;
  scrollbar-gutter: stable;

  h4 {
    margin-block: 0;
    color: var(--paper-ink);
    font: 700 clamp(0.91rem, 1.55vw, 1.45rem) / 1.25 var(--display-font);
    overflow-wrap: anywhere;
  }

  small { font-size: 0.84em; }
  .entry-label { color: var(--entry-accent); font-size: 0.86em; font-weight: 700; }
}

.page-folio {
  position: absolute;
  bottom: clamp(0.24rem, 1vh, 0.56rem);
  inset-inline: 0;
  color: #724d31;
  text-align: center;
  font-variant-numeric: oldstyle-nums;
  font-size: 0.82rem;
  line-height: 1.15;
}

.reader-page--single {
  padding: 0.85rem clamp(0.82rem, 3.8vw, 1.35rem) 1.6rem;
  gap: 0.68rem;
  border-radius: 0.18rem;
  box-shadow: inset 0.34rem 0 0.95rem -0.64rem #523318a6;

  .reader-page__heading h3 { font-size: clamp(1.22rem, 5.2vw, 1.8rem); letter-spacing: 0.04em; }
  :deep(.reader-content) {
    font-size: 0.935rem;
    line-height: 1.6;
    h4 { font-size: clamp(1.04rem, 4.3vw, 1.38rem); }
  }
}
</style>
