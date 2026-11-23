<script setup lang="ts">
import { ref, watch } from 'vue'
import { listWorks, type WorkDetail, type WorkItem } from '../../api/catalog'
import { coverPreview } from '../../reader/coverPreview'

const props = defineProps<{ detail: WorkDetail }>()
const related = ref<WorkItem[]>([])

watch(() => props.detail.work.id, async (id, _, onCleanup) => {
  const request = new AbortController()
  onCleanup(() => request.abort())
  related.value = []
  const tag = props.detail.tags[0]?.slug
  try {
    const list = (tags?: string[]) => listWorks('anime', { page: 1, size: 5, tags }, request.signal)
    const result = await list(tag ? [tag] : undefined)
    let matches = result.items.filter(item => item.work.id !== id)
    if (tag && !matches.length && !request.signal.aborted) matches = (await list()).items.filter(item => item.work.id !== id)
    if (!request.signal.aborted) related.value = matches.slice(0, 3)
  } catch {
    // Related works are optional while the video and catalog loads independently.
  }
}, { immediate: true })
</script>

<template>
  <section v-if="related.length" class="related" aria-labelledby="related-title">
    <h2 id="related-title">同架推荐</h2>
    <ul>
      <li v-for="{ work } in related" :key="work.id">
        <RouterLink :to="{ name: 'work-detail', params: { id: work.id }, query: { from: 'anime' } }">
          <div class="related-cover">
            <img v-if="work.cover" :src="coverPreview(work.cover.url)" alt="" loading="lazy" decoding="async"
              @error="($event.currentTarget as HTMLImageElement).hidden = true">
          </div>
          <div class="related-copy"><strong>{{ work.title }}</strong><span>{{ work.category.name }}</span></div>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped lang="scss">
.related { padding:.9rem 0 .15rem; }
h2 { margin:0 0 .55rem; color:var(--foil); font:700 1.22rem/1.25 var(--display-font); }
ul { display:grid; gap:.43rem; list-style:none; padding:0; margin:0; }
a { display:grid; grid-template-columns: minmax(4.2rem,32%) minmax(0,1fr); gap:.55rem; align-items:center;
  padding:.34rem; border:1px solid #98704a71; border-radius:.22rem; color:#f0d8aa; background:#2b1f16ac; text-decoration:none; }
a:hover { background:#443023; border-color:var(--brass-frame); }
.related-cover { overflow:hidden; aspect-ratio:16/10; background:#23382e; }
img { width:100%; height:100%; object-fit:cover; }
img[hidden] { display:none; }
.related-copy { display:grid; min-width:0; overflow-wrap:anywhere; font-size:.9rem; }
.related-copy span { color:var(--muted); font-size:.77rem; }
</style>
