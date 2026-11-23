import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import type { Episode, PlaybackSource } from '../api/playback'
import { readPosition, restorePosition, savePosition } from './resume'

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

export function useLibraryVideo(props: { workId: number; episode?: Episode; source?: PlaybackSource; autoplay: boolean },
                                onEnded: () => void) {
  const video = ref<HTMLVideoElement | null>(null)
  const session = shallowRef<Session | null>(null)
  const duration = ref(0)
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

  const ready = computed(() => duration.value > 0 && state.value !== 'failed')

  function writeProgress(force = false) {
    if (!session.value || !ready.value) return
    if (!force && Math.abs(time.value - lastSaved) < 2) return
    const media = video.value
    const position = media?.ended || time.value >= duration.value - 0.2 ? 0 : time.value
    savePosition(session.value.workId, session.value.episodeId, position)
    lastSaved = time.value
  }

  watch([() => props.workId, () => props.episode?.id, () => props.source?.id, revision], () => {
    const previous = session.value
    const nextEpisode = props.episode
    const sameEpisode = !!previous && !!nextEpisode && previous.workId === props.workId
      && previous.edition === nextEpisode.edition && previous.episodeNumber === nextEpisode.number
    const offset = sameEpisode ? time.value : nextEpisode ? readPosition(props.workId, nextEpisode.id) : 0
    const continuePlaying = sameEpisode ? playing.value || resumeOnRetry : props.autoplay
    writeProgress(true)
    video.value?.pause()

    session.value = props.source && nextEpisode ? {
      key: ++nextKey, workId: props.workId, episodeId: nextEpisode.id,
      edition: nextEpisode.edition, episodeNumber: nextEpisode.number,
      url: props.source.url, position: offset, sameEpisode, resumePlay: continuePlaying,
    } : null
    time.value = offset
    duration.value = 0
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
    if (position) element.currentTime = position
    element.playbackRate = speed.value
    element.volume = volume.value
    element.muted = muted.value
    state.value = 'ready'
    if (session.value.resumePlay) void play()
  }

  function seek(seconds: number) {
    if (!ready.value || !video.value || !Number.isFinite(seconds)) return
    const position = Math.min(duration.value, Math.max(0, seconds))
    video.value.currentTime = position
    time.value = position
    writeProgress(true)
  }

  function progress(event: Event) {
    if (!matches(event) || !ready.value) return
    time.value = event.target.currentTime
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
  }

  function pause(event: Event) {
    if (!matches(event)) return
    playing.value = false
    if (state.value === 'buffering') state.value = 'ready'
    writeProgress(true)
  }

  function buffer(event: Event) {
    if (matches(event) && !event.target.paused) state.value = 'buffering'
  }

  function error(event: Event) {
    if (!matches(event)) return
    resumeOnRetry = playing.value
    playing.value = false
    state.value = 'failed'
  }

  function ended(event: Event) {
    if (!matches(event)) return
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

  onBeforeUnmount(() => { writeProgress(true); video.value?.pause() })
  return { video, session, duration, time, playing, muted, volume, speed, state, ready,
    metadata, seek, progress, toggle, resumed, pause, buffer, error, ended, retry, setVolume, toggleMute, setSpeed }
}
