<script setup lang="ts">
import type { RelatedScreening } from '../../player/useRelatedScreenings'
import { coverPreview } from '../../reader/coverPreview'

defineProps<{ items: RelatedScreening[] }>()
</script>

<template>
  <section v-if="items.length" id="related-screenings" class="related" aria-labelledby="related-title">
    <h2 id="related-title">相关推荐</h2>
    <ul>
      <li v-for="{ item, plays } in items" :key="item.work.id">
        <RouterLink :to="{ name: 'work-detail', params: { id: item.work.id }, query: { from: 'anime' } }">
          <div class="related-cover">
            <img v-if="item.work.cover" :src="coverPreview(item.work.cover.url)" alt="" loading="lazy" decoding="async"
              @error="($event.currentTarget as HTMLImageElement).hidden = true">
          </div>
          <div class="related-copy">
            <strong>{{ item.work.title }}</strong>
            <span>{{ item.work.category.name }}<template v-if="item.anime?.episodeCount"> · {{ item.anime.episodeCount }} 集</template></span>
            <span v-if="plays !== undefined">{{ plays }} 次播放</span>
          </div>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped lang="scss">
.related { margin-top: .85rem; padding-top: .72rem; border-top: 1px solid #94734574; }
h2 { margin: 0 0 .55rem; color: var(--paper-ink); font: 700 1.13rem/1.25 var(--display-font); }
ul { display: grid; gap: .42rem; list-style: none; padding: 0; margin: 0; }
a {
  display: grid;
  grid-template-columns: minmax(4.2rem, 37%) minmax(0, 1fr);
  gap: .58rem;
  align-items: center;
  padding: .3rem;
  border: 1px solid #98704a59;
  border-radius: .24rem;
  color: var(--paper-ink);
  background: #f4dfb472;
  text-decoration: none;
}
a:hover { background: #fbecce; border-color: #876a40; }
.related-cover { overflow: hidden; aspect-ratio: 16/9; border-radius: .16rem; background: var(--pine); }
img { width: 100%; height: 100%; object-fit: cover; }
img[hidden] { display: none; }
.related-copy { display: grid; gap: .14rem; min-width: 0; overflow-wrap: anywhere; font-size: .88rem; line-height: 1.28; }
.related-copy strong { display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.related-copy span { color: var(--paper-muted); font-size: .73rem; }
</style>
