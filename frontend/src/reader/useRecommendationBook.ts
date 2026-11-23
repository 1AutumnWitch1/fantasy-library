import { computed, ref, shallowRef, watch, type Ref } from 'vue'
import { needsLogin } from '../api/account'
import { catalogError } from '../api/catalog'
import { readRecommendations, type RecommendationFeed, type RecommendationType } from '../api/recommendations'
import { useAccountStore } from '../stores/account'
import type { BookSpread } from './bookTypes'

export function useRecommendationBook(type: Ref<RecommendationType | null>) {
  const account = useAccountStore()
  const feed = shallowRef<RecommendationFeed | null>(null)
  const busy = ref(true)
  const error = ref('')
  const revision = ref(0)
  const checkingAccount = computed(() => account.status === 'idle' || account.status === 'loading')
  const guest = computed(() => !account.profile)
  void account.restore()

  const cancel = watch([type, () => account.profile?.id, checkingAccount, revision], async ([kind, member, checking], _, onCleanup) => {
    const request = new AbortController()
    onCleanup(() => request.abort())
    feed.value = null
    error.value = ''
    busy.value = true
    if (checking) return

    try {
      const response = await readRecommendations(kind, !!member, request.signal)
      if (!request.signal.aborted) feed.value = response
    } catch (reason) {
      if (request.signal.aborted) return
      if (member && needsLogin(reason)) account.signedOut()
      else error.value = catalogError(reason)
    } finally {
      if (!request.signal.aborted) busy.value = false
    }
  }, { immediate: true })

  const spreads = computed<readonly BookSpread[]>(() => {
    const status = busy.value ? 'loading' : error.value ? 'error' : 'ready'
    const data = feed.value
    return [
      {
        left: { kind: 'recommendations', number: 1, title: '馆员荐读', section: 'picks', status,
          mode: data?.mode ?? 'EMPTY', items: data?.forYou ?? [], guest: guest.value },
        right: { kind: 'recommendations', number: 2, title: '热门榜', section: 'popular', status, items: data?.popular ?? [] },
      }, {
        left: { kind: 'recommendations', number: 3, title: '新入馆', section: 'newest', status, items: data?.newest ?? [] },
        right: { kind: 'recommendations', number: 4, title: '近期活跃讨论', section: 'discussions', status,
          items: data?.discussions ?? [] },
      },
    ]
  })

  return { type, busy, error, spreads, cancel, retry: () => { revision.value++ } }
}
