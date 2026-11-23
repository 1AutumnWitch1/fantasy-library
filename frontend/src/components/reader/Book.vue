<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, toRef, toRefs, watch } from 'vue'
import { closeBook, closedPosition, fadePage, openBook, prepareReader, turnLeaf } from '../../animations/readerMotion'
import type { ReaderMotionElements } from '../../animations/readerMotion'
import { measureBook, type BookRect } from '../../reader/bookGeometry'
import { movePage, type PageSide } from '../../reader/pagePosition'
import { pageSound } from '../../reader/pageSound'
import { compactReaderQuery, lightweightDevice, reducedMotionQuery, simplifyReader } from '../../reader/preferences'
import ReaderSearch from './ReaderSearch.vue'
import ReaderSettings from './ReaderSettings.vue'
import type { BookDefinition } from '../../reader/bookTypes'
import { useReaderStore } from '../../stores/reader'
import { planTurn, turnBetween, type BookTurn } from '../../reader/turnPlan'
import BookSigil from '../shelf/BookSigil.vue'
import BookSpine from '../shelf/BookSpine.vue'
import BookCover from './BookCover.vue'
import BookObject from './BookObject.vue'
import CatalogFilters from './CatalogFilters.vue'
import { useCatalogBook } from '../../reader/useCatalogBook'
import { useCommunityBook } from '../../reader/useCommunityBook'
import { usePersonalBook } from '../../reader/usePersonalBook'
import PersonalTabs from './PersonalTabs.vue'
import RecommendationFilter from './RecommendationFilter.vue'
import { useRecommendationBook } from '../../reader/useRecommendationBook'
import { catalogSpread } from '../../reader/catalogSpread'

const props = defineProps<{
  book: BookDefinition
  origin: BookRect
  source: HTMLButtonElement
  closeRequested: boolean
}>()
const emit = defineEmits<{ requestClose: []; closed: [] }>()

type ReaderPhase = 'loading' | 'opening' | 'ready' | 'turning' | 'closing'

const phase = ref<ReaderPhase>('loading')
const reader = useReaderStore()
const reading = reader.stateFor(props.book.spine.id)
const { spreadIndex, selectedTags, searchQuery, slideIndexes, carouselPaused } = toRefs(reading)
const live = props.book.catalog ? useCatalogBook(props.book.catalog, reading) : null
const community = props.book.spine.id === 'community' ? useCommunityBook(reading) : null
const personal = props.book.spine.id === 'personal' ? usePersonalBook(toRef(reader, 'personalChapter'), reading) : null
const recommendations = props.book.spine.id === 'recommendations' ? useRecommendationBook(toRef(reader, 'recommendationType')) : null
const localSpreads = computed(() => recommendations?.spreads.value ?? props.book.spreads)
const pendingTurn = shallowRef<BookTurn | null>(null)
const singlePage = ref(false)
const systemReduced = ref(false)
const lightweight = ref(false)
const reduceMotion = computed(() => simplifyReader(reader.motionPreference, systemReduced.value, singlePage.value, lightweight.value))
const side = computed(() => reader.singleSides[props.book.spine.id] ?? 'left')
const tagsExpanded = ref(false)
const requestingTurn = ref(false)
const sound = pageSound()
const settings = ref<InstanceType<typeof ReaderSettings> | null>(null)

function setSide(value: PageSide) {
  reader.singleSides[props.book.spine.id] = value
}
function setSound(value: boolean) {
  reader.soundEnabled = value
  if (value) sound.unlock()
  else sound.stop()
}

if (personal) watch(() => reader.personalChapter, () => setSide('left'))
const currentSpread = computed(() => live?.spread.value ?? community?.spread() ?? personal?.spread.value ?? localSpreads.value[spreadIndex.value] ?? localSpreads.value[0])
const spreadCount = computed(() => live ? live.page.value?.totalPages ?? 0
  : community ? community.result.value?.page.totalPages ?? 0 : personal?.spreadCount.value ?? localSpreads.value.length)
const turnLocked = computed(() => phase.value !== 'ready' || requestingTurn.value || !!live?.loading.value || !!personal?.working.value || !!personal?.marking.value || !!community?.busy.value || !!recommendations?.busy.value)
const previousPage = computed(() => movePage(spreadIndex.value, side.value, -1, singlePage.value))
const nextPage = computed(() => movePage(spreadIndex.value, side.value, 1, singlePage.value))
const canGoBack = computed(() => previousPage.value.index >= 0 && spreadCount.value > 0)
const canGoForward = computed(() => nextPage.value.index < spreadCount.value)
const theme = {
  '--binding-light': props.book.theme.light,
  '--binding-leather': props.book.theme.leather,
  '--binding-shade': props.book.theme.shade,
}
const displayedSpread = computed(() => pendingTurn.value?.underlay ?? currentSpread.value)

function setFilter<K extends 'selectedTags' | 'searchQuery'>(key: K, value: typeof reading[K]) {
  if (!live || phase.value !== 'ready' || props.closeRequested || reading[key] === value) return
  reading[key] = value
  spreadIndex.value = 0
  if (singlePage.value) setSide(props.book.catalog === 'anime' ? 'right' : 'left')
  slideIndexes.value = {}
  carouselHeld.value = false
  live.reload(0, key === 'searchQuery')
}

function toggleTag(slug: string) {
  const tags = selectedTags.value
  if (!slug) {
    if (tags.length) setFilter('selectedTags', [])
    return
  }
  const updated = tags.includes(slug) ? tags.filter(tag => tag !== slug) : [...tags, slug]
  setFilter('selectedTags', updated)
}

const carouselHeld = ref(false)
const pageVisible = ref(true)
const activeFeatured = computed(() => {
  const page = singlePage.value && side.value === 'right' ? currentSpread.value.right : currentSpread.value.left
  return page.kind === 'featured' ? page : undefined
})
const featuredIndex = computed(() => activeFeatured.value ? Math.min(slideIndexes.value[activeFeatured.value.number] ?? 0, activeFeatured.value.slides.length - 1) : 0)

function selectFeatured(index: number) {
  if (phase.value !== 'ready' || !activeFeatured.value) return
  slideIndexes.value[activeFeatured.value.number] = index
}

function trackVisibility() {
  pageVisible.value = !document.hidden
}

watch(
  [phase, reduceMotion, carouselHeld, carouselPaused, pageVisible, activeFeatured, featuredIndex],
  (_, __, onCleanup) => {
    const count = activeFeatured.value?.slides.length ?? 0
    if (phase.value !== 'ready' || reduceMotion.value || carouselHeld.value ||
        carouselPaused.value || !pageVisible.value || count < 2) return
    const timer = window.setTimeout(() => {
      selectFeatured((featuredIndex.value + 1) % count)
    }, 3500)
    onCleanup(() => window.clearTimeout(timer))
  },
)
const pageRange = computed(() => {
  if ((live && !live.page.value) || (community && !community.result.value)) return '…'
  if (!spreadCount.value) return community ? '0 篇手稿' : '0 部作品'
  const spread = currentSpread.value
  if (singlePage.value) return spread[side.value].number + ' / ' + spreadCount.value * 2
  return spread.left.number + '–' + spread.right.number + ' / ' + spreadCount.value * 2
})
const busyMessage = computed(() => ({
  loading: '正在载入书页…',
  opening: '正在展开…',
  ready: '',
  turning: '正在翻页…',
  closing: '正在归还书籍…',
})[phase.value])

const dialog = ref<HTMLDialogElement | null>(null)
const stage = ref<HTMLElement | null>(null)
const backdrop = ref<HTMLElement | null>(null)
const toolbar = ref<HTMLElement | null>(null)
const navigation = ref<HTMLElement | null>(null)
const genrePanel = ref<HTMLElement | null>(null)
const flight = ref<HTMLElement | null>(null)
const spine = ref<HTMLElement | null>(null)
const object = ref<InstanceType<typeof BookObject> | null>(null)
const closeButton = ref<HTMLButtonElement | null>(null)
const previousButton = ref<HTMLButtonElement | null>(null)
const nextButton = ref<HTMLButtonElement | null>(null)

let elements: ReaderMotionElements | null = null
let activeMotion: ReturnType<typeof openBook> | null = null
let viewport: MediaQueryList | undefined
let motionMedia: MediaQueryList | undefined
let releaseScroll: (() => void) | undefined
let disposed = false

function lockBackgroundScroll() {
  const { style } = document.body
  const overflow = style.overflow
  const padding = style.paddingRight
  const gutter = Math.max(0, window.innerWidth - document.documentElement.clientWidth)
  if (gutter) style.paddingRight = (parseFloat(getComputedStyle(document.body).paddingRight) + gutter) + 'px'
  style.overflow = 'hidden'
  return () => {
    style.overflow = overflow
    style.paddingRight = padding
  }
}

function updateDisplay() {
  const narrow = viewport?.matches ?? false
  const reduced = motionMedia?.matches ?? false
  if (narrow !== singlePage.value || reduced !== systemReduced.value) settleOnResize()
  singlePage.value = narrow
  systemReduced.value = reduced
}

function focusControl(preferred: HTMLElement | null) {
  void nextTick(() => {
    if (disposed || phase.value !== 'ready') return
    const target = [preferred, nextButton.value, previousButton.value, closeButton.value]
      .find(item => item instanceof HTMLButtonElement && !item.disabled)
    target?.focus({ preventScroll: true })
  })
}

function settleOnResize() {
  if (phase.value !== 'ready') activeMotion?.progress(1)
}

onMounted(async () => {
  const physical = object.value
  if (!dialog.value || !physical?.layout || !physical.book || !physical.board || !physical.cover ||
      !backdrop.value || !toolbar.value || !navigation.value || !flight.value || !spine.value) {
    emit('closed')
    return
  }

  if (live) {
    void live.loadFilters()
    live.reload(spreadIndex.value)
  }
  viewport = window.matchMedia(compactReaderQuery)
  motionMedia = window.matchMedia(reducedMotionQuery)
  lightweight.value = lightweightDevice()
  updateDisplay()
  viewport.addEventListener('change', updateDisplay)
  motionMedia.addEventListener('change', updateDisplay)
  window.addEventListener('resize', settleOnResize)
  trackVisibility()
  document.addEventListener('visibilitychange', trackVisibility)
  releaseScroll = lockBackgroundScroll()
  const controls = [toolbar.value, navigation.value]
  if (genrePanel.value) controls.push(genrePanel.value)
  elements = {
    backdrop: backdrop.value,
    controls,
    book: physical.book,
    board: physical.board,
    cover: physical.cover,
    flight: flight.value,
    spine: spine.value,
  }

  prepareReader(elements)
  dialog.value.showModal()
  stage.value?.focus({ preventScroll: true })
  await nextTick()
  if (disposed || !physical.layout) return

  phase.value = 'opening'
  activeMotion = openBook(elements, props.origin, closedPosition(physical.layout), reduceMotion.value, () => {
    activeMotion = null
    phase.value = 'ready'
    focusControl(closeButton.value)
  })
})

async function requestTurn(step: -1 | 1) {
  if (turnLocked.value || props.closeRequested) return
  const destination = movePage(spreadIndex.value, side.value, step, singlePage.value)
  if (destination.index < 0 || destination.index >= spreadCount.value) return

  if (reader.soundEnabled) sound.unlock()
  const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
  const crossesSpread = destination.index !== spreadIndex.value
  requestingTurn.value = true
  try {
    const result = crossesSpread && live ? await live.fetchPage(destination.index, !reduceMotion.value) : null
    const edition = crossesSpread && community ? await community.fetchPage(destination.index) : undefined
    const personalEdition = crossesSpread && personal ? await personal.fetchEdition(destination.index) : undefined
    if (disposed || props.closeRequested || (crossesSpread &&
        ((live && !result) || (community && !edition) || (personal && !personalEdition)))) return

    if (live && result && (result.page - 1 !== destination.index || !result.items.length)) {
      live.commit(result)
      setSide('left')
      return
    }
    if (community && edition && (edition.page.page - 1 !== destination.index || !edition.page.items.length)) {
      community.commit(edition)
      setSide('left')
      return
    }
    if (personal && personalEdition && personalEdition.index !== destination.index) {
      personal.commit(personalEdition)
      setSide('left')
      return
    }

    const incoming = personalEdition?.spread ?? (edition ? community?.spread(edition)
      : result && props.book.catalog ? catalogSpread(props.book.catalog, result) : undefined)
    const simplified = reduceMotion.value || !crossesSpread
    const turn = simplified ? null : incoming
      ? turnBetween(currentSpread.value, incoming, step, destination.index)
      : planTurn(localSpreads.value, spreadIndex.value, step)
    if (!simplified && !turn) return

    phase.value = 'turning'
    carouselHeld.value = false
    pendingTurn.value = turn
    stage.value?.focus({ preventScroll: true })
    await nextTick()
    if (disposed) return
    if (reader.soundEnabled) sound.play()

    function swapPage() {
      if (live && result) live.commit(result)
      else if (community && edition) community.commit(edition)
      else if (personal && personalEdition) personal.commit(personalEdition)
      else spreadIndex.value = destination.index
      setSide(destination.side)
    }

    function settleTurn() {
      activeMotion = null
      pendingTurn.value = null
      phase.value = 'ready'
      focusControl(previousFocus)
    }

    const leaf = object.value?.leaf
    const paper = object.value?.paper
    if (turn && leaf?.element) {
      activeMotion = turnLeaf(leaf.element, leaf.surfaces(), turn.direction, () => {
        swapPage()
        settleTurn()
      })
    } else if (paper) {
      activeMotion = fadePage(paper, swapPage, settleTurn)
    } else {
      swapPage()
      settleTurn()
    }
  } finally {
    requestingTurn.value = false
  }
}

function retryPage() {
  if (!live || phase.value !== 'ready') return
  if (live.page.value && live.failedIndex.value !== spreadIndex.value) {
    void requestTurn(live.failedIndex.value > spreadIndex.value ? 1 : -1)
  } else {
    live.reload(spreadIndex.value)
  }
}

function retryCommunity() {
  if (!community || phase.value !== 'ready') return
  if (community.result.value && community.failed.value !== spreadIndex.value) {
    void requestTurn(community.failed.value > spreadIndex.value ? 1 : -1)
  } else {
    void community.reload()
  }
}

function requestClose() {
  if (props.closeRequested) return
  if (phase.value === 'opening' || phase.value === 'turning') activeMotion?.progress(1)
  if (phase.value === 'ready') emit('requestClose')
}

watch([() => props.closeRequested, phase], ([requested, currentPhase]) => {
  if (requested && currentPhase === 'ready') animateReturn()
}, { flush: 'post' })

function animateReturn() {
  const layout = object.value?.layout
  if (phase.value !== 'ready' || !layout || !elements) return

  live?.cancel()
  community?.cancel()
  recommendations?.cancel()
  personal?.cancel()
  sound.stop()
  phase.value = 'closing'
  stage.value?.focus({ preventScroll: true })
  const origin = props.source.isConnected ? measureBook(props.source) : props.origin
  activeMotion = closeBook(elements, closedPosition(layout), origin, reduceMotion.value, () => {
    dialog.value?.close()
    emit('closed')
  })
}

function handleKeys(event: KeyboardEvent) {
  if (event.key === 'Escape' && settings.value?.dismiss()) {
    event.preventDefault()
    return
  }
  if (event.target instanceof Element && event.target.closest('input, textarea, select, [contenteditable]')) return
  if (event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return
  if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
    event.preventDefault()
    void requestTurn(event.key === 'ArrowRight' ? 1 : -1)
  }
}

onBeforeUnmount(() => {
  disposed = true
  activeMotion?.kill()
  viewport?.removeEventListener('change', updateDisplay)
  motionMedia?.removeEventListener('change', updateDisplay)
  sound.dispose()
  window.removeEventListener('resize', settleOnResize)
  document.removeEventListener('visibilitychange', trackVisibility)
  dialog.value?.close()
  releaseScroll?.()
})
</script>

<template>
  <Teleport to="body">
    <dialog ref="dialog" class="book-reader" aria-labelledby="reader-title" :data-reader-phase="phase"
      :data-reader-book="book.spine.id" :data-layout="singlePage ? 'single' : 'spread'"
      :data-motion="reduceMotion ? 'simple' : 'full'" :style="theme"
      @cancel.prevent="requestClose" @keydown="handleKeys">
      <div ref="backdrop" class="reader-backdrop" aria-hidden="true"></div>

      <div ref="stage" class="reader-stage" tabindex="-1">
        <div class="reader-shell" :class="{ 'reader-shell--plain': !live }">
          <header ref="toolbar" class="reader-toolbar" :class="{ 'reader-toolbar--bookmarks': recommendations || personal || (singlePage && live) }">
            <div class="reader-identity">
              <span class="reader-seal" aria-hidden="true"><BookSigil :kind="book.spine.emblem" /></span>
              <div>
                <h2 id="reader-title">{{ book.spine.title }}</h2>
                <p>{{ personal ? personal.account.profile?.displayName ?? '私人藏书室' : live ? '馆藏目录 · Oracle' : community ? '馆友手稿' : recommendations ? '馆藏荐读' : '示例馆藏 · Mock' }}</p>
              </div>
            </div>

            <PersonalTabs v-if="personal" :selected="reader.personalChapter" :unread="personal.unread.value"
              :disabled="phase !== 'ready' || personal.marking.value" @select="reader.personalChapter = $event" />
            <RecommendationFilter v-if="recommendations" :selected="recommendations.type.value"
              :disabled="phase !== 'ready'" @select="reader.recommendationType = $event" />
            <ReaderSearch v-if="singlePage && live" class="compact-search" :model-value="searchQuery"
              label="搜索作品" placeholder="搜作品名称或简介" :disabled="phase !== 'ready'"
              @update:model-value="setFilter('searchQuery', $event)" />
            <div class="reader-tools">
              <button v-if="personal?.account.profile" type="button"
                :disabled="turnLocked" @click="personal.refresh()">刷新</button>
              <ReaderSettings ref="settings" :motion="reader.motionPreference" :sound="reader.soundEnabled"
                :forced="singlePage || systemReduced" :system-reduced="systemReduced" :simple="reduceMotion"
                :inert="phase !== 'ready'" @motion="reader.motionPreference = $event" @sound="setSound" />
              <button ref="closeButton" type="button" class="reader-close" :disabled="phase !== 'ready'"
                @click="requestClose">合书返回</button>
            </div>
          </header>

          <div class="reader-desk">
            <div class="reader-spread">
              <div v-if="live && book.catalog" ref="genrePanel" class="reader-genre-panel">
                <button v-if="singlePage" type="button" class="genre-disclosure" :aria-expanded="tagsExpanded"
                  aria-controls="reader-tag-controls" :disabled="phase !== 'ready'" @click="tagsExpanded = !tagsExpanded">
                  题材标签{{ selectedTags.length ? ' · 已选 ' + selectedTags.length : '' }}
                  <span aria-hidden="true">{{ tagsExpanded ? '−' : '+' }}</span>
                </button>
                <div v-show="!singlePage || tagsExpanded" id="reader-tag-controls" class="reader-tag-controls">
                <CatalogFilters :kind="book.catalog" :tags="live.tagOptions.value"
                  :selected-tags="selectedTags" :disabled="phase !== 'ready'"
                  :loading="live.filtersLoading.value" :error="live.filterError.value" @retry="live.loadFilters()"
                  @tag="toggleTag" />
                </div>
              </div>
              <BookObject ref="object" :definition="book" :spread="displayedSpread" :turn="pendingTurn" :aria-busy="phase !== 'ready' || requestingTurn"
                :single-page="singlePage" :side="side" :slide-indexes="slideIndexes" :interactive="phase === 'ready'" :reduced="reduceMotion"
                :paused="carouselPaused" :search="searchQuery" :marking="personal?.marking.value || personal?.working.value"
                @mark-read="personal?.readNotice($event)" @search="setFilter('searchQuery', $event)"
                @select-featured="selectFeatured" @hold-featured="carouselHeld = $event"
                @toggle-featured-pause="carouselPaused = !carouselPaused" />
            </div>
          </div>

          <nav ref="navigation" class="reader-navigation" aria-label="翻页">
            <button ref="previousButton" type="button" aria-keyshortcuts="ArrowLeft" :disabled="turnLocked || !canGoBack"
              @click="requestTurn(-1)"><span aria-hidden="true">‹</span> 上一页</button>
            <p class="reader-page-range" aria-label="当前页码" aria-live="polite">{{ pageRange }}</p>
            <button ref="nextButton" type="button" aria-keyshortcuts="ArrowRight" :disabled="turnLocked || !canGoForward"
              @click="requestTurn(1)">下一页 <span aria-hidden="true">›</span></button>
          </nav>
        </div>
      </div>

      <div class="reader-activity" role="status">
        <span v-if="busyMessage">{{ busyMessage }}</span>
        <span v-else-if="personal?.working.value">正在调阅私人档案…</span>
        <span v-else-if="personal?.error.value" class="catalog-failure">{{ personal.error.value }}
          <button type="button" @click="personal.refresh()">重试私人档案</button>
        </span>
        <span v-else-if="community?.busy.value">正在调阅讨论…</span>
        <span v-else-if="community?.error.value" class="catalog-failure">{{ community.error.value }}
          <button type="button" @click="retryCommunity">重试档案</button>
        </span>
        <span v-else-if="recommendations?.busy.value">正在查询推荐…</span>
        <span v-else-if="recommendations?.error.value" class="catalog-failure">{{ recommendations.error.value }}
          <button type="button" @click="recommendations.retry()">重试荐读</button>
        </span>
        <span v-else-if="live?.loading.value">正在查询馆藏…</span>
        <span v-else-if="live?.error.value" class="catalog-failure">{{ live.error.value }}
          <button type="button" @click="retryPage">重试书页</button>
        </span>
      </div>

      <div ref="flight" class="travelling-book" aria-hidden="true" inert data-travelling-book>
        <BookCover :book="book" />
        <div ref="spine" class="travelling-spine">
          <BookSpine :book="book.spine" :selected="false" tabindex="-1" />
        </div>
      </div>
    </dialog>
  </Teleport>
</template>

<style scoped lang="scss" src="../../styles/book-reader.scss">
</style>

<style scoped lang="scss">
.reader-toolbar--bookmarks {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);

  .reader-tools { justify-self: end; }
}

@media (max-width: 46rem) {
  .reader-toolbar--bookmarks {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: .45rem;

    .reader-tools { grid-area: 1 / 2; }
    .recommendation-filter, .personal-tabs, .compact-search { grid-area: 2 / 1 / auto / -1; }
  }
}

@media (min-width: 46.001rem) and (max-width: 61rem) {
  [data-reader-book='personal'] .reader-toolbar--bookmarks {
    grid-template-columns: minmax(0, 1fr) auto;
    .reader-tools { grid-area: 1 / 2; }
    .personal-tabs { grid-area: 2 / 1 / auto / -1; }
  }
}

[data-reader-book='personal'] .reader-identity {
  min-width: 0;
  .reader-seal { flex: none; }
  > div { min-width: 0; }
  h2 { white-space: nowrap; }
  p { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
@media(max-width:22rem) {
  [data-reader-book='personal'] .reader-toolbar { gap: .35rem; }
  [data-reader-book='personal'] .reader-identity { gap: .3rem; }
  [data-reader-book='personal'] .reader-tools button { padding-inline: .32rem; }
}
</style>
