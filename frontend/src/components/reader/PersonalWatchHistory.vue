<script setup lang="ts">
import { coverPreview } from '../../reader/coverPreview'
import { watchLabels } from '../../api/account'
import { dateLabel } from '../../api/community'
import type { WatchEvent } from '../../api/personal'
import BookSigil from '../shelf/BookSigil.vue'

defineProps<{ events: WatchEvent[]; interactive: boolean }>()
</script>

<template>
  <ol class="watch-journal">
    <li v-for="event in events" :key="event.id">
      <div class="journal-cover">
        <BookSigil kind="moon" />
        <img v-if="event.coverUrl" :key="event.coverUrl" :src="coverPreview(event.coverUrl)" alt="" loading="lazy" decoding="async"
          @error="($event.currentTarget as HTMLImageElement).hidden = true">
      </div>
      <div class="journal-entry">
        <time :datetime="event.recordedAt">{{ dateLabel(event.recordedAt) }}</time>
        <h4>
          <RouterLink v-if="interactive" :to="{ name: 'work-detail', params: { id: event.workId }, query: { from: 'personal' } }">
            {{ event.title }}
          </RouterLink>
          <span v-else>{{ event.title }}</span>
        </h4>
        <p>{{ watchLabels[event.watchStatus] }} · 已记录 {{ event.watchedEpisodes }}{{ event.episodeCount ? ' / ' + event.episodeCount : '' }} 集</p>
      </div>
    </li>
  </ol>
</template>

<style scoped lang="scss">
.watch-journal {
  display: grid; align-content: start; gap: .55rem; padding: 0; margin: 0; list-style: none;
}
li { display: grid; grid-template-columns: minmax(2.5rem, 15%) minmax(0, 1fr); gap: .65rem; align-items: center;
  padding: .48rem; background: #fff2c954; border: 1px solid #986b484d; border-left: 2px solid #85596c; }
.journal-cover { position: relative; display: grid; place-items: center; aspect-ratio: 4/5; overflow: hidden; background: #3a2936; color: #dfb683; }
img { position:absolute; inset:0; width:100%; height:100%; object-fit:cover; }
img[hidden] { display:none; }
h4, p { margin: 0; }
a { color: #3e3148; }
time { color: #624831; font-size: .82em; }
@media(max-width: 46rem) { li { grid-template-columns: 3.2rem minmax(0,1fr); gap:.5rem; } }
</style>
