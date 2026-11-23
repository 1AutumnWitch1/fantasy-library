<script setup lang="ts">
import { coverPreview } from '../../reader/coverPreview'
import { watchLabels, type SavedWork } from '../../api/account'
defineProps<{ items: SavedWork[]; interactive: boolean }>()
</script>

<template>
  <ul class="private-shelf reader-content">
    <li v-for="item in items" :key="item.workId" class="edition">
      <img v-if="item.coverUrl" :key="item.coverUrl" :src="coverPreview(item.coverUrl)" loading="lazy" decoding="async" :alt="item.coverAlt ?? ''"
        @error="($event.target as HTMLImageElement).hidden = true">
      <div class="edition-title">
        <h4>{{ item.title }}</h4>
        <p v-if="item.watchStatus">{{ watchLabels[item.watchStatus] }} · {{ item.watchedEpisodes }}{{ item.episodeCount ? ' / ' + item.episodeCount : '' }} 集</p>
        <p v-else>{{ [item.favorite && '已收藏', item.wishlist && '愿望单'].filter(Boolean).join(' · ') }}</p>
      </div>
      <RouterLink v-if="interactive" :to="{ name: 'work-detail', params: { id: item.workId }, query: { from: 'personal' } }"
        :aria-label="'打开《' + item.title + '》'" class="edition-link" />
    </li>
  </ul>
</template>

<style scoped lang="scss">
.private-shelf {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(8rem, 1fr));
  gap: .9rem .55rem;
  padding: .7rem .7rem 1rem;
  list-style: none;
  background:
    linear-gradient(to bottom, transparent calc(50% - .42rem), #aa7947 50%, #452b1c calc(50% + .38rem), transparent calc(50% + .39rem)),
    linear-gradient(110deg, #1e110d88, #3623173b 55%, #23160fd9),
    var(--walnut-grain) center / 34rem;
  border: .26rem solid #6f4e32;
  border-bottom-width: .45rem;
  box-shadow: inset 0 0 1rem #120a05b0, 0 .2rem .35rem #51311166;
}
.edition {
  position: relative;
  align-self: end;
  justify-self: center;
  width: min(100%, 13.5rem);
  max-height: 100%;
  aspect-ratio: 4/5;
  background: #313b2e;
  border: 1px solid #bc9666;
  border-radius: .2rem .35rem .35rem .2rem;
  overflow: hidden;
  box-shadow: .18rem .14rem .48rem #120904e0;
  img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; object-position: center 35%; }
}
.edition-title {
  position: absolute;
  inset: auto 0 0;
  background: linear-gradient(transparent,#251b16f5 42%);
  padding: 1.2rem .6rem .55rem;
  color: #f4dfb2;
  h4 { font-size: clamp(.9rem,1.7vw,1.55rem); color: inherit !important; }
  p { margin: .24rem 0 0; font-size: .86em; }
}
.edition-link { position: absolute; inset: 0; }
.edition-link:focus-visible { outline: 3px solid #edc88d; outline-offset: -3px; }
@media(max-width:46rem) {
  .private-shelf { grid-template-columns: repeat(2,minmax(0,1fr)); grid-template-rows: none; grid-auto-rows: auto; align-content: start; padding: .4rem; background: linear-gradient(#2012095c,#110b06ad), var(--walnut-grain) center / 20rem; }
  .edition { align-self: start; }
  .edition-title { padding-inline:.3rem; }
}
</style>
