<script setup lang="ts">
import { computed } from 'vue'
import { coverPreview } from '../../reader/coverPreview'
import type { RecommendationWork, WorkHeat } from '../../api/recommendations'
import BookSigil from '../shelf/BookSigil.vue'

const props = defineProps<{ entry: RecommendationWork; interactive: boolean; reason?: string; heat?: WorkHeat; rank?: number; newest?: boolean }>()
const intake = computed(() => props.entry.addedAt
  ? new Date(props.entry.addedAt).toLocaleDateString('zh-CN')
  : null)
const stats = computed(() => {
  const heat = props.heat
  if (!heat) return ''
  return [heat.favoriteCount && heat.favoriteCount + ' 收藏', heat.wishlistCount && heat.wishlistCount + ' 愿望单',
    heat.commentCount && heat.commentCount + ' 评论', heat.likeCount && heat.likeCount + ' 赞',
    heat.viewCount && heat.viewCount + ' 浏览'].filter(Boolean).join(' · ')
})
</script>

<template>
  <article class="reco-card" :class="{ 'reco-card--rank': heat }">
    <span v-if="rank" class="rank-marker" aria-hidden="true">{{ String(rank).padStart(2, '0') }}</span>
    <div class="reco-cover">
      <BookSigil :kind="entry.work.type === 'ANIME' ? 'moon' : 'swords'" aria-hidden="true" />
      <img v-if="entry.work.cover" :key="entry.work.cover.url" :src="coverPreview(entry.work.cover.url)" loading="lazy" decoding="async" :alt="entry.work.cover.altText"
        @error="($event.currentTarget as HTMLImageElement).hidden = true">
    </div>
    <div class="reco-caption">
      <p class="reco-category">{{ entry.work.type === 'ANIME' ? '追番' : '游戏' }} · {{ entry.work.category.name }}</p>
      <h4>
        <RouterLink v-if="interactive" :to="{ name: 'work-detail', params: { id: entry.work.id }, query: { from: 'recommendations' } }">
          {{ entry.work.title }}
        </RouterLink>
        <span v-else>{{ entry.work.title }}</span>
      </h4>
      <p v-if="reason" class="reco-reason">{{ reason }}</p>
      <template v-if="heat">
        <p class="reco-heat"><strong>{{ heat.score }}</strong> 热度</p>
        <p v-if="stats" class="reco-meta">{{ stats }}</p>
      </template>
      <template v-if="newest">
        <p v-if="intake" class="reco-meta">收录于 {{ intake }}</p>
        <p v-else-if="entry.work.releaseDate" class="reco-meta">首发于 {{ entry.work.releaseDate }}</p>
        <p v-if="entry.tags.length" class="reco-meta">{{ entry.tags.slice(0, 3).map(tag => tag.name).join(' · ') }}</p>
      </template>
    </div>
  </article>
</template>

<style scoped lang="scss">
.reco-card {
  position: relative;
  display: grid;
  grid-template-rows: auto 1fr;
  height: 100%;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #8b5a345e;
  border-radius: .22rem;
  background: #fff0c548;
  box-shadow: 0 .22rem .65rem #5f361019;
}

.reco-cover {
  position: relative;
  display: grid;
  place-items: center;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-bottom: 1px solid #86593160;
  background: radial-gradient(ellipse at 50% 42%, #506454, #252c24);
  color: #e9c184;

  :deep(.sigil) { width: 2em; height: 2em; }
  img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; object-position: 50% 45%; }
  img[hidden] { display: none; }
}

.reco-caption {
  min-width: 0;
  padding: clamp(.4rem, .84vw, .75rem);
  overflow-wrap: anywhere;

  h4 { font-size: clamp(.91rem, 1.62vw, 1.48rem); margin: 0; }
  p { margin: 0; }
  a { color: #294538; text-decoration: none; }
  a::after { content: ''; position: absolute; inset: 0; }
  a:hover, a:focus-visible { text-decoration: underline; }
}

.reco-category { font-size: .76em; color: #6e482b; }
.reco-reason, .reco-meta { padding-top: .22em; font-size: .84em; }
.reco-reason { color: #4f3825; }
.reco-heat { align-self: center; color: #65361e; font-size: .86em; }
.rank-marker { color: #714528; font: 700 clamp(.7rem, 1.5vw, 1.5rem) Georgia, serif; text-align: center; }

.reco-card--rank {
  grid-template: minmax(0, 1fr) / 2.05em minmax(3rem, 23%) minmax(0, 1fr);
  align-items: center;
  padding: .28em .38em;
  border-width: 0 0 1px;
  border-radius: 0;
  background: transparent;
  box-shadow: none;

  .reco-cover { aspect-ratio: 4 / 3; border: 1px solid #86593160; border-radius: .16rem; }
  .reco-caption { padding: .22em 0 .22em .72em; }
}

@media (max-width: 46rem) {
  .reco-card--rank {
    grid-template-columns: 1.45em minmax(2.8rem, 21%) minmax(0, 1fr);
    .reco-caption { padding-left: .32em; }
  }
}
</style>
