import { ref, shallowRef, watch } from 'vue'
import { afterViewingWrites, playbackStats, viewingProgress, type PlaybackStats, type ViewingProgress } from '../api/viewing'
import { useAccountStore } from '../stores/account'

export function useViewingProgress(id: () => number | undefined) {
  const account = useAccountStore()
  const stats = shallowRef<PlaybackStats | null>(null)
  const progress = shallowRef<ViewingProgress | null>(null)
  const checking = ref(false)
  const error = ref(false)
  const revision = ref(0)
  const statsRevision = ref(0)
  watch([id, statsRevision], async ([work], _, onCleanup) => {
    const request = new AbortController()
    onCleanup(() => request.abort())
    if (!work) { stats.value = null; return }
    try {
      const result = await playbackStats(work, request.signal)
      if (!request.signal.aborted) stats.value = result
    } catch { if (!request.signal.aborted) stats.value = null }
  }, { immediate: true })
  watch([id, () => account.profile?.id, revision], async ([work, member], [previousWork, previousMember], onCleanup) => {
    const request = new AbortController()
    onCleanup(() => request.abort())
    error.value = false
    if (work !== previousWork || member !== previousMember) progress.value = null
    checking.value = Boolean(work && member && !progress.value)
    if (!work || !member) return
    try {
      await afterViewingWrites()
      if (request.signal.aborted) return
      const result = await viewingProgress(work, request.signal)
      if (!request.signal.aborted) progress.value = result
    } catch { if (!request.signal.aborted) error.value = true }
    finally { if (!request.signal.aborted) checking.value = false }
  }, { immediate: true })
  const refreshProgress = () => { revision.value++ }
  return { stats, progress, checking, error, refreshStats: () => { statsRevision.value++ }, refreshProgress, retry: refreshProgress }
}
