import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import type { Episode, PlaybackSource } from '../api/playback'
import { trackPlayback, type SavedEpisode } from '../api/viewing'
import { readEpisodePosition, restorePosition, saveEpisodePosition } from './resume'

interface Session {
  key: number
  workId: number
  episodeId: number
  edition: Episode['edition']
  episodeNumber: number
  url: string
  position: number
  sameEpisode: boolean
  resumePlay: boolean
}

export function useLibraryVideo(props: { workId: number; episode?: Episode; source?: PlaybackSource; autoplay: boolean; resume?: SavedEpisode },
                                onEnded: () => void, onCounted: () => void) {
  const video = ref<HTMLVideoElement | null>(null)
  const session = shallowRef<Session | null>(null)
  const duration = ref(0)
  const buffered = shallowRef<{ start: number; end: number }[]>([])
  const time = ref(0)
  const playing = ref(false)
  const muted = ref(false)
  const volume = ref(0.8)
  const speed = ref(1)
  const state = ref<'loading' | 'ready' | 'buffering' | 'failed'>('loading')
  const revision = ref(0)
  let nextKey = 0
  let lastSaved = -10
  let resumeOnRetry = false
  let recorder: ReturnType<typeof trackPlayback> | null = null
  let tracking = false
  let flowing = false
  let lastSample = 0
  let lastPosition = 0
  let wakeLock: WakeLockSentinel | undefined
  let active = true

  function sample(discontinuity = false, leaving = false) {
    const media = video.value
    if (!recorder || !tracking || !media || !Number.isFinite(media.duration) || media.duration <= 0) return
    const positionMs = Math.floor(media.currentTime * 1000)
    if (!discontinuity && (!flowing || positionMs === lastPosition)) return
    const form = { positionMs, durationMs: Math.ceil(media.duration * 1000), playing: flowing, discontinuity }
    if (leaving) recorder.leave(form)
    else recorder.sample(form)
    lastPosition = positionMs
    lastSample = performance.now()
  }

  async function keepAwake() {
    if (wakeLock || !navigator.wakeLock || !flowing || document.hidden) return
    try {
      const next = await navigator.wakeLock.request('screen')
      if (!active || !flowing || wakeLock) await next.release()
      else {
        wakeLock = next
        next.addEventListener('release', () => { if (wakeLock === next) wakeLock = undefined }, { once: true })
      }
    } catch { /* Browser or device settings can deny the optional wake lock. */ }
  }

  function stopFlow() {
    sample()
    flowing = false
    void wakeLock?.release().catch(() => undefined)
    wakeLock = undefined
  }

  function pageHide() { writeProgress(true); sample(false, true) }

  function visibilityChanged() {
    if (document.hidden) sample()
    else void keepAwake()
  }

  const ready = computed(() => duration.value > 0 && state.value !== 'failed')

  function writeProgress(force = false) {
    if (!session.value || !ready.value) return
    const media = video.value
    const current = media?.currentTime ?? time.value
    if (!media?.ended && (current === lastSaved || !force && Math.abs(current - lastSaved) < 2)) return
    const position = media?.ended || current >= duration.value - 0.2 ? 0 : current
    saveEpisodePosition(session.value.workId, {
      id: session.value.episodeId, edition: session.value.edition, number: session.value.episodeNumber,
    }, position)
    lastSaved = current
  }

  watch([() => props.workId, () => props.episode?.id, () => props.source?.id, revision], () => {
    const previous = session.value
    const nextEpisode = props.episode
    const sameEpisode = !!previous && !!nextEpisode && previous.workId === props.workId
      && previous.edition === nextEpisode.edition && previous.episodeNumber === nextEpisode.number
    const offset = sameEpisode ? (video.value?.currentTime ?? time.value)
      : nextEpisode ? readEpisodePosition(props.workId, nextEpisode, props.resume) : 0
    const continuePlaying = sameEpisode ? playing.value || resumeOnRetry : props.autoplay
    writeProgress(true)
    stopFlow()
    tracking = false
    video.value?.pause()
    recorder = props.source && nextEpisode ? trackPlayback(props.workId, props.source.id, onCounted) : null

    session.value = props.source && nextEpisode ? {
      key: ++nextKey, workId: props.workId, episodeId: nextEpisode.id,
      edition: nextEpisode.edition, episodeNumber: nextEpisode.number,
      url: props.source.url, position: offset, sameEpisode, resumePlay: continuePlaying,
    } : null
    time.value = offset
    duration.value = 0
    buffered.value = []
    playing.value = false
    lastSaved = offset
    resumeOnRetry = false
    state.value = 'loading'
  }, { immediate: true })

  function matches(event: Event): event is Event & { target: HTMLVideoElement } {
    return !!session.value && event.target === video.value
      && video.value?.dataset.session === String(session.value.key)
  }

  async function play() {
    const element = video.value
    if (!element || state.value === 'failed') return
    try { await element.play() } catch {
      if (element === video.value) playing.value = false
    }
  }

  function metadata(event: Event) {
    if (!matches(event) || !session.value) return
    const element = event.target
    duration.value = Number.isFinite(element.duration) ? element.duration : props.episode?.durationSeconds ?? 0
    const position = restorePosition(session.value.position, duration.value, session.value.sameEpisode)
    time.value = position
    lastSaved = position
    if (position) element.currentTime = position
    bufferProgress(event)
    element.playbackRate = speed.value
    element.volume = volume.value
    element.muted = muted.value
    state.value = 'ready'
    if (session.value.resumePlay) void play()
  }

  function seek(seconds: number) {
    if (!ready.value || !video.value || !Number.isFinite(seconds)) return
    const position = Math.min(duration.value, Math.max(0, seconds))
    sample()
    video.value.currentTime = position
    sample(true)
    time.value = position
    writeProgress(true)
  }

  function bufferProgress(event: Event) {
    if (!matches(event) || duration.value <= 0) return
    const ranges = event.target.buffered
    buffered.value = Array.from({ length: ranges.length }, (_, index) => ({
      start: ranges.start(index) / duration.value * 100,
      end: Math.min(100, ranges.end(index) / duration.value * 100),
    }))
  }

  function progress(event: Event) {
    if (!matches(event) || !ready.value) return
    time.value = event.target.currentTime
    if (flowing && !event.target.seeking && performance.now() - lastSample >= 18_000) sample()
    writeProgress()
  }

  function toggle() {
    if (playing.value) video.value?.pause()
    else void play()
  }

  function resumed(event: Event) {
    if (!matches(event)) return
    playing.value = true
    state.value = 'ready'
    if (!flowing) {
      const position = Math.floor(event.target.currentTime * 1000)
      flowing = true
      lastSample = performance.now()
      if (!tracking) {
        tracking = true
        lastPosition = position
        recorder?.begin(position)
      } else sample(true)
      void keepAwake()
    }
  }

  function pause(event: Event) {
    if (!matches(event)) return
    stopFlow()
    playing.value = false
    if (state.value === 'buffering') state.value = 'ready'
    writeProgress(true)
  }

  function buffer(event: Event) {
    if (matches(event) && !event.target.paused) {
      if (!event.target.seeking) stopFlow()
      else flowing = false
      state.value = 'buffering'
    }
  }

  function error(event: Event) {
    if (!matches(event)) return
    stopFlow()
    resumeOnRetry = playing.value
    playing.value = false
    state.value = 'failed'
  }

  function ended(event: Event) {
    if (!matches(event)) return
    stopFlow()
    playing.value = false
    writeProgress(true)
    onEnded()
  }

  function retry() { revision.value++ }

  function setVolume(value: number) {
    volume.value = value
    muted.value = value === 0
    if (video.value) {
      video.value.volume = volume.value
      video.value.muted = muted.value
    }
  }

  function toggleMute() {
    muted.value = !muted.value
    if (muted.value === false && volume.value === 0) volume.value = 0.6
    if (video.value) {
      video.value.volume = volume.value
      video.value.muted = muted.value
    }
  }

  function setSpeed(value: number) {
    speed.value = value
    if (video.value) video.value.playbackRate = value
  }

  onMounted(() => {
    window.addEventListener('pagehide', pageHide)
    document.addEventListener('visibilitychange', visibilityChanged)
  })
  onBeforeUnmount(() => {
    active = false
    writeProgress(true)
    stopFlow()
    video.value?.pause()
    window.removeEventListener('pagehide', pageHide)
    document.removeEventListener('visibilitychange', visibilityChanged)
  })
  return { video, session, duration, buffered, time, playing, muted, volume, speed, state, ready,
    metadata, seek, progress, bufferProgress, toggle, resumed, pause, buffer, error, ended, retry, setVolume, toggleMute, setSpeed }
}
