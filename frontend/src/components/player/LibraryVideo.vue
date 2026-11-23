<script setup lang="ts">
import { computed, ref } from 'vue'
import { episodeLabel, timeLabel, type Episode, type PlaybackSource } from '../../api/playback'
import { useLibraryVideo } from '../../player/useLibraryVideo'

const props = defineProps<{
  workId: number
  title: string
  episode?: Episode
  source?: PlaybackSource
  poster?: string
  autoplay: boolean
  loading: boolean
  catalogError?: string
  alternate: boolean
  previous?: boolean
  next?: boolean
}>()
const emit = defineEmits<{ ended: []; alternate: []; previous: []; next: []; retryCatalog: [] }>()
const {
  video, session, duration, time, playing, muted, volume, speed, state, ready,
  metadata, seek, progress, toggle, resumed, pause, buffer, error, ended, retry, setVolume, toggleMute, setSpeed,
} = useLibraryVideo(props, () => emit('ended'))
const room = ref<HTMLElement | null>(null)
const controlError = ref('')
const supportsPip = computed(() => !!video.value && document.pictureInPictureEnabled &&
  typeof video.value.requestPictureInPicture === 'function')
const percent = computed(() => duration.value > 0 ? (time.value / duration.value) * 100 : 0)

function togglePlayback() {
  controlError.value = ''
  toggle()
}

async function fullscreen() {
  controlError.value = ''
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else if (room.value?.requestFullscreen) await room.value.requestFullscreen()
    else {
      const media = video.value as (HTMLVideoElement & { webkitEnterFullscreen?: () => void }) | null
      if (!media?.webkitEnterFullscreen) throw new Error('Fullscreen unavailable')
      media.webkitEnterFullscreen()
    }
  } catch {
    controlError.value = '当前浏览器无法切换全屏。'
  }
}

async function miniWindow() {
  controlError.value = ''
  try {
    if (document.pictureInPictureElement) await document.exitPictureInPicture()
    else await video.value?.requestPictureInPicture()
  } catch {
    controlError.value = '当前视频无法进入小窗。'
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
  event.preventDefault()
}
</script>

<template>
  <section ref="room" class="film-player" aria-label="馆藏播放器" tabindex="0" @keydown="keyboard">
    <div class="film-screen">
      <img v-if="poster && (!session || (!playing && time < 0.1))" class="screen-art"
        :src="poster" alt="" decoding="async">
      <video v-if="session" :key="session.key" ref="video" :data-session="session.key"
        :src="session.url" :aria-label="title + (episode ? ' · ' + episodeLabel(episode) : '')"
        preload="metadata" playsinline
        @loadedmetadata="metadata" @timeupdate="progress" @play="resumed"
        @playing="resumed" @pause="pause" @waiting="buffer" @ended="ended" @error="error"
        @click="togglePlayback" />
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
      <span v-if="episode?.edition === 'PREVIEW' && session" class="preview-label">馆藏演示</span>
    </div>

    <div class="film-controls">
      <label class="film-seek">
        <span class="sr-only">播放进度</span>
        <input type="range" aria-label="播放进度" min="0" :max="duration || 1" step="0.1"
          :value="time" :disabled="!ready" :style="{ '--progress': percent + '%' }"
          :aria-valuetext="timeLabel(time) + ' / ' + timeLabel(duration)"
          @input="seek(Number(($event.target as HTMLInputElement).value))">
      </label>
      <div class="film-control-row">
        <div class="film-transport">
          <button type="button" :aria-label="playing ? '暂停' : '播放'" :disabled="!session || state === 'failed'"
            @click="togglePlayback"><span aria-hidden="true">{{ playing ? 'Ⅱ' : '▶' }}</span></button>
          <button v-if="previous" type="button" aria-label="上一段" @click="emit('previous')">‹</button>
          <button v-if="next" type="button" aria-label="下一段" @click="emit('next')">›</button>
          <output class="film-time">{{ timeLabel(time) }} <span>/</span> {{ timeLabel(duration || episode?.durationSeconds || 0) }}</output>
        </div>
        <div class="film-adjust">
          <button type="button" :aria-label="muted ? '取消静音' : '静音'" :aria-pressed="muted" :disabled="!session"
            @click="toggleMute">{{ muted ? '静音' : '音量' }}</button>
          <input class="film-volume" type="range" aria-label="音量大小" min="0" max="1" step=".05" :disabled="!session"
            :value="muted ? 0 : volume" @input="setVolume(Number(($event.target as HTMLInputElement).value))">
          <select aria-label="播放倍速" :value="speed" :disabled="!session" @change="setSpeed(Number(($event.target as HTMLSelectElement).value))">
            <option v-for="rate in [0.75, 1, 1.25, 1.5, 2]" :key="rate" :value="rate">{{ rate }}×</option>
          </select>
          <button v-if="supportsPip" type="button" class="film-pip" :disabled="!ready" @click="miniWindow">小窗</button>
          <button type="button" :disabled="!session" @click="fullscreen">全屏</button>
        </div>
      </div>
      <p v-if="controlError" class="film-control-error" role="status">{{ controlError }}</p>
    </div>
  </section>
</template>

<style scoped lang="scss">
.film-player {
  overflow: hidden;
  position: relative;
  border-radius: .5rem;
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
  display:grid; place-items:center; width:clamp(3.1rem,7.7vw,5.75rem); aspect-ratio:1;
  border-radius:50%; border:1px solid #f9d393; background: #172d25d9;
  box-shadow:0 0 0 4px #202f29b5, 0 .5rem 1.5rem #0b100fe8;
  color:#ffe4ac; font-size:clamp(1.2rem,3vw,2rem); cursor:pointer; pointer-events:auto;
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
  position:absolute; inset:auto auto .6rem; margin:0; padding:.2rem .6rem; color:#f6dfae; background:#1d1e14cc;
}
.film-controls {
  color: #f3daaa; padding: .17rem clamp(.45rem,1.2vw,.95rem) .44rem;
  border-top: 1px solid #b38b5d91; background: linear-gradient(#232b24,#1a211d);
}
.film-seek { display:block; }
.film-seek input {
  width:100%; height:1.55rem; margin:0; appearance:none; background:transparent; cursor:pointer;
  &::-webkit-slider-runnable-track { height:.25rem; background:linear-gradient(to right,#d5ab6d var(--progress),#72674b var(--progress)); border-radius:3px; }
  &::-moz-range-track { height:.25rem; background:#72674b; border-radius:3px; }
  &::-moz-range-progress { background:#d5ab6d; height:.25rem; }
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
  display:grid; grid-template-rows:minmax(0,1fr) auto; border:0; border-radius:0;
  .film-screen { aspect-ratio:auto; }
}
@media (max-width: 34rem) {
  .film-volume { display:none; }
  .film-pip { display:none; }
  .film-time { font-size:.76rem; }
  .film-control-row button, select { font-size:.78rem; min-width:1.92rem; padding-inline:.37rem; }
}
</style>
