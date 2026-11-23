<script setup lang="ts">
import type { RecommendationPage } from '../../reader/bookTypes'
import type { RecommendationMode } from '../../api/recommendations'
import PostPreview from '../community/PostPreview.vue'
import RecommendationWorkCard from './RecommendationWorkCard.vue'

defineProps<{ page: RecommendationPage; interactive: boolean }>()
const headings: Record<RecommendationMode, string> = {
  PERSONALIZED: '根据你的收藏与追番状态', POPULAR: '馆友热门', EDITORIAL: '编辑精选', EMPTY: '',
}
</script>

<template>
  <div class="reader-content recommendation-page">
    <p v-if="page.status === 'loading'" class="reco-state">正在调阅荐读目录…</p>
    <p v-else-if="page.status === 'error'" class="reco-state">推荐书暂时无法调阅。</p>
    <template v-else-if="page.section === 'picks'">
      <div v-if="page.mode !== 'EMPTY' || page.guest" class="picks-intro">
        <p v-if="page.mode !== 'EMPTY'">{{ headings[page.mode] }}</p>
        <RouterLink v-if="page.guest && interactive"
          :to="{ name: 'account', query: { next: '/books/recommendations' } }">登录查看个性推荐 ›</RouterLink>
      </div>
      <ul v-if="page.items.length" class="reco-grid">
        <li v-for="pick in page.items" :key="pick.entry.work.id">
          <RecommendationWorkCard :entry="pick.entry" :reason="pick.reason" :interactive="interactive" />
        </li>
      </ul>
      <p v-else class="reco-state">这个分类下暂时没有推荐作品。</p>
    </template>
    <template v-else-if="page.section === 'popular'">
      <ol v-if="page.items.length" class="reco-ranks">
        <li v-for="(item, index) in page.items" :key="item.entry.work.id">
          <RecommendationWorkCard :entry="item.entry" :heat="item.heat" :rank="index + 1" :interactive="interactive" />
        </li>
      </ol>
      <p v-else class="reco-state">这个分类下还没有作品互动，榜单暂时为空。</p>
    </template>
    <template v-else-if="page.section === 'newest'">
      <ul v-if="page.items.length" class="reco-grid">
        <li v-for="entry in page.items" :key="entry.work.id">
          <RecommendationWorkCard :entry="entry" :interactive="interactive" newest />
        </li>
      </ul>
      <p v-else class="reco-state">这个分类下还没有收录作品。</p>
    </template>
    <template v-else>
      <ul v-if="page.items.length" class="reco-discussions">
        <li v-for="post in page.items" :key="post.id">
          <PostPreview :post="post" :interactive="interactive" from-recommendations />
        </li>
      </ul>
      <p v-else class="reco-state">近 14 天没有活跃讨论。</p>
    </template>
  </div>
</template>

<style scoped lang="scss">
.recommendation-page {
  --reco-gap: clamp(.42rem, 1vw, .95rem);
  padding: .05rem .14rem .25rem .06rem;
}

.picks-intro {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: .2rem .65rem;
  margin: 0 0 .6rem;
  color: #6e482b;
  font-size: .86em;

  p { margin: 0; }
  a { color: #2b473a; }
}

.reco-grid, .reco-ranks, .reco-discussions { margin: 0; padding: 0; list-style: none; }
.reco-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--reco-gap); }
.reco-ranks { display: grid; gap: .28rem; }
.reco-discussions { display: grid; gap: calc(var(--reco-gap) * .65); }
.reco-state { margin: .2rem 0 0; color: #704e30; }

@media (max-width: 23rem) {
  .reco-grid { grid-template-columns: minmax(0, 1fr); }
  .reco-discussions :deep(.preview-labels) { flex-wrap: wrap; }
  .reco-discussions :deep(.preview-excerpt img) { display: none; }
}
</style>
