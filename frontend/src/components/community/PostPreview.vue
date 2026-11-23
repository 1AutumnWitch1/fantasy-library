<script setup lang="ts">
import { dateLabel, mediaUrl, topics, type Post, type Topic } from '../../api/community'
import AuthorAvatar from './AuthorAvatar.vue'

defineProps<{ post: Post; interactive: boolean; compact?: boolean; workId?: number; filter?: Topic; fromRecommendations?: boolean }>()
</script>

<template>
  <article class="post-preview" :class="{ 'post-preview--compact': compact }">
    <div class="preview-heading">
      <div v-if="!compact" class="preview-labels">
        <p class="preview-topic">{{ topics[post.topic] }}</p>
        <time :datetime="post.createdAt">{{ dateLabel(post.createdAt) }}</time>
      </div>
      <h4>
        <RouterLink v-if="interactive" :to="{ name: 'community-post', params: { id: post.id },
          query: { workId, topic: filter, from: fromRecommendations ? 'recommendations' : undefined } }">{{ post.title }}</RouterLink>
        <span v-else>{{ post.title }}</span>
      </h4>
    </div>
    <div class="preview-excerpt">
      <p>{{ post.body }}</p>
      <img v-if="post.previewImage && !compact" :src="mediaUrl(post.previewImage)" alt="帖子插图" loading="lazy" decoding="async">
    </div>
    <div class="preview-footer">
      <div class="preview-author" :title="post.authorName">
        <AuthorAvatar :avatar-key="post.avatarKey" />
        <span>{{ post.authorName }}</span>
      </div>
      <div class="preview-counts">
        <span>{{ post.viewCount }} 浏览</span>
        <span>{{ post.commentCount }} 评论</span>
        <span>{{ post.likeCount }} 赞</span>
      </div>
    </div>
  </article>
</template>

<style scoped lang="scss">
.post-preview {
  position: relative;
  display: grid;
  gap: 0.3rem;
  min-width: 0;
  padding: 0.65rem 0.78rem;
  border: 1px solid #95734678;
  border-left: 3px solid var(--brass-frame);
  border-radius: 0.22rem;
  box-shadow: 0 1px 4px #78502d1a, inset 0 1px #fcf0c780;
  background: var(--paper-card);
}

.preview-heading { min-width: 0; }
.preview-heading h4 {
  font-size: clamp(0.98rem, 1.35vw, 1.35rem);
  line-height: 1.26;
  min-width: 0;
}

.preview-heading a, .preview-heading h4 > span, .preview-excerpt p {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  overflow-wrap: anywhere;
}

.preview-heading a { color: var(--pine); text-decoration: none; }
.preview-heading a:hover { text-decoration: underline; text-underline-offset: 0.17em; }
.preview-heading a:focus-visible { outline: 2px solid #806238; outline-offset: 2px; }

.preview-labels { display: flex; justify-content: space-between; align-items: center; gap: 0.3rem; margin-bottom: 0.12rem; }
.preview-topic { margin: 0; color: #78542f; font-size: 0.78rem; font-weight: 600; }

.preview-excerpt {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;

  p { margin: 0; color: var(--paper-body); min-width: 0; font-size: 0.92em; }
  img { flex: none; width: clamp(3rem, 6.8vw, 5rem); aspect-ratio: 7 / 4; object-fit: cover; border-radius: 0.16rem; border: 1px solid #a08354; }
}

.preview-footer, .preview-author, .preview-counts { display: flex; align-items: center; min-width: 0; }
.preview-footer { justify-content: space-between; flex-wrap: wrap; gap: 0.28rem 0.55rem; font-size: 0.82rem; }
.preview-author { gap: 0.38rem; color: var(--paper-ink); font-weight: 600; }
.preview-author > span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.preview-counts { gap: 0.24rem 0.55rem; flex-wrap: wrap; color: var(--paper-muted); }
.preview-counts span { white-space: nowrap; }
time { font-size: 0.74rem; color: var(--paper-muted); }

.post-preview--compact {
  background: #f3e2b270;
  padding: 0.5rem 0.62rem;
  border-left: 3px solid var(--pine);
  h4 { font-size: clamp(0.88rem, 1.15vw, 1.18rem); }
  .preview-excerpt p { -webkit-line-clamp: 1; }
}

@media (max-width: 46rem) {
  .post-preview, .post-preview--compact { padding: 0.5rem 0.55rem; gap: 0.24rem; }
  .preview-heading h4 { font-size: clamp(0.95rem, 3.6vw, 1.1rem); }
  .preview-footer { font-size: 0.78rem; }
  .preview-author :deep(.author-avatar) { width: 1.4rem; font-size: 0.7rem; }
  .preview-excerpt img, time { display: none; }
  .preview-counts { gap: 0.2rem 0.45rem; }
  .preview-topic { font-size: 0.72rem; }
}
</style>
