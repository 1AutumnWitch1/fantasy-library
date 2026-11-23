import { computed, onScopeDispose, ref, shallowRef } from 'vue'
import { catalogError, getTags, listWorks, type CatalogKind, type CatalogPageResult, type NamedItem } from '../api/catalog'
import { catalogSpread, WORKS_PER_SPREAD } from './catalogSpread'
import { prepareCovers } from './prepareCovers'

interface ReadingQuery {
  spreadIndex: number
  searchQuery: string
  selectedTags: readonly string[]
}

export function useCatalogBook(kind: CatalogKind, reading: ReadingQuery) {
  const page = shallowRef<CatalogPageResult | null>(null)
  const loading = ref(false)
  const error = ref('')
  const failedIndex = ref(0)
  const tagOptions = shallowRef<NamedItem[]>([])
  const filterError = ref('')
  const filtersLoading = ref(false)
  let request: AbortController | undefined
  let filterRequest: AbortController | undefined
  let timer: ReturnType<typeof setTimeout> | undefined

  function cancel() {
    clearTimeout(timer)
    request?.abort()
    request = undefined
    loading.value = false
  }

  async function loadFilters() {
    filterRequest?.abort()
    const ticket = new AbortController()
    filterRequest = ticket
    filterError.value = ''
    filtersLoading.value = true
    try {
      const result = await getTags(kind, ticket.signal)
      if (!ticket.signal.aborted) tagOptions.value = result
    } catch (reason) {
      if (!ticket.signal.aborted) filterError.value = catalogError(reason)
    } finally {
      if (!ticket.signal.aborted) filtersLoading.value = false
    }
  }

  // A fetched destination stays separate from the visible spread until the leaf lands.
  async function fetchPage(index: number, turning = false) {
    cancel()
    const ticket = new AbortController()
    request = ticket
    error.value = ''
    loading.value = true
    const query = {
      size: WORKS_PER_SPREAD,
      q: reading.searchQuery.trim() || undefined,
      tags: [...reading.selectedTags],
    }
    try {
      let number = index + 1
      while (!ticket.signal.aborted) {
        const result = await listWorks(kind, { ...query, page: number }, ticket.signal)
        if (ticket.signal.aborted) return null
        // Rows may have been removed since this book was last open.
        const last = Math.max(1, result.totalPages)
        if (number <= last) {
          if (turning) await prepareCovers(result.items, ticket.signal)
          return ticket.signal.aborted ? null : result
        }
        number = last
      }
    } catch (reason) {
      if (!ticket.signal.aborted) {
        error.value = catalogError(reason)
        failedIndex.value = index
      }
    } finally {
      if (request === ticket) loading.value = false
    }
    return null
  }

  function commit(result: CatalogPageResult) {
    page.value = result
    reading.spreadIndex = result.page - 1
  }
  function reload(index: number, debounce = false) {
    cancel()
    error.value = ''
    page.value = null
    loading.value = true
    async function run() {
      const result = await fetchPage(index)
      if (result) commit(result)
    }
    if (debounce) timer = setTimeout(run, 220)
    else void run()
  }
  const spread = computed(() => catalogSpread(kind, page.value,
    loading.value ? '正在查询馆藏…' : error.value ? '馆藏读取失败' : '没有匹配的作品'))
  onScopeDispose(() => { cancel(); filterRequest?.abort() })
  return { page, spread, loading, error, failedIndex, tagOptions, filterError, filtersLoading,
    loadFilters, fetchPage, reload, cancel, commit }
}
