<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GameDownloadActions from '../components/catalog/GameDownloadActions.vue'
import EpisodePicker from '../components/player/EpisodePicker.vue'
import WorkCollectionPanel from '../components/reader/WorkCollectionPanel.vue'
import BookSigil from '../components/shelf/BookSigil.vue'
import { communityLink } from '../api/community'
import { airingLabels } from '../api/catalog'
import { useViewingProgress } from '../player/useViewingProgress'
import { groupPlayback, type Episode } from '../api/playback'
import { useWorkScreen } from '../player/useWorkScreen'

const route = useRoute()
const router = useRouter()
const { detail, episodes, checkingEpisodes, error, playbackError, retry } = useWorkScreen(() => route.params.id, true)
const viewing = useViewingProgress(() => detail.value?.anime ? detail.value.work.id : undefined)
const { stats, progress } = viewing
const latest = computed(() => progress.value?.latest)
const resume = computed(() => !latest.value?.completed && episodes.value.some(item =>
  item.edition === 'EPISODE' && item.number === latest.value?.episodeNo && item.sources.length > 0))
const coverFailed = ref(false)
const bannerFailed = ref(false)
const returnLabels = { personal: '返回私人书架', recommendations: '返回推荐书', games: '返回游戏书', anime: '返回追番书' }
const kind = computed(() => {
  if (route.query.from === 'personal' || route.query.from === 'recommendations') return route.query.from
  if (detail.value) return detail.value.work.type === 'GAME' ? 'games' : 'anime'
  return route.query.from === 'anime' || route.query.from === 'games' ? route.query.from : null
})
const gallery = computed(() => detail.value?.images.filter(image => image.kind === 'GALLERY') ?? [])
const banner = computed(() => gallery.value[0] ?? detail.value?.work.cover)
const lineId = ref<number>()
const lines = computed(() => groupPlayback(episodes.value))
const selectedLine = computed(() => lines.value.find(line => line.id === lineId.value) ?? lines.value[0])
const playable = computed(() => selectedLine.value?.episodes ?? [])
const previewOnly = computed(() => playable.value.length > 0 && playable.value.every(item => item.edition === 'PREVIEW'))

watch(() => detail.value?.work.id, () => {
  lineId.value = undefined
  coverFailed.value = false
  bannerFailed.value = false
  document.title = detail.value ? detail.value.work.title + ' · 幻想图书馆' : '作品资料 · 幻想图书馆'
})

function watchLink(episodeId?: number) {
  return { name: 'work-watch', params: { id: route.params.id },
    query: { from: kind.value ?? undefined, episode: episodeId === undefined ? undefined : String(episodeId) } }
}

function playEpisode(episode: Episode) {
  void router.push(watchLink(episode.id))
}

function returnToBook() {
  const target = kind.value ? { name: 'book', params: { bookId: kind.value } } : { name: 'shelf' }
  if (router.options.history.state.back === router.resolve(target).fullPath) router.back()
  else void router.replace(target)
}
</script>

<template>
  <main class="dossier-room">
    <div class="dossier-wrap">
      <div class="work-return">
        <button type="button" @click="returnToBook">‹ {{ kind ? returnLabels[kind] : '返回书架' }}</button>
      </div>
      <template v-if="detail">
        <article class="dossier-hero" :class="{ 'dossier-hero--game': detail.game }">
          <img v-if="banner && !bannerFailed" :src="banner.url" alt="" class="dossier-backdrop" decoding="async" @error="bannerFailed = true">
          <div class="dossier-hero__contents">
            <figure class="plate">
              <img v-if="detail.work.cover && !coverFailed" :src="detail.work.cover.url" :alt="detail.work.cover.altText || detail.work.title"
                decoding="async" @error="coverFailed = true">
              <BookSigil v-else :kind="detail.anime ? 'moon' : 'swords'" />
            </figure>
            <header class="dossier-title">
              <p class="work-category">{{ detail.anime ? '追番' : '游戏' }} <span aria-hidden="true">◆</span> {{ detail.work.category.name }}</p>
              <h1>{{ detail.work.title }}</h1>
              <p class="title-details">
                <span v-if="detail.anime">{{ airingLabels[detail.anime.airingStatus] }}</span>
                <span v-if="detail.anime?.episodeCount">全 {{ detail.anime.episodeCount }} 集</span>
                <span v-if="detail.game?.platforms.length">{{ detail.game.platforms.map(item => item.name).join(' · ') }}</span>
                <time v-if="detail.work.releaseDate" :datetime="detail.work.releaseDate">{{ detail.work.releaseDate }}</time>
              </p>
              <details v-if="detail.work.summary" class="dossier-summary">
                <summary>作品简介</summary><p>{{ detail.work.summary }}</p>
              </details>
              <p v-if="stats" class="title-details">{{ stats.followers }} 人追番 · {{ stats.plays }} 次播放</p>
              <WorkCollectionPanel :key="detail.work.id" :detail="detail" @changed="viewing.refreshStats" />
              <div class="dossier-actions">
                <RouterLink v-if="detail.anime" class="watch-link" :to="watchLink()">
                  <span aria-hidden="true">▶</span> {{ resume ? '继续观看' : latest ? '观看分集' : '开始观看' }}
                </RouterLink>
                <GameDownloadActions v-else-if="detail.game" :key="detail.work.id" :work-id="detail.work.id" />
                <RouterLink v-if="detail.anime" :to="{ ...watchLink(), hash: '#screening-discussion' }">番剧评论 ›</RouterLink>
                <RouterLink v-else :to="communityLink(detail.work.id)">作品讨论 ›</RouterLink>
              </div>
              <p v-if="detail.anime && !checkingEpisodes && !playbackError" class="availability">
                {{ previewOnly ? '已收录 ' + playable.length + ' 段馆藏演示预告' : !playable.length ? '暂未收录播放源' : '已收录 ' + playable.length + ' 段可观看内容' }}
              </p>
            </header>
          </div>
        </article>

        <EpisodePicker v-if="lines.length" class="work-playback" :lines="lines"
          :active-line-id="selectedLine?.id" :busy="checkingEpisodes" @line="lineId = $event" @episode="playEpisode" />

        <div class="dossier-papers" :class="{ 'dossier-papers--single': !detail.anime }">
          <section class="record-paper" aria-labelledby="work-archive">
            <h2 id="work-archive">作品档案</h2>
            <ul v-if="detail.tags.length" class="record-tags" aria-label="作品标签">
              <li v-for="tag in detail.tags" :key="tag.id">{{ tag.name }}</li>
            </ul>
            <dl class="record-meta">
              <template v-if="detail.work.releaseDate"><dt>{{ detail.anime ? '首播日期' : '发行日期' }}</dt><dd>{{ detail.work.releaseDate }}</dd></template>
              <template v-if="detail.anime">
                <dt>制作公司</dt><dd>{{ detail.anime.studio || '未收录' }}</dd>
                <dt>播出状态</dt><dd>{{ airingLabels[detail.anime.airingStatus] }}</dd>
                <dt>集数</dt><dd>{{ detail.anime.episodeCount ?? '待定' }}</dd>
              </template>
              <template v-if="detail.game">
                <dt>开发商</dt><dd>{{ detail.game.developer || '未收录' }}</dd>
                <dt>游戏平台</dt><dd>{{ detail.game.platforms.map(item => item.name).join(' · ') || '未收录' }}</dd>
                <template v-if="detail.game.ageRating"><dt>年龄分级</dt><dd>{{ detail.game.ageRating }}</dd></template>
              </template>
            </dl>
          </section>
          <section v-if="detail.anime" class="record-paper record-summary" aria-labelledby="work-summary">
            <h2 id="work-summary">作品简介</h2>
            <p>{{ detail.work.summary?.trim() || '暂无简介' }}</p>
          </section>
        </div>

        <section v-if="gallery.length" class="dossier-gallery" aria-labelledby="work-gallery">
          <h2 id="work-gallery">馆藏插图</h2>
          <div>
            <figure v-for="picture in gallery" :key="picture.id">
              <a :href="picture.url" target="_blank" rel="noopener" :aria-label="'查看原图 ' + (picture.altText || detail.work.title)">
                <img :src="picture.url" :alt="picture.altText || detail.work.title" loading="lazy" decoding="async">
              </a>
              <figcaption v-if="picture.altText">{{ picture.altText }}</figcaption>
            </figure>
          </div>
        </section>
      </template>
      <div v-else class="record-paper" role="status">
        <p v-if="error" role="alert">{{ error }}</p><p v-else>正在调阅作品资料…</p>
        <button v-if="error" type="button" @click="retry">重新查询</button>
      </div>
    </div>
  </main>
</template>

<style scoped lang="scss" src="../styles/work-dossier.scss"></style>
<style scoped>
.dossier-summary { margin: .75rem 0; color: #f2ddbc; }
.dossier-summary summary { cursor: pointer; }
.dossier-summary p { margin: .5rem 0; white-space: pre-line; }
.dossier-actions { margin-top: .65rem; }
.dossier-papers--single { grid-template-columns: 1fr; }
.record-summary { align-self: start; }
.record-summary p {
  margin: .65rem 0 0;
  line-height: 1.7;
  white-space: pre-line;
  overflow-wrap: anywhere;
}
</style>
