import { shallowRef, watch } from 'vue'
import { listWorks, type WorkDetail, type WorkItem } from '../api/catalog'
import { playbackStats } from '../api/viewing'

export interface RelatedScreening {
  item: WorkItem
  plays?: number
}

export function useRelatedScreenings(detail: () => WorkDetail | null | undefined) {
  const related = shallowRef<RelatedScreening[]>([])
  watch(detail, async (work, _, onCleanup) => {
    const request = new AbortController()
    onCleanup(() => request.abort())
    related.value = []
    if (!work?.anime) return
    const tag = work.tags[0]?.slug
    const list = (tags?: string[]) => listWorks('anime', { page: 1, size: 7, tags }, request.signal)
    const others = (items: WorkItem[]) => items.filter(item => item.work.id !== work.work.id)
    try {
      const result = await list(tag ? [tag] : undefined)
      let matches = others(result.items)
      if (matches.length < 4 && tag && !request.signal.aborted) {
        const fallback = others((await list()).items)
        matches = [...matches, ...fallback.filter(item => !matches.some(match => match.work.id === item.work.id))]
      }
      if (request.signal.aborted) return
      const items = matches.slice(0, 5)
      related.value = items.map(item => ({ item }))
      const stats = await Promise.allSettled(items.map(item => playbackStats(item.work.id, request.signal)))
      if (!request.signal.aborted) related.value = items.map((item, index) => {
        const result = stats[index]
        return { item, plays: result?.status === 'fulfilled' ? result.value.plays : undefined }
      })
    } catch {
      // Recommendations load independently of the video and episode list.
    }
  }, { immediate: true })
  return related
}
