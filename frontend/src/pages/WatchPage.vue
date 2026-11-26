<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAccountStore } from '../stores/account'
import { useViewingProgress } from '../player/useViewingProgress'
import { useRelatedScreenings } from '../player/useRelatedScreenings'
import { readId } from '../api/community'
import { episodeLabel, groupPlayback, timeLabel, type Episode, type PlaybackSource } from '../api/playback'
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
const tab = ref<'recommendations' | 'comments'>('recommendations')
const shownSummary = ref(false)
const shareOpen = ref(false)
const shareStatus = ref('')
const shareInput = ref<HTMLInputElement | null>(null)
const work = computed(() => detail.value?.work)
const account = useAccountStore()
const viewing = useViewingProgress(() => detail.value?.anime ? work.value?.id : undefined)
const { stats, progress: viewingProgress, checking: checkingProgress } = viewing
const related = useRelatedScreenings(() => detail.value)
void account.restore()

const available = computed(() => episodes.value.filter(item => item.sources.length))
const lines = computed(() => groupPlayback(episodes.value))
const latest = computed(() => viewingProgress.value?.latest)
const episode = computed(() => {
  const selected = available.value.find(item => item.id === readId(route.query.episode))
  if (selected) return selected
  if (account.status === 'idle' || account.status === 'loading' || checkingProgress.value) return undefined
  const recent = latest.value
  const saved = recent && (recent.completed
    ? available.value.filter(item => item.edition === 'EPISODE' && item.number > recent.episodeNo)
        .sort((a, b) => a.number - b.number)[0]
    : available.value.find(item => item.edition === 'EPISODE' && item.number === recent.episodeNo))
  return saved ?? available.value.find(item => item.edition === 'EPISODE') ?? available.value[0]
})
const resume = computed(() => {
  const recent = latest.value
  return recent && episode.value?.edition === 'EPISODE' && recent.episodeNo === episode.value.number
    ? recent
    : undefined
})
const playable = computed(() => lines.value.find(line => line.id === episode.value?.nodeId)?.episodes ?? [])
const source = computed(() => {
  const item = episode.value
  return item?.sources.find(option => option.id === nodes.value[item.id]) ?? item?.sources[0]
})
const index = computed(() => playable.value.findIndex(item => item.id === episode.value?.id))
const previous = computed(() => index.value > 0 ? playable.value[index.value - 1] : undefined)
const next = computed(() => index.value >= 0 ? playable.value[index.value + 1] : undefined)
const poster = computed(() => detail.value?.images.find(image => image.kind === 'GALLERY')?.url ?? work.value?.cover?.url)
const detailsLink = computed(() => ({ name: 'work-detail', params: { id: route.params.id }, query: { from: route.query.from } }))
const originLink = computed(() => new URL(router.resolve({
  name: 'work-watch', params: route.params,
  query: { ...route.query, episode: episode.value && String(episode.value.id) },
}).href, location.origin).href)
const episodeTitle = computed(() => {
  const current = episode.value
  return current && current.title !== String(current.number) && current.title !== episodeLabel(current) ? current.title : ''
})
const holdingPlayer = computed(() => checkingProgress.value || account.status === 'idle' || account.status === 'loading')

watch(() => route.params.id, () => {
  nodes.value = {}
  autoplay.value = false
  tab.value = 'recommendations'
  shownSummary.value = false
  shareOpen.value = false
  shareStatus.value = ''
})
watch([work, () => route.fullPath], ([item]) => {
  if (item) document.title = item.title + ' · 放映室'
}, { flush: 'post' })
watch([work, episode], ([item, current]) => {
  if (!item || !current || item.id !== readId(route.params.id) || current.id === readId(route.query.episode)) return
  void router.replace({ name: 'work-watch', params: route.params, query: { ...route.query, episode: String(current.id) }, hash: route.hash })
}, { flush: 'post' })
watch([work, () => route.hash], async ([item, hash]) => {
  if (item && hash === '#screening-discussion') {
    tab.value = 'comments'
    await nextTick()
    document.getElementById('screening-discussion')?.scrollIntoView()
  }
}, { flush: 'post', immediate: true })

function navigateTabs(event: KeyboardEvent) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  if (event.key === 'Home') tab.value = 'recommendations'
  else if (event.key === 'End') tab.value = 'comments'
  else tab.value = tab.value === 'recommendations' ? 'comments' : 'recommendations'
  const tabs = event.currentTarget as HTMLElement
  tabs.querySelector<HTMLElement>(tab.value === 'comments' ? '#screening-comments-tab' : '#screening-recommendations-tab')?.focus()
}

function selectEpisode(item: Episode, automatic = false) {
  if (episode.value?.id === item.id || !item.sources.length) return
  autoplay.value = true
  shareStatus.value = ''
  const destination = { name: 'work-watch', params: route.params, query: { ...route.query, episode: String(item.id) }, hash: route.hash }
  void (automatic ? router.replace(destination) : router.push(destination))
}

function selectLine(lineId: number) {
  const matching = lines.value.find(line => line.id === lineId)?.episodes ?? []
  const current = episode.value
  const target = matching.find(item => item.edition === current?.edition && item.number === current.number) ?? matching[0]
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

function ended() {
  viewing.refreshProgress()
  if (autoNext.value && next.value) selectEpisode(next.value, true)
}

async function share() {
  shareStatus.value = ''
  try {
    await navigator.clipboard.writeText(originLink.value)
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
      <header class="screening-heading">
        <nav class="screening-path" aria-label="播放页导航">
          <RouterLink :to="detailsLink">‹ 返回作品详情</RouterLink>
          <span aria-hidden="true">/</span>
          <span aria-current="page">放映室</span>
        </nav>
        <template v-if="work">
          <h1>{{ work.title }}</h1>
          <div class="screening-meta">
            <span v-if="episode" class="scene-index">{{ episodeLabel(episode) }}</span>
            <span v-if="episodeTitle">{{ episodeTitle }}</span>
            <span v-if="stats">{{ stats.plays }} 次播放<span aria-hidden="true"> · </span>{{ stats.followers }} 人追番</span>
          </div>
        </template>
      </header>

      <div v-if="detail && work && detail.anime" class="screening-layout">
        <div class="stage-column">
          <LibraryVideo :key="work.id" :work-id="work.id" :title="work.title"
            :episode="holdingPlayer ? undefined : episode" :source="holdingPlayer ? undefined : source"
            :poster="poster" :autoplay="autoplay" :resume="resume"
            :quality-sources="episode?.sources" :loading="checkingEpisodes || holdingPlayer" :catalog-error="playbackError"
            :alternate="(episode?.sources.length ?? 0) > 1" :previous="!!previous" :next="!!next"
            @alternate="alternateSource" @retry-catalog="retry" @previous="previous && selectEpisode(previous)"
            @next="next && selectEpisode(next)" @ended="ended" @quality="selectSource"
            @counted="viewing.refreshStats" @back="router.push(detailsLink)" />
          <div class="screening-actions" aria-label="播放页操作">
            <WorkCollectionPanel :detail="detail" @changed="viewing.refreshStats" />
            <button type="button" class="share-button" @click="share">
              <span aria-hidden="true">↗</span> 分享链接
            </button>
            <p v-if="stats" class="screening-counts">{{ stats.plays }} 次播放 <span aria-hidden="true">·</span> {{ stats.followers }} 人追番</p>
            <p v-if="shareStatus" class="share-status" role="status">{{ shareStatus }}</p>
          </div>
          <label v-if="shareOpen" class="share-input">
            播放链接 <input ref="shareInput" readonly :value="originLink">
          </label>
        </div>

        <aside class="screening-aside" aria-label="选集与相关推荐">
          <ScreeningProgram :key="work.id" :lines="lines" :episode="episode" :source="source"
            :completed-episode="latest?.completed ? latest.episodeNo : undefined"
            :auto-next="autoNext" :busy="checkingEpisodes" @line="selectLine" @episode="selectEpisode"
            @source="selectSource" @auto-next="autoNext = $event" />
          <RelatedScreenings :items="related" />
        </aside>

        <div class="screening-content">
          <div class="screening-tabs" role="tablist" aria-label="放映室内容" @keydown="navigateTabs">
            <button id="screening-recommendations-tab" role="tab" aria-controls="screening-recommendations"
              :aria-selected="tab === 'recommendations'" :tabindex="tab === 'recommendations' ? 0 : -1" type="button" @click="tab = 'recommendations'">推荐</button>
            <button id="screening-comments-tab" role="tab" aria-controls="screening-comments-panel"
              :aria-selected="tab === 'comments'" :tabindex="tab === 'comments' ? 0 : -1" type="button" @click="tab = 'comments'">评论</button>
          </div>
          <section v-show="tab === 'recommendations'" id="screening-recommendations" class="screening-summary"
            role="tabpanel" aria-labelledby="screening-recommendations-tab">
            <h2>作品简介</h2>
            <p v-if="episode" class="summary-scene">当前播放 {{ episodeLabel(episode) }} <span v-if="episodeTitle"> · {{ episodeTitle }}</span></p>
            <p v-if="work.summary" :class="{ 'summary-excerpt': !shownSummary && (work.summary?.length ?? 0) > 100 }">{{ work.summary }}</p>
            <button v-if="(work.summary?.length ?? 0) > 100" type="button" class="summary-toggle"
              :aria-expanded="shownSummary" @click="shownSummary = !shownSummary">{{ shownSummary ? '收起简介' : '展开简介' }}</button>
            <p v-if="latest" class="summary-history">
              <strong>观看记录</strong>
              <span>最近观看第 {{ latest.episodeNo }} 集 · {{ latest.completed ? '已看完' : timeLabel(latest.positionMs / 1000) }}</span>
              <span v-if="viewingProgress?.completedEpisodes">已看完 {{ viewingProgress.completedEpisodes }} 集</span>
            </p>
            <ul v-if="related.length" class="summary-related" aria-label="相关作品">
              <li v-for="{ item } in related.slice(0, 3)" :key="item.work.id">
                <RouterLink :to="{ name: 'work-detail', params: { id: item.work.id }, query: { from: 'anime' } }">{{ item.work.title }} ›</RouterLink>
              </li>
            </ul>
          </section>
          <div v-show="tab === 'comments'" id="screening-comments-panel" role="tabpanel" aria-labelledby="screening-comments-tab">
            <ScreeningDiscussion :key="work.id" :work-id="work.id" />
          </div>
        </div>
      </div>
      <p v-else-if="detail" class="screening-state">此作品的资料与收藏信息在作品详情页。</p>
      <div v-else class="screening-state">
        <p v-if="error" role="alert">{{ error }}</p><p v-else role="status">正在打开放映室…</p>
        <button v-if="error" type="button" @click="retry">重试</button>
      </div>
    </div>
  </main>
</template>

<style scoped lang="scss" src="../styles/screening-room.scss"></style>
