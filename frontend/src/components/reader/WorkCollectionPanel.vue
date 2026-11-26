<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { accountError, getSaved, needsLogin, selectWork, type CollectionSelection, type SavedWork } from '../../api/account'
import type { WorkDetail } from '../../api/catalog'
import { useAccountStore } from '../../stores/account'

const props = defineProps<{ detail: WorkDetail }>()
const emit = defineEmits<{ changed: [] }>()
const route = useRoute()
const router = useRouter()
const account = useAccountStore()
const anime = computed(() => props.detail.work.type === 'ANIME')
const saved = shallowRef<SavedWork | null>(null)
const busy = ref(false)
const error = ref('')
const retry = ref(0)
const loaded = ref(false)
const login = () => router.push({ name: 'account', query: { next: route.fullPath } })

watch([() => props.detail.work.id, () => account.profile?.id, retry], async ([id, userId], _, onCleanup) => {
  const request = new AbortController()
  onCleanup(() => request.abort())
  saved.value = null
  loaded.value = false
  error.value = ''
  if (!userId) return
  try {
    const result = await getSaved(id, request.signal)
    if (!request.signal.aborted) {
      saved.value = result.item
      loaded.value = true
    }
  } catch (reason) {
    if (request.signal.aborted) return
    if (needsLogin(reason)) account.signedOut()
    else error.value = accountError(reason)
  }
}, { immediate: true })

async function change(kind: CollectionSelection, selected: boolean) {
  if (!account.profile) { await login(); return }
  if (!loaded.value || busy.value) return
  const id = props.detail.work.id
  const user = account.profile.id
  busy.value = true
  error.value = ''
  try {
    const result = await selectWork(id, kind, selected)
    if (id === props.detail.work.id && user === account.profile?.id) {
      saved.value = result.item
      emit('changed')
    }
  } catch (reason) {
    if (user !== account.profile?.id || id !== props.detail.work.id) return
    if (needsLogin(reason)) { account.signedOut(); await login() }
    else error.value = accountError(reason)
  } finally { busy.value = false }
}
</script>

<template>
  <div class="collection-panel" :aria-busy="busy || account.status === 'loading'">
    <div class="collection-buttons" :class="{ 'collection-buttons--game': !anime }">
      <button type="button" :disabled="busy || account.status === 'loading' || !!account.profile && !loaded"
        :aria-pressed="saved?.favorite === 1" @click="change(anime ? 'FOLLOW' : 'FAVORITE', saved?.favorite !== 1)">
        {{ anime ? (saved?.favorite === 1 ? '✓ 已追番' : '＋ 追番') : (saved?.favorite === 1 ? '★ 已收藏' : '☆ 收藏') }}
      </button>
      <button v-if="!anime" type="button" :disabled="busy || account.status === 'loading' || !!account.profile && !loaded"
        :aria-pressed="saved?.wishlist === 1" @click="change('WISHLIST', saved?.wishlist !== 1)">
        {{ saved?.wishlist === 1 ? '✓ 已加入愿望单' : '＋ 加入愿望单' }}
      </button>
    </div>
    <p v-if="error" role="alert">{{ error }} <button v-if="!loaded" type="button" class="retry" @click="retry++">重试</button></p>
  </div>
</template>

<style scoped>
.collection-buttons { display: flex; gap: .6rem; }
.collection-buttons--game { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); width: min(100%, 26rem); }
button {
  font: 700 .95rem/1.35 var(--body-font, inherit); min-height: 2.75rem; padding: .45rem .8rem;
  color: #302318; background: #ecd095; border: 1px solid #b68f55; border-radius: .28rem; cursor: pointer;
  transition: transform .12s, background-color .15s; overflow-wrap: anywhere;
}
button[aria-pressed="true"] { color: #f3dfb9; background: #354b3c; }
button:disabled { opacity: .62; cursor: default; }
button:active:not(:disabled) { transform: scale(.97); }
button.retry { min-height: 2rem; }
p { margin: .45rem 0 0; font-size: .85rem; }
@media (prefers-reduced-motion: reduce) { button { transition: none; } }
</style>
