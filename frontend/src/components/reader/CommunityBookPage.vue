<script setup lang="ts">
import { useRoute } from 'vue-router'
import { communityLink, topics, type Topic } from '../../api/community'
import type { CommunityPage } from '../../reader/bookTypes'
import PostPreview from '../community/PostPreview.vue'
import { useAccountStore } from '../../stores/account'

defineProps<{ page: CommunityPage; interactive: boolean }>()
const account = useAccountStore()
const route = useRoute()
const filters = Object.entries(topics) as [Topic, string][]
</script>

<template>
  <div v-if="page.number % 2 === 1" class="notice-board reader-content">
    <div class="notice">
      <span aria-hidden="true" class="wax-seal">✧</span>
      <p>{{ page.workId ? '本卷收录与该作品相关的讨论。' : '在这里分享观后感、攻略和馆藏发现。' }}</p>
      <RouterLink v-if="interactive && page.workId" :to="{ name: 'work-detail', params: { id: page.workId } }">查看相关作品 ›</RouterLink>
    </div>
    <div v-if="interactive" class="notice-actions">
      <RouterLink class="write-manuscript" :to="account.profile ? { name: 'community-new', query: { workId: page.workId, topic: page.topic } }
        : { name: 'account', query: { next: route.fullPath } }">
        {{ account.profile ? '撰写手稿' : '登录后发帖' }}
      </RouterLink>
    </div>
    <nav v-if="interactive" class="notice-topics" aria-label="话题分类">
      <RouterLink :to="communityLink(page.workId)" :aria-current-value="!page.topic ? 'page' : 'false'">全部话题</RouterLink>
      <RouterLink v-for="[topic, label] in filters" :key="topic" :to="communityLink(page.workId, topic)"
        :aria-current-value="page.topic === topic ? 'page' : 'false'">{{ label }}</RouterLink>
    </nav>
    <RouterLink v-if="interactive && page.workId" :to="communityLink()">浏览全部讨论 ›</RouterLink>
    <section class="popular-shelf" aria-label="热门手稿">
      <h4 class="popular-title">热门手稿</h4>
      <ol v-if="page.items.length" class="popular-posts">
        <li v-for="post in page.items" :key="post.id">
          <PostPreview :post="post" :interactive="interactive" :work-id="page.workId" :filter="page.topic" compact />
        </li>
      </ol>
      <p v-else class="popular-message">{{ page.message }}</p>
    </section>
  </div>
  <ol v-else-if="page.items.length" class="post-index reader-content">
    <li v-for="post in page.items" :key="post.id">
      <PostPreview :post="post" :interactive="interactive" :work-id="page.workId" :filter="page.topic" />
    </li>
  </ol>
  <p v-else class="empty">{{ page.message }}</p>
</template>

<style scoped lang="scss">
.notice-board { display: grid; align-content: start; gap: clamp(0.45rem, 1.4vh, 0.85rem); padding: 0.15rem 0.2rem; }

.notice {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.25rem 0.65rem;
  padding: clamp(0.5rem, 1.2vw, 0.9rem);
  background: var(--paper-card);
  border: 1px solid #a88550;
  border-radius: 0.22rem;
  box-shadow: 0 2px 6px #65331428;
  p { margin: 0; color: var(--paper-ink); }
  a { grid-column: 2; }
}

.wax-seal {
  display: grid;
  place-items: center;
  grid-row: 1 / span 2;
  text-shadow: 1px 1px #3d191d;
  width: 2.1rem;
  height: 2.1rem;
  border-radius: 50%;
  color: #e3bb82;
  background: #76383c;
  box-shadow: inset 0 0 0 3px #95534c;
}

.notice-actions, .notice-topics { display: flex; flex-wrap: wrap; gap: 0.38rem; align-items: center; }
a { color: var(--pine); text-underline-offset: 0.2em; }

.notice-topics a, .write-manuscript {
  display: inline-flex;
  align-items: center;
  min-height: 2.25rem;
  padding: 0.25rem 0.62rem;
  border: 1px solid #9a7544;
  border-radius: 0.22rem;
  background: #f9ebc699;
  color: var(--paper-ink);
  font-size: 0.88em;
  text-decoration: none;

  &:hover { background: #f5deaa; }
}

.notice-topics a[aria-current="page"], .write-manuscript {
  color: #f6dcaa;
  background: var(--pine);
  border-color: var(--brass-frame);
  font-weight: 600;

  &:hover { background: var(--pine-hover); }
}

.post-index, .popular-posts { list-style: none; padding: 0 0.1rem 0.2rem; display: grid; align-content: start; gap: 0.5rem; }
.post-index li, .popular-posts li { min-width: 0; }
.popular-shelf { min-width: 0; }
.popular-title {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin: 0 0 0.45rem;
  &::after { content: ''; flex: 1; min-width: 0; height: 1px; background: linear-gradient(90deg, #95724f8a, transparent); }
}
.popular-posts { margin: 0; padding-inline: 0; }
.popular-message { margin: 0; color: var(--paper-muted); font-size: 0.88em; }
.empty { align-self: center; color: var(--paper-muted); text-align: center; }

@media (max-width: 46rem) {
  .notice-actions, .notice-topics { align-items: stretch; }
  .notice-board a { overflow-wrap: anywhere; }
  .notice { grid-template-columns: minmax(0, 1fr); }
  .notice a { grid-column: 1; }
  .wax-seal { grid-row: auto; width: 1.7rem; height: 1.7rem; }
}
</style>
