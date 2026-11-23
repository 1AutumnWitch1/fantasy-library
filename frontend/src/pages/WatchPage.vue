<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { readId } from '../api/community'
import { episodeLabel, groupPlayback, type Episode, type PlaybackSource } from '../api/playback'
import { useWorkScreen } from '../player/useWorkScreen'
import LibraryVideo from '../components/player/LibraryVideo.vue'
import ScreeningProgram from '../components/player/ScreeningProgram.vue'
import RelatedScreenings from '../components/player/RelatedScreenings.vue'
import ScreeningDiscussion from '../components/player/ScreeningDiscussion.vue'
import WorkCollectionPanel from '../components/reader/WorkCollectionPanel.vue'

const route = useRoute()
const router = useRouter()
const { detail, episodes, checkingEpisodes, error, playbackError, retry } = useWorkScreen(() => route.params.id, true)
const nodes = ref<Record<number, number>>({})
const autoNext = ref(false)
const autoplay = ref(false)
const saveOpen = ref(false)
const shareOpen = ref(false)
const shareStatus = ref('')
const shareInput = ref<HTMLInputElement | null>(null)
const work = computed(() => detail.value?.work)
const available = computed(() => episodes.value.filter(item => item.sources.length))
const lines = computed(() => groupPlayback(episodes.value))
const episode = computed(() => available.value.find(item => item.id === readId(route.query.episode)) ?? available.value[0])
const playable = computed(() => lines.value.find(line => line.id === episode.value?.nodeId)?.episodes ?? [])
const source = computed(() => {
  const item = episode.value
  return item?.sources.find(option => option.id === nodes.value[item.id]) ?? item?.sources[0]
})
const index = computed(() => playable.value.findIndex(item => item.id === episode.value?.id))
const previous = computed(() => index.value > 0 ? playable.value[index.value - 1] : undefined)
const next = computed(() => index.value >= 0 ? playable.value[index.value + 1] : undefined)
const poster = computed(() => detail.value?.images.find(image => image.kind === 'GALLERY')?.url ?? work.value?.cover?.url)
const originLink = computed(() => new URL(route.fullPath, location.origin).href)
const detailsLink = computed(() => ({ name: 'work-detail', params: { id: route.params.id }, query: { from: route.query.from } }))

watch(() => route.params.id, () => { nodes.value = {}; autoplay.value = false; saveOpen.value = false; shareOpen.value = false; shareStatus.value = '' })
watch(work, value => {
  if (value) document.title = value.title + ' · 放映室'
})
watch([work, () => route.hash], ([value, hash]) => {
  if (value && hash === '#screening-discussion') document.getElementById('screening-discussion')?.scrollIntoView()
}, { flush: 'post' })

function selectEpisode(item: Episode) {
  if (episode.value?.id === item.id || !item.sources.length) return
  autoplay.value = true
  void router.replace({ name: 'work-watch', params: route.params, query: { ...route.query, episode: String(item.id) } })
}

function selectLine(lineId: number) {
  const matching = available.value.filter(item => item.nodeId === lineId)
  const previous = episode.value
  const target = matching.find(item => item.edition === previous?.edition && item.number === previous.number) ?? matching[0]
  if (target) selectEpisode(target)
}

function selectSource(item: PlaybackSource) {
  if (episode.value) nodes.value[episode.value.id] = item.id
}

function alternateSource() {
  const choices = episode.value?.sources
  if (!choices?.length) return
  const position = choices.findIndex(item => item.id === source.value?.id)
  selectSource(choices[(position + 1) % choices.length])
}

async function share() {
  shareStatus.value = ''
  try {
    await navigator.clipboard.writeText(location.href)
    shareOpen.value = false
    shareStatus.value = '已复制播放链接'
  } catch {
    shareOpen.value = true
    await nextTick()
    shareInput.value?.select()
  }
}
</script>

<template>
  <main class="screening-room">
    <div class="screening-wrap">
      <nav class="screening-path" aria-label="播放页导航">
        <RouterLink :to="detailsLink">‹ 返回作品详情</RouterLink>
        <span> / 放映室</span>
      </nav>
      <template v-if="detail && work">
        <template v-if="detail.anime">
          <header class="screening-heading">
            <h1>{{ work.title }}</h1>
            <p v-if="episode"><span class="scene-index">{{ episodeLabel(episode) }}</span> {{ episode.title }}</p>
            <p v-else>{{ work.category.name }} · {{ detail.anime.studio }}</p>
          </header>
          <div class="screening-layout">
            <div class="stage-column">
              <LibraryVideo :key="work.id" :work-id="work.id" :title="work.title" :episode="episode" :source="source"
                :poster="poster" :autoplay="autoplay" :loading="checkingEpisodes" :catalog-error="playbackError" :alternate="(episode?.sources.length ?? 0) > 1"
                :previous="!!previous" :next="!!next"
                @alternate="alternateSource" @retry-catalog="retry" @previous="previous && selectEpisode(previous)" @next="next && selectEpisode(next)"
                @ended="autoNext && next && selectEpisode(next)" />
              <div class="screening-actions" aria-label="播放页操作">
                <a v-if="source" class="screening-node-jump" href="#screening-nodes">选来源与分集</a>
                <button type="button" :aria-expanded="saveOpen" aria-controls="screening-collection" @click="saveOpen = !saveOpen">私人书架</button>
                <button type="button" @click="share">分享链接</button>
                <a href="#screening-discussion">番剧评论 ↓</a>
                <p v-if="shareStatus" role="status">{{ shareStatus }}</p>
              </div>
              <ScreeningProgram :lines="lines" :episode="episode" :source="source"
                :auto-next="autoNext" :busy="checkingEpisodes" @line="selectLine" @episode="selectEpisode"
                @source="selectSource" @auto-next="autoNext = $event" />
              <label v-if="shareOpen" class="share-input">
                播放链接 <input ref="shareInput" readonly :value="originLink">
              </label>
              <div v-if="saveOpen" id="screening-collection" class="screening-collection">
                <WorkCollectionPanel :detail="detail" />
              </div>
              <details class="screening-summary">
                <summary>作品简介</summary>
                <p>{{ work.summary }}</p>
              </details>
            </div>
            <aside class="screening-aside" aria-label="相关推荐">
              <RelatedScreenings :detail="detail" />
            </aside>
            <ScreeningDiscussion class="screening-discussion" :key="work.id" :work-id="work.id" />
          </div>
        </template>
        <p v-else class="screening-state">此作品的资料与收藏信息在作品详情页。</p>
      </template>
      <div v-else class="screening-state">
        <p v-if="error" role="alert">{{ error }}</p><p v-else role="status">正在打开放映室…</p>
        <button v-if="error" type="button" @click="retry">重试</button>
      </div>
    </div>
  </main>
</template>

<style scoped lang="scss" src="../styles/screening-room.scss"></style>
