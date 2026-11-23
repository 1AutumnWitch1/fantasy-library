<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'
import { accountError, getSaved, needsLogin, removeSaved, saveWork, watchLabels, type SavedWork, type WatchStatus } from '../../api/account'
import type { WorkDetail } from '../../api/catalog'
import { useAccountStore } from '../../stores/account'

const props = defineProps<{ detail: WorkDetail }>()
const route = useRoute()
const account = useAccountStore()
const anime = computed(() => props.detail.work.type === 'ANIME')
const saved = shallowRef<SavedWork | null>(null)
const status = ref<WatchStatus>('PLAN_TO_WATCH')
const episodes = ref(0)
const favorite = ref(false)
const wishlist = ref(false)
const busy = ref(false)
const loading = ref(false)
const error = ref('')
const notice = ref('')
const retry = ref(0)
const loaded = ref(false)
const destination = computed(() => ({ name: 'account', query: { next: route.fullPath } }))

function reset(item: SavedWork | null) {
  saved.value = item
  status.value = item?.watchStatus ?? 'PLAN_TO_WATCH'
  episodes.value = item?.watchedEpisodes ?? 0
  favorite.value = item?.favorite === 1
  wishlist.value = item?.wishlist === 1
}

watch([() => props.detail.work.id, () => account.profile?.id, retry], async ([id, userId], _, onCleanup) => {
  const request = new AbortController()
  onCleanup(() => request.abort())
  reset(null)
  loaded.value = false
  error.value = ''
  notice.value = ''
  loading.value = Boolean(userId)
  if (!userId) return
  try {
    const result = await getSaved(id, request.signal)
    if (request.signal.aborted) return
    reset(result.item)
    loaded.value = true
  } catch (reason) {
    if (!request.signal.aborted) report(reason)
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
}, { immediate: true })

function report(reason: unknown) {
  if (needsLogin(reason)) account.signedOut()
  else error.value = accountError(reason)
}

async function persist(remove = false) {
  const id = props.detail.work.id
  const userId = account.profile?.id
  if (!userId || busy.value || !loaded.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    const deleting = remove || (!anime.value && !favorite.value && !wishlist.value)
    const form = {
      favorite: anime.value || favorite.value,
      wishlist: !anime.value && wishlist.value,
      watchStatus: anime.value ? status.value : null,
      watchedEpisodes: anime.value ? episodes.value : 0,
    }
    const updated = deleting ? await removeSaved(id).then(() => null) : await saveWork(id, form)
    if (id === props.detail.work.id && userId === account.profile?.id) {
      reset(updated)
      notice.value = updated ? '已保存到私人书架。' : '已从私人书架移除。'
    }
  } catch (reason) {
    report(reason)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="collection-panel" aria-labelledby="collection-title">
    <h2 id="collection-title">{{ anime ? '追番收藏' : '游戏收藏与愿望单' }}</h2>
    <p v-if="account.status === 'loading' || loading" role="status">正在翻阅私人记录…</p>
    <p v-else-if="account.status === 'error'" role="alert">{{ account.error }}
      <button type="button" @click="account.restore(true)">重试</button>
    </p>
    <p v-else-if="!account.profile"><RouterLink :to="destination">登录后{{ anime ? '记录追番' : '收藏游戏' }} ›</RouterLink></p>
    <template v-else>
      <p v-if="error" class="error" role="alert">{{ error }}
        <button v-if="!loaded" type="button" @click="retry++">重试</button>
      </p>
      <form v-if="loaded" @submit.prevent="persist()">
        <div v-if="anime" class="fields">
          <label>追番状态
            <select v-model="status" :disabled="busy">
              <option v-for="(label, value) in watchLabels" :key="value" :value="value">{{ label }}</option>
            </select>
          </label>
          <label>已看集数
            <input v-model.number="episodes" inputmode="numeric" type="number" required
              min="0" :max="detail.anime?.episodeCount ?? 99999" :disabled="busy">
          </label>
        </div>
        <div v-else class="fields choices">
          <label><input v-model="favorite" type="checkbox" :disabled="busy"> 游戏收藏</label>
          <label><input v-model="wishlist" type="checkbox" :disabled="busy"> 游戏愿望单</label>
        </div>
        <div class="actions">
          <button type="submit" :disabled="busy || (!anime && !saved && !favorite && !wishlist)">
            {{ saved ? '保存更改' : '加入私人书架' }}
          </button>
          <button v-if="saved" type="button" class="remove" :disabled="busy" @click="persist(true)">移出书架</button>
          <RouterLink :to="{ name: 'book', params: { bookId: 'personal' } }">查看书架 ›</RouterLink>
        </div>
        <p v-if="notice" role="status">{{ notice }}</p>
      </form>
    </template>
  </section>
</template>

<style scoped lang="scss">
.collection-panel { margin-top: 1.2rem; padding-top: .9rem; border-top: 1px solid #94744777; }
h2 { margin: 0 0 .55rem; font: 700 1.25rem/1.25 var(--display-font); }
p { margin: .55rem 0; }
.fields, .actions { display: flex; align-items: flex-end; flex-wrap: wrap; gap: .5rem .7rem; }
.actions { align-items: center; margin-top: .75rem; }
label { display: grid; gap: .2rem; font-size: .93rem; }
.choices label { display: flex; align-items: center; gap: .35rem; }
input, select, button {
  font: inherit; color: #442f1e; padding: .35rem .6rem; border: 1px solid #9b784b; border-radius: .15rem; background: #fff0bd90;
}
input[type=number] { max-width: 7rem; }
input[type=checkbox] { accent-color: #364b3c; width: 1rem; height: 1rem; }
button { min-height: 2.5rem; color: #f6e1b5; background: #314438; cursor: pointer; }
button:disabled { opacity: .58; cursor: default; }
.remove { background: #673f37; }
a { color: #2d5641; }
.error { color: #7d2b20; }
</style>
