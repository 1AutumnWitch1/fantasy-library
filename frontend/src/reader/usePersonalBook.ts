import { computed, onScopeDispose, ref, shallowRef, watch, type Ref } from 'vue'
import { accountError, listSaved, needsLogin, type SavedWork } from '../api/account'
import type { Page } from '../api/community'
import { getHistory, getInbox, markNoticeRead, type Inbox, type PersonalChapter, type WatchEvent } from '../api/personal'
import { useAccountStore } from '../stores/account'
import type { BookSpread, PersonalPage } from './bookTypes'

const itemsPerSide = 4
type Remote = { chapter: 'history'; data: Page<WatchEvent> } | { chapter: 'inbox'; data: Inbox }
interface Edition { chapter: PersonalChapter; owner: number; index: number; spread: BookSpread; remote?: Remote }

export function usePersonalBook(chapter: Ref<PersonalChapter>, reading: { spreadIndex: number }) {
  const account = useAccountStore()
  const saved = shallowRef<SavedWork[]>([])
  const loaded = ref(false)
  const remote = shallowRef<Remote | null>(null)
  const busy = ref(false)
  const failure = ref('')
  const unread = ref(0)
  const marking = ref(false)
  const owner = computed(() => account.profile?.id)
  const guest = computed(() => account.status === 'guest')
  const error = computed(() => account.status === 'error' ? account.error : failure.value)
  const working = computed(() => busy.value || account.status === 'loading' || account.status === 'idle')
  let pending: AbortController | undefined
  let badgeRequest: AbortController | undefined
  let disposed = false

  function report(reason: unknown) {
    if (needsLogin(reason)) account.signedOut()
    else failure.value = accountError(reason)
  }

  function lists() {
    if (chapter.value === 'games') {
      const games = saved.value.filter(item => item.workType === 'GAME')
      return [games.filter(item => item.wishlist), games.filter(item => item.favorite)] as const
    }
    const anime = saved.value.filter(item => item.workType === 'ANIME')
    return [anime.filter(item => item.watchStatus === 'WATCHING' || item.watchStatus === 'PLAN_TO_WATCH'),
      anime.filter(item => item.watchStatus === 'COMPLETED' || item.watchStatus === 'ON_HOLD')] as const
  }

  function status() {
    return working.value ? '正在读取私人书架…' : error.value
  }

  function localSpread(index: number): BookSpread {
    const [left, right] = lists()
    const titles = chapter.value === 'games' ? ['游戏愿望单', '已收藏游戏'] : ['在看与想看', '已看与搁置']
    const empty = chapter.value === 'games' ? ['愿望单里还没有游戏', '还没有收藏游戏'] : ['还没有追番计划', '这里还没有追番收藏']
    const page = (number: number, title: string, items: SavedWork[], message: string): PersonalPage => ({
      kind: 'personal', section: 'collection', number, title, guest: guest.value,
      items: items.slice(index * itemsPerSide, (index + 1) * itemsPerSide),
      message: (!loaded.value && status()) || message,
    })
    return { left: page(index * 2 + 1, titles[0], left, empty[0]), right: page(index * 2 + 2, titles[1], right, empty[1]) }
  }

  function remoteSpread(index: number, response = remote.value): BookSpread {
    const inbox = chapter.value === 'inbox'
    const data = response?.chapter === chapter.value ? response : null
    const message = status() || (inbox ? '还没有收到消息' : '保存追番进度后会在这里留下记录')
    const pages = inbox ? ['收到的消息', '消息档案'] : ['观看历史', '追番记录']
    const base = (side: number) => ({ kind: 'personal' as const, title: pages[side], number: index * 2 + side + 1,
      guest: guest.value, message })
    if (inbox) {
      const items = data?.chapter === 'inbox' ? data.data.page.items : []
      const middle = Math.ceil(items.length / 2)
      return {
        left: { ...base(0), section: 'inbox', notices: items.slice(0, middle), unread: data?.chapter === 'inbox' ? data.data.unread : unread.value },
        right: { ...base(1), section: 'inbox', notices: items.slice(middle), unread: 0, message: items.length ? '' : message },
      }
    }
    const items = data?.chapter === 'history' ? data.data.items : []
    const middle = Math.ceil(items.length / 2)
    return {
      left: { ...base(0), section: 'history', events: items.slice(0, middle) },
      right: { ...base(1), section: 'history', events: items.slice(middle), message: items.length ? '' : message },
    }
  }

  const isRemote = computed(() => chapter.value === 'history' || chapter.value === 'inbox')
  const spread = computed(() => isRemote.value ? remoteSpread(reading.spreadIndex) : localSpread(reading.spreadIndex))
  const spreadCount = computed(() => {
    if (isRemote.value) {
      const data = remote.value
      return Math.max(1, (data?.chapter === 'inbox' ? data.data.page.totalPages : data?.data.totalPages) ?? 0)
    }
    return Math.max(1, ...lists().map(items => Math.ceil(items.length / itemsPerSide)))
  })

  async function fetchEdition(index: number): Promise<Edition | undefined> {
    pending?.abort()
    const user = owner.value
    const current = chapter.value
    if (!user || disposed) return
    if (current === 'anime' || current === 'games') {
      return { chapter: current, owner: user, index, spread: localSpread(index) }
    }
    const request = new AbortController()
    pending = request
    failure.value = ''
    busy.value = true
    try {
      let page = index + 1
      while (!request.signal.aborted) {
        const size = itemsPerSide * 2
        const result: Remote = current === 'inbox'
          ? { chapter: current, data: await getInbox(page, size, request.signal) }
          : { chapter: current, data: await getHistory(page, size, request.signal) }
        if (request.signal.aborted || owner.value !== user || chapter.value !== current) return
        const total = result.chapter === 'inbox' ? result.data.page.totalPages : result.data.totalPages
        if (page <= Math.max(1, total)) {
          return { chapter: current, owner: user, index: page - 1, remote: result, spread: remoteSpread(page - 1, result) }
        }
        page = Math.max(1, total)
      }
    } catch (reason) {
      if (!request.signal.aborted && owner.value === user) report(reason)
    } finally {
      if (pending === request) busy.value = false
    }
  }

  function commit(edition: Edition) {
    if (edition.chapter !== chapter.value || edition.owner !== owner.value || disposed) return
    if (edition.remote) {
      remote.value = edition.remote
      if (edition.remote.chapter === 'inbox') unread.value = edition.remote.data.unread
    }
    reading.spreadIndex = edition.index
  }

  async function refresh() {
    if (account.status === 'error') return account.restore(true)
    if (!owner.value || disposed) return
    if (isRemote.value) {
      const edition = await fetchEdition(reading.spreadIndex)
      if (edition) commit(edition)
      return
    }
    pending?.abort()
    const request = new AbortController()
    const user = owner.value
    pending = request
    busy.value = true
    failure.value = ''
    try {
      const result = await listSaved(request.signal)
      if (request.signal.aborted || user !== owner.value) return
      saved.value = result
      loaded.value = true
      reading.spreadIndex = Math.min(reading.spreadIndex, spreadCount.value - 1)
    } catch (reason) {
      if (!request.signal.aborted && user === owner.value) report(reason)
    } finally {
      if (pending === request) busy.value = false
    }
  }

  async function readNotice(id?: number) {
    if (marking.value || working.value || !owner.value || chapter.value !== 'inbox') return
    marking.value = true
    failure.value = ''
    const user = owner.value
    try {
      await markNoticeRead(id)
      if (owner.value === user && chapter.value === 'inbox') await refresh()
    } catch (reason) {
      if (!disposed && owner.value === user) report(reason)
    } finally {
      marking.value = false
    }
  }

  watch(owner, async (user, _, onCleanup) => {
    saved.value = []
    loaded.value = false
    unread.value = 0
    if (!user || chapter.value === 'inbox') return
    const request = new AbortController()
    badgeRequest = request
    onCleanup(() => request.abort())
    try {
      const summary = await getInbox(1, 1, request.signal)
      if (!request.signal.aborted && owner.value === user) unread.value = summary.unread
    } catch (reason) {
      if (!request.signal.aborted && needsLogin(reason)) account.signedOut()
    }
  }, { immediate: true })

  watch([owner, chapter], () => {
    pending?.abort()
    if (chapter.value === 'inbox') badgeRequest?.abort()
    pending = undefined
    busy.value = false
    failure.value = ''
    remote.value = null
    reading.spreadIndex = 0
    void refresh()
  }, { immediate: true })

  function cancel() {
    disposed = true
    pending?.abort()
    badgeRequest?.abort()
  }

  void account.restore()
  onScopeDispose(cancel)
  return { spread, spreadCount, working, error, unread, marking, refresh, fetchEdition, commit, readNotice, account, cancel }
}
