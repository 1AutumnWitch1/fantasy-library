import { ref, shallowRef, watch } from 'vue'
import { catalogError, getWork, type WorkDetail } from '../api/catalog'
import { listEpisodes, type Episode } from '../api/playback'

export function useWorkScreen(workId: () => unknown, allNodes = false) {
  const detail = shallowRef<WorkDetail | null>(null)
  const episodes = shallowRef<Episode[]>([])
  const error = ref('')
  const playbackError = ref('')
  const checkingEpisodes = ref(false)
  const attempt = ref(0)

  watch([workId, attempt], async ([id], _, onCleanup) => {
    const request = new AbortController()
    onCleanup(() => request.abort())
    detail.value = null
    episodes.value = []
    error.value = ''
    playbackError.value = ''
    checkingEpisodes.value = true
    try {
      const work = await getWork(String(id), request.signal)
      if (request.signal.aborted) return
      detail.value = work
      if (!work.anime) return
      try {
        const result = await listEpisodes(work.work.id, request.signal, allNodes)
        if (!request.signal.aborted) episodes.value = result
      } catch (failure) {
        if (!request.signal.aborted) playbackError.value = catalogError(failure)
      }
    } catch (failure) {
      if (!request.signal.aborted) error.value = catalogError(failure)
    } finally {
      if (!request.signal.aborted) checkingEpisodes.value = false
    }
  }, { immediate: true })

  return { detail, episodes, checkingEpisodes, error, playbackError, retry: () => { attempt.value++ } }
}
