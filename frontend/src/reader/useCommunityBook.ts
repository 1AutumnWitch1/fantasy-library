import { computed, shallowRef, ref, watch, onScopeDispose } from 'vue'
import { useRoute } from 'vue-router'
import { accountError } from '../api/account'
import { isTopic, listPopularPosts, listPosts, readId, type Page, type Post, type Topic } from '../api/community'
import type { BookSpread } from './bookTypes'

interface Edition {
  page: Page<Post>
  topic?: Topic
  workId?: number
}

export function useCommunityBook(reading: { spreadIndex: number }) {
  const route = useRoute()
  const topic = computed(() => isTopic(route.query.topic) ? route.query.topic : undefined)
  const workId = computed(() => readId(route.query.workId))
  const result = shallowRef<Edition | null>(null)
  const busy = ref(false)
  const error = ref('')
  const failed = ref(0)
  const popular = shallowRef<Post[]>([])
  const popularMessage = ref('')
  let request: AbortController | undefined
  let popularRequest: AbortController | undefined

  function cancelArchive() {
    request?.abort()
    request = undefined
    busy.value = false
  }

  async function fetchPage(index: number) {
    cancelArchive()
    const pending = new AbortController()
    request = pending
    const context = { topic: topic.value, workId: workId.value }
    error.value = ''
    busy.value = true
    try {
      let number = index + 1
      while (!pending.signal.aborted) {
        const page = await listPosts(number, context.topic, context.workId, pending.signal)
        if (pending.signal.aborted) return
        const last = Math.max(1, page.totalPages)
        if (number <= last) return { page, ...context }
        number = last
      }
    } catch (reason) {
      if (!pending.signal.aborted) {
        error.value = accountError(reason)
        failed.value = index
      }
    } finally {
      if (request === pending) busy.value = false
    }
  }

  function commit(edition: Edition) {
    if (edition.topic !== topic.value || edition.workId !== workId.value) return
    result.value = edition
    reading.spreadIndex = edition.page.page - 1
  }

  function cancel() {
    cancelArchive()
    popularRequest?.abort()
  }

  async function loadPopular() {
    popularRequest?.abort()
    const pending = new AbortController()
    popularRequest = pending
    popularMessage.value = '正在调阅热门手稿…'
    try {
      const page = await listPopularPosts(topic.value, workId.value, pending.signal)
      if (!pending.signal.aborted) {
        popular.value = page.items
        popularMessage.value = '这个话题还没有手稿。'
      }
    } catch {
      if (!pending.signal.aborted) popularMessage.value = '暂时无法调阅热门手稿。'
    }
  }

  async function reload() {
    void loadPopular()
    const response = await fetchPage(reading.spreadIndex)
    if (response) commit(response)
  }

  watch([topic, workId], () => {
    reading.spreadIndex = 0
    result.value = null
    popular.value = []
    void reload()
  }, { immediate: true })
  onScopeDispose(cancel)

  function spread(edition = result.value): BookSpread {
    const number = ((edition?.page.page ?? 1) - 1) * 2 + 1
    const filters = { topic: edition?.topic ?? topic.value, workId: edition?.workId ?? workId.value }
    return {
      left: { kind: 'community', number, title: '馆务公告', items: popular.value, ...filters, message: popularMessage.value },
      right: {
        kind: 'community', number: number + 1, title: '讨论档案', items: edition?.page.items ?? [], ...filters,
        message: busy.value ? '正在调阅讨论…' : error.value || '这一页还没有手稿',
      },
    }
  }

  return { result, busy, error, failed, fetchPage, commit, reload, cancel, spread }
}
