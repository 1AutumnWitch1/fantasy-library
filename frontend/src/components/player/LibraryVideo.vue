<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { episodeLabel, timeLabel, type Episode, type PlaybackSource } from '../../api/playback'
import type { SavedEpisode } from '../../api/viewing'
import { useLibraryVideo } from '../../player/useLibraryVideo'

const props = defineProps<{
  workId: number
  title: string
  episode?: Episode
  source?: PlaybackSource
  poster?: string
  autoplay: boolean
  resume?: SavedEpisode
  qualitySources?: PlaybackSource[]
  loading: boolean
  catalogError?: string
  alternate: boolean
  previous?: boolean
  next?: boolean
}>()
const emit = defineEmits<{ ended: []; alternate: []; previous: []; next: []; retryCatalog: []; counted: []; back: []; quality: [PlaybackSource] }>()
const {
  video, session, duration, buffered, time, playing, muted, volume, speed, state, ready,
  metadata, seek, progress, bufferProgress, toggle, resumed, pause, buffer, error, ended, retry, setVolume, toggleMute, setSpeed,
} = useLibraryVideo(props, () => emit('ended'), () => emit('counted'))
const room = ref<HTMLElement | null>(null)
const controlError = ref('')
interface WebKitVideo extends HTMLVideoElement {
  webkitPresentationMode?: 'inline' | 'picture-in-picture'
  webkitSupportsPresentationMode?: (mode: 'picture-in-picture') => boolean
  webkitSetPresentationMode?: (mode: 'inline' | 'picture-in-picture') => void
}

const inPip = ref(false)
const supportsPip = computed(() => {
  const media: WebKitVideo | null = video.value
  if (!media || !ready.value) return false
  return (document.pictureInPictureEnabled && typeof media.requestPictureInPicture === 'function')
    || Boolean(media.webkitSetPresentationMode && media.webkitSupportsPresentationMode?.('picture-in-picture'))
})
const shown = ref(true)
const inFullscreen = ref(false)
const menuFocused = ref(false)
const brightness = ref(1)
const sliderPreview = ref<number | null>(null)
const hud = ref('')
const quality = computed(() => [...new Map((props.qualitySources ?? []).filter(item => item.quality)
  .map(item => [item.quality, item])).values()])
const displayTime = computed(() => sliderPreview.value ?? time.value)
const percent = computed(() => duration.value > 0 ? (displayTime.value / duration.value) * 100 : 0)
let controlsTimer = 0
let hudTimer = 0
let singleTap = 0
let lastTapAt = -1_000
let lastTapSide = ''
let gesture: {
  id: number; x: number; y: number; width: number; height: number;
  at: number; brightness: number; volume: number; region: 'left' | 'middle' | 'right'; rightSide: boolean;
  axis?: 'seek' | 'brightness' | 'volume'
} | null = null

function reveal() {
  shown.value = true
  window.clearTimeout(controlsTimer)
  if (playing.value && state.value === 'ready' && !gesture?.axis && sliderPreview.value === null && !menuFocused.value) {
    controlsTimer = window.setTimeout(() => { shown.value = false }, 3000)
  }
}

watch([playing, state, menuFocused, () => session.value?.key], reveal)

function syncFullscreen() {
  inFullscreen.value = document.fullscreenElement === room.value && room.value !== null
  if (!inFullscreen.value) screen.orientation?.unlock?.()
  reveal()
}

function back() {
  if (inFullscreen.value) void fullscreen()
  else emit('back')
}

onMounted(() => document.addEventListener('fullscreenchange', syncFullscreen))
onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', syncFullscreen)
  window.clearTimeout(controlsTimer)
  window.clearTimeout(hudTimer)
  window.clearTimeout(singleTap)
})

function feedback(message: string) {
  window.clearTimeout(hudTimer)
  hud.value = message
  if (!gesture?.axis && sliderPreview.value === null) {
    hudTimer = window.setTimeout(() => { hud.value = '' }, 900)
  }
}

function beginDrag(event: PointerEvent) {
  if (!session.value || event.button !== 0 || !event.isPrimary) return
  const surface = event.currentTarget as HTMLElement
  const rect = surface.getBoundingClientRect()
  const x = event.clientX - rect.left
  const region = x < rect.width / 3 ? 'left' : x > rect.width * 2 / 3 ? 'right' : 'middle'
  gesture = { id: event.pointerId, x: event.clientX, y: event.clientY, width: rect.width, height: rect.height,
    at: time.value, brightness: brightness.value, volume: muted.value ? 0 : volume.value, region, rightSide: x >= rect.width / 2 }
  surface.setPointerCapture(event.pointerId)
}

function moveDrag(event: PointerEvent) {
  const start = gesture
  if (!start) {
    if (event.pointerType === 'mouse') reveal()
    return
  }
  if (start.id !== event.pointerId) return
  const dx = event.clientX - start.x
  const dy = event.clientY - start.y
  if (!start.axis && Math.hypot(dx, dy) < 9) return
  start.axis ??= Math.abs(dx) >= Math.abs(dy) ? 'seek' : start.rightSide ? 'volume' : 'brightness'
  reveal()
  if (start.axis === 'seek' && ready.value) {
    const preview = Math.max(0, Math.min(duration.value, start.at + dx / start.width * Math.min(120, duration.value)))
    sliderPreview.value = preview
    const difference = Math.round(preview - start.at)
    feedback((difference < 0 ? '快退 ' : '快进 ') + Math.abs(difference) + ' 秒 · ' + timeLabel(preview))
  } else if (start.axis === 'brightness') {
    brightness.value = Math.max(.06, Math.min(1.4, start.brightness - dy / start.height))
    feedback('☀ 亮度 ' + Math.round(brightness.value * 100) + '%')
  } else if (start.axis === 'volume') {
    setVolume(Math.max(0, Math.min(1, start.volume - dy / start.height)))
    feedback('♪ 音量 ' + Math.round(volume.value * 100) + '%')
  }
}

function endDrag(event: PointerEvent) {
  const start = gesture
  if (!start || start.id !== event.pointerId) return
  gesture = null
  if (start.axis) {
    if (event.type === 'pointerup' && start.axis === 'seek' && sliderPreview.value !== null) seek(sliderPreview.value)
    sliderPreview.value = null
    feedback(hud.value)
    reveal()
  } else if (event.type === 'pointerup') {
    const now = performance.now()
    if (now - lastTapAt <= 300 && lastTapSide === start.region) {
      window.clearTimeout(singleTap)
      lastTapAt = -1_000
      if (start.region === 'middle') {
        togglePlayback()
        feedback(playing.value ? '暂停或继续' : '继续播放')
      } else {
        seek(time.value + (start.region === 'left' ? -10 : 10))
        feedback(start.region === 'left' ? '快退 10 秒' : '快进 10 秒')
      }
    } else {
      window.clearTimeout(singleTap)
      lastTapAt = now
      lastTapSide = start.region
      singleTap = window.setTimeout(reveal, 300)
    }
  }
}

function sliderInput(event: Event) {
  sliderPreview.value = Number((event.target as HTMLInputElement).value)
  reveal()
  feedback(timeLabel(sliderPreview.value))
}

function sliderCommit(event: Event) {
  seek(Number((event.target as HTMLInputElement).value))
  sliderPreview.value = null
  feedback(hud.value)
  reveal()
}

function qualityChange(event: Event) {
  const id = Number((event.target as HTMLSelectElement).value)
  const source = quality.value.find(option => option.id === id)
  if (source) emit('quality', source)
  reveal()
}

function togglePlayback() {
  controlError.value = ''
  reveal()
  toggle()
}

async function fullscreen() {
  controlError.value = ''
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else if (room.value?.requestFullscreen) {
      await room.value.requestFullscreen()
      const orientation = screen.orientation as ScreenOrientation & { lock?: (mode: 'landscape') => Promise<void> }
      if (matchMedia('(pointer: coarse)').matches) void orientation.lock?.('landscape').catch(() => undefined)
    }
    else {
      const media = video.value as (HTMLVideoElement & { webkitEnterFullscreen?: () => void }) | null
      if (!media?.webkitEnterFullscreen) throw new Error('Fullscreen unavailable')
      media.webkitEnterFullscreen()
    }
  } catch {
    controlError.value = '当前浏览器无法切换全屏。'
  }
}

function syncPip() {
  const media: WebKitVideo | null = video.value
  inPip.value = Boolean(media && (document.pictureInPictureElement === media || media.webkitPresentationMode === 'picture-in-picture'))
}

async function miniWindow() {
  controlError.value = ''
  const media: WebKitVideo | null = video.value
  if (!media) return
  try {
    if (document.pictureInPictureElement) await document.exitPictureInPicture()
    else if (document.pictureInPictureEnabled && typeof media.requestPictureInPicture === 'function') await media.requestPictureInPicture()
    else media.webkitSetPresentationMode?.(inPip.value ? 'inline' : 'picture-in-picture')
  } catch {
    controlError.value = '当前视频无法切换小窗。'
  }
}

function keyboard(event: KeyboardEvent) {
  if (event.target instanceof Element && event.target.closest('input, select, button, a')) return
  if (event.altKey || event.ctrlKey || event.metaKey || event.shiftKey || !session.value) return
  if (event.key === ' ' || event.key.toLowerCase() === 'k') togglePlayback()
  else if (event.key === 'ArrowLeft') seek(time.value - 5)
  else if (event.key === 'ArrowRight') seek(time.value + 5)
  else if (event.key.toLowerCase() === 'f') void fullscreen()
  else return
  reveal()
  event.preventDefault()
}
</script>

<template>
  <section ref="room" class="film-player" aria-label="馆藏播放器" tabindex="0" @keydown="keyboard" @focusin="reveal">
    <div class="film-screen">
      <img v-if="poster && (!session || (!playing && time < 0.1))" class="screen-art"
        :src="poster" alt="" decoding="async">
      <video v-if="session" :key="session.key" ref="video" :data-session="session.key"
        :src="session.url" :aria-label="title + (episode ? ' · ' + episodeLabel(episode) : '')"
        preload="metadata" playsinline :style="{ filter: 'brightness(' + brightness + ')' }"
        @loadedmetadata="metadata" @timeupdate="progress" @progress="bufferProgress"
        @playing="resumed" @pause="pause" @waiting="buffer" @ended="ended" @error="error"
        @enterpictureinpicture="syncPip" @leavepictureinpicture="syncPip" @webkitpresentationmodechanged="syncPip"
        />
      <div v-if="session" class="film-gestures" @pointerenter="reveal" @pointerdown="beginDrag" @pointermove="moveDrag"
        @pointerup="endDrag" @pointercancel="endDrag"></div>
      <div v-if="loading || catalogError || !session || state === 'failed'" class="film-overlay film-message">
        <template v-if="loading"><p role="status">正在调阅放映目录…</p></template>
        <template v-else-if="catalogError">
          <p role="alert">{{ catalogError }}</p>
          <div class="film-actions"><button type="button" @click="emit('retryCatalog')">重试放映目录</button></div>
        </template>
        <template v-else-if="state === 'failed'">
          <p role="alert">当前节点播放失败</p>
          <div class="film-actions">
            <button type="button" @click="retry">重试当前节点</button>
            <button v-if="alternate" type="button" @click="emit('alternate')">换一个节点</button>
          </div>
        </template>
        <p v-else>暂未收录播放源</p>
      </div>
      <div v-else-if="!playing" class="film-overlay film-idle">
        <button type="button" class="film-play" aria-label="播放视频" @click="togglePlayback"><span aria-hidden="true">▶</span></button>
        <span>{{ time > 0 ? '继续播放' : '点击播放' }}</span>
      </div>
      <p v-else-if="state === 'buffering'" class="film-buffer" role="status">正在缓冲…</p>
      <div v-if="shown" class="film-top">
        <button type="button" @click="back">{{ inFullscreen ? '‹ 退出全屏' : '‹ 返回' }}</button>
        <span>{{ episode ? episodeLabel(episode) : title }}</span>
        <small v-if="episode?.edition === 'PREVIEW' && session">馆藏预告</small>
      </div>
      <output v-if="hud" class="film-hud">{{ hud }}</output>

    <div v-if="shown" class="film-controls" @pointerdown="reveal" @pointermove="reveal">
      <label class="film-seek">
        <span class="sr-only">播放进度</span>
        <span class="film-track" aria-hidden="true">
          <span v-for="range in buffered" :key="range.start" class="film-buffered"
            :style="{ left: range.start + '%', width: (range.end - range.start) + '%' }"></span>
          <span class="film-played" :style="{ width: percent + '%' }"></span>
        </span>
        <input type="range" aria-label="播放进度" min="0" :max="duration || 1" step="0.1"
          :value="displayTime" :disabled="!ready"
          :aria-valuetext="timeLabel(displayTime) + ' / ' + timeLabel(duration)"
          @input="sliderInput" @change="sliderCommit">
      </label>
      <div class="film-control-row">
        <div class="film-transport">
          <button type="button" :aria-label="playing ? '暂停' : '播放'" :disabled="!session || state === 'failed'"
            @click="togglePlayback"><span aria-hidden="true">{{ playing ? 'Ⅱ' : '▶' }}</span></button>
          <button v-if="previous" type="button" aria-label="上一段" @click="emit('previous')">‹</button>
          <button v-if="next" type="button" aria-label="下一段" @click="emit('next')">›</button>
          <output class="film-time">{{ timeLabel(displayTime) }} <span>/</span> {{ timeLabel(duration || episode?.durationSeconds || 0) }}</output>
        </div>
        <div class="film-adjust">
          <button type="button" :aria-label="muted ? '取消静音' : '静音'" :aria-pressed="muted" :disabled="!session"
            @click="toggleMute">{{ muted ? '静音' : '音量' }}</button>
          <input class="film-volume" type="range" aria-label="音量大小" min="0" max="1" step=".05" :disabled="!session"
            :value="muted ? 0 : volume" @input="setVolume(Number(($event.target as HTMLInputElement).value))">
          <select aria-label="播放倍速" :value="speed" :disabled="!session" @focus="menuFocused = true" @blur="menuFocused = false" @change="setSpeed(Number(($event.target as HTMLSelectElement).value))">
            <option v-for="rate in [0.75, 1, 1.25, 1.5, 2]" :key="rate" :value="rate">{{ rate }}×</option>
          </select>
          <select v-if="quality.length > 1" aria-label="清晰度" :value="source?.id" @focus="menuFocused = true" @blur="menuFocused = false" @change="qualityChange">
            <option v-for="item of quality" :key="item.id" :value="item.id">{{ item.quality }}</option>
          </select>
          <button v-if="supportsPip" type="button" class="film-pip" :aria-pressed="inPip" @click="miniWindow">{{ inPip ? '关闭小窗' : '小窗' }}</button>
          <button type="button" :disabled="!session" @click="fullscreen">{{ inFullscreen ? '退出全屏' : '全屏' }}</button>
        </div>
      </div>
      <p v-if="controlError" class="film-control-error" role="status">{{ controlError }}</p>
    </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.film-player {
  overflow: hidden;
  position: relative;
  border-radius: .36rem;
  outline-offset: 3px;
  border: 1px solid #ac855b;
  background: #131716;
  box-shadow: 0 .6rem 1.6rem #090605a8, inset 0 0 0 2px #523b2b;
}
.film-screen {
  position: relative;
  isolation: isolate;
  display: grid;
  place-items: center;
  overflow: hidden;
  aspect-ratio: 16/9;
  background: radial-gradient(ellipse at 45% 40%, #253930, #111816 70%);
}
.screen-art, video {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
}
.screen-art { z-index:1; object-fit:cover; object-position:center 44%; pointer-events:none; }
video { object-fit: contain; cursor: pointer; }
.film-overlay {
  position:absolute; inset:0; z-index:2; display:grid; place-content:center; justify-items:center;
  gap: .57rem; color: #f5dfb4; background: #10131334; text-align: center; pointer-events: none;
}
.film-idle { background: radial-gradient(ellipse at center, #0b131664, #10131116 75%); }
.film-idle > span { font-size: .9rem; font-weight: 700; text-shadow: 0 2px 7px #050807; }
.film-message { background: #10171599; padding: .7rem; }
.film-message p { margin: 0; color:#f7e4be; font-size: clamp(.93rem,2vw,1.22rem); }
.film-play {
  display:grid; place-items:center; width:clamp(2.85rem,5vw,4rem); aspect-ratio:1;
  border-radius:50%; border:1px solid #f9d393; background: #172d25d9;
  box-shadow:0 0 0 4px #202f29b5, 0 .5rem 1.5rem #0b100fe8;
  color:#ffe4ac; font-size:clamp(1.13rem,2vw,1.45rem); cursor:pointer; pointer-events:auto;
}
.film-play span { margin-left:.12em; }
.film-play:hover { background:#324e3f; }
.film-actions { display:flex; flex-wrap:wrap; justify-content:center; gap:.42rem; }
.film-actions button { pointer-events:auto; }
.preview-label {
  position:absolute; z-index:3; right:.65rem; top:.65rem; padding:.16rem .43rem;
  border:1px solid #c79e6a90; background:#182721ed; color:#ebd2a1; font-size:.75rem; pointer-events:none;
}
.film-buffer {
  position:absolute; z-index:2; margin:0; padding:.2rem .6rem; color:#f6dfae; background:#1d1e14cc;
}
.film-gestures { position: absolute; inset: 0; z-index: 1; touch-action: none; cursor: pointer; }
.film-top {
  position: absolute; inset: 0 0 auto; z-index: 3; display: flex; align-items: center; gap: .5rem;
  padding: .2rem .45rem; color: #f3daaa; background: linear-gradient(#130e09e8, transparent);
  pointer-events: none;
}
.film-top span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.film-top button { color: inherit; background: #1c241dd1; border: 0; min-height: 2.5rem; padding: .3rem .55rem; pointer-events: auto; cursor: pointer; }
.film-hud { position: absolute; z-index: 4; padding: .65rem 1rem; border-radius: .35rem; color: #f9e6bc; background: #17120ddd; pointer-events: none; }
.film-controls {
  position: absolute; inset: auto 0 0; z-index: 3;
  color: #f3daaa; padding: .17rem clamp(.45rem,1.2vw,.95rem) .28rem;
  background: linear-gradient(transparent, #120d09ed 45%);
}
.film-seek { position: relative; display: block; }
.film-track {
  position: absolute;
  inset: 50% .38rem auto;
  height: .25rem;
  translate: 0 -50%;
  overflow: hidden;
  border-radius: 999px;
  background: #f2e3c35e;
  pointer-events: none;
}
.film-buffered, .film-played { position: absolute; inset: 0 auto 0 0; }
.film-buffered { background: #f2e3c370; }
.film-played { background: #ddb16b; }
.film-seek input {
  position: relative; width:100%; height:1.55rem; margin:0; appearance:none; background:transparent; cursor:pointer;
  &::-webkit-slider-runnable-track { height:.25rem; background:transparent; border-radius:3px; }
  &::-moz-range-track { height:.25rem; background:transparent; border-radius:3px; }
  &::-moz-range-progress { background:transparent; height:.25rem; }
  &::-webkit-slider-thumb { appearance:none; width:.77rem; height:.77rem; margin-top:-.26rem; border-radius:50%; background:#f1cb8a; }
  &::-moz-range-thumb { width:.73rem; height:.73rem; border:0; border-radius:50%; background:#f1cb8a; }
}
.film-control-row, .film-transport, .film-adjust { display:flex; align-items:center; gap:.3rem; min-width:0; }
.film-control-row { justify-content:space-between; flex-wrap:wrap; }
.film-time { font: .84rem/1.4 var(--reading-font); white-space:nowrap; font-variant-numeric: tabular-nums; }
.film-time span { color:#b89f78; }
.film-control-row button, .film-actions button, select {
  min-height:2.35rem; min-width:2.35rem; padding:.22rem .48rem; font: .84rem/1.25 var(--reading-font);
  border:1px solid transparent; border-radius:.25rem; background:#2c372b; color:#f4ddb2; cursor:pointer;
}
.film-control-row button { background:transparent; }
.film-control-row button:hover:enabled, select:hover:enabled, .film-actions button:hover { border-color:#a88b60; background:#3e4c3b; }
.film-transport button { font-size:1.08rem; }
.film-volume { width:clamp(2.3rem,6vw,4.6rem); accent-color:#dfb576; cursor:pointer; }
:disabled { opacity:.43; cursor:default; }
.film-control-error { margin:.2rem 0 0; font-size:.82rem; color:#ffbf9f; }
.sr-only { position:absolute; width:1px; height:1px; overflow:hidden; clip-path:inset(50%); white-space:nowrap; }
.film-player:fullscreen {
  border:0; border-radius:0; background:#050505;
  .film-screen { height:100%; aspect-ratio:auto; background:#050505; }
}
@media (max-width: 34rem) {
  .film-volume { display:none; }
  .film-transport button[aria-label='上一段'], .film-transport button[aria-label='下一段'] { display:none; }
  .film-time { font-size:.76rem; }
  .film-control-row button, select { font-size:.78rem; min-width:1.92rem; padding-inline:.37rem; }
}
</style>
