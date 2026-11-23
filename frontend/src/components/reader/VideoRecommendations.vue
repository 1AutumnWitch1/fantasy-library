<script setup lang="ts">
import type { VideoItem } from '../../data/animeReader'
import ReaderArtwork from './ReaderArtwork.vue'

defineProps<{ videos: readonly VideoItem[]; searching: boolean }>()
</script>

<template>
  <ul v-if="videos.length" class="video-grid" aria-label="视频推荐">
    <li v-for="video in videos" :key="video.title" class="video-item">
      <div class="video-cover">
        <ReaderArtwork :artwork="video.artwork" />
        <span class="video-duration">{{ video.duration }}</span>
      </div>
      <span class="video-title" :title="video.title">{{ video.title }}</span>
    </li>
  </ul>
  <p v-else class="video-empty" role="status">{{ searching ? '未找到匹配的视频。' : '这一页暂无该题材的推荐。' }}</p>
</template>

<style scoped lang="scss">
.video-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: clamp(0.34rem, 1.05vh, 0.74rem) clamp(0.34rem, 0.74vw, 1rem);
  width: 100%;
  height: 100%;
  min-height: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.video-empty {
  margin: 0;
  color: #68513c;
  font-size: clamp(0.75rem, 1.04vw, 1.03rem);
}

.video-item {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  max-height: 100%;
  min-height: 0;
  gap: clamp(0.15rem, 0.45vh, 0.36rem);
  padding: clamp(0.14rem, 0.22vw, 0.31rem);
  background: #fbdf9e41;
  border: 1px solid #91664153;
  border-radius: 0.23rem;
  box-shadow: inset 0 1px #fff4ca94, 0 0.17rem 0.32rem #73604028;
}

.video-cover {
  position: relative;
  overflow: hidden;
  min-height: 0;
  border: 1px solid #78614399;
  border-radius: 0.14rem;
}

.video-duration {
  position: absolute;
  right: 0.28rem;
  bottom: 0.26rem;
  padding: 0.14rem 0.33rem;
  border-radius: 0.17rem;
  color: #fff0ce;
  background: #19241ef0;
  font: clamp(0.75rem, 1.08vw, 0.92rem) / 1.15 Georgia, serif;
  white-space: nowrap;
}

.video-title {
  overflow: hidden;
  color: #382719;
  font-family: var(--display-font);
  font-size: clamp(0.77rem, 1.33vw, 1.48rem);
  font-weight: 700;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 46rem) {
  .video-grid { gap: 0.28rem; }
  .video-item { padding: 0.1rem; gap: 0.14rem; }
  .video-title {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    min-height: 2.6em;
    font-size: clamp(0.65rem, 2.57vw, 0.86rem);
    line-height: 1.3;
    white-space: normal;
  }
  .video-duration { font-size: 0.59rem; right: 0.13rem; bottom: 0.15rem; padding: 0.08rem 0.15rem; }
}
</style>
