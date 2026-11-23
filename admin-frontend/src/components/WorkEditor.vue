<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { accountError } from '../api/account'
import { setItemListed, type WorkType } from '../api/admin'
import { getArchive, getOptions, saveArchive, type Draft, type Options } from '../api/editorial'
import { publicUrl } from '../router'
import WorkPictures from './WorkPictures.vue'
import PlaybackEditor from './PlaybackEditor.vue'
import GameDownloadEditor from './GameDownloadEditor.vue'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.name === 'work-edit' ? Number(route.params.id) : undefined)
const draft = ref<Draft>()
const options = ref<Options>()
const listed = ref(false)
const loading = ref(true)
const busy = ref(false)
const picturesUploading = ref(false)
const resourceBusy = ref(false)
const resourcePanel = ref<HTMLElement | null>(null)
const editorBusy = computed(() => busy.value || picturesUploading.value || resourceBusy.value)
const error = ref('')
const failure = ref('')
const notice = ref('')
const version = ref(0)
const saved = ref('')
const savedPlatforms = ref('')
const dirty = computed(() => !!draft.value && JSON.stringify(draft.value) !== saved.value)
const tagSearch = ref('')
const tags = computed(() => {
  const term = tagSearch.value.trim().toLowerCase()
  return options.value?.tags.filter(tag => tag.name.toLowerCase().includes(term)) ?? []
})

watch([id, () => route.hash, loading], async ([workId, hash, isLoading]) => {
  if (workId && !isLoading && hash === '#work-resources') {
    await nextTick()
    resourcePanel.value?.focus({ preventScroll: true })
    resourcePanel.value?.scrollIntoView({ block: 'start' })
  }
}, { flush: 'post' })

function empty(type: WorkType): Draft {
  return { type, slug: '', title: '', summary: '', releaseDate: null, categoryId: 0, tagIds: [],
    anime: type === 'ANIME' ? { studio: '', airingStatus: 'UPCOMING', episodeCount: null } : null,
    game: type === 'GAME' ? { developer: '', ageRating: null, platformIds: [] } : null,
    cover: null, gallery: [] }
}

watch([id, () => route.query.type, version], async ([current, type], _, onCleanup) => {
  const request = new AbortController()
  onCleanup(() => request.abort())
  draft.value = undefined
  loading.value = true
  error.value = ''
  failure.value = ''
  notice.value = route.query.created === '1' ? '已保存为待上架作品。' : ''
  try {
    if (current !== undefined && (!Number.isSafeInteger(current) || current < 1)) throw new Error('作品编号无效')
    const [choices, archive] = await Promise.all([
      getOptions(request.signal), current ? getArchive(current, request.signal) : Promise.resolve(null),
    ])
    if (request.signal.aborted) return
    const form = empty(archive?.entry.type ?? (type === 'GAME' ? 'GAME' : 'ANIME'))
    if (archive) {
      const { type, slug, title, summary, releaseDate, categoryId } = archive.entry
      Object.assign(form, {
        type, slug, title, summary, releaseDate, categoryId, tagIds: archive.tagIds,
        anime: archive.anime ?? form.anime, game: archive.game ?? form.game, cover: archive.cover, gallery: archive.gallery,
      })
    }
    listed.value = archive?.entry.listed ?? false
    options.value = choices
    saved.value = JSON.stringify(form)
    savedPlatforms.value = form.game?.platformIds.join(',') ?? ''
    draft.value = form
  } catch (reason) {
    if (!request.signal.aborted) failure.value = reason instanceof Error && !('response' in reason)
      ? (reason.message === '作品编号无效' ? reason.message : accountError(reason)) : accountError(reason)
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
}, { immediate: true })

function canLeaveEditor() {
  return !picturesUploading.value && !resourceBusy.value
    && (!dirty.value || (!busy.value && window.confirm('你有未保存的修改，确定离开编辑页？')))
}
onBeforeRouteLeave(canLeaveEditor)
onBeforeRouteUpdate((to, from) => to.params.id === from.params.id || canLeaveEditor())

async function submit() {
  const form = draft.value
  if (!form || editorBusy.value) return
  error.value = ''
  notice.value = ''
  if (!form.categoryId || (form.game && !form.game.platformIds.length)) {
    error.value = !form.categoryId ? '请选择作品分类。' : '请至少选择一个游戏平台。'
    return
  }
  busy.value = true
  try {
    const payload: Draft = {
      ...form,
      releaseDate: form.releaseDate || null,
      anime: form.anime && { ...form.anime, episodeCount: form.anime.episodeCount || null },
      game: form.game && { ...form.game, ageRating: form.game.ageRating || null },
    }
    const result = await saveArchive(id.value, payload)
    saved.value = JSON.stringify(form)
    savedPlatforms.value = form.game?.platformIds.join(',') ?? ''
    if (!id.value) await router.replace({ name: 'work-edit', params: { id: result.id }, query: { created: '1' } })
    else notice.value = '作品资料已保存。'
  } catch (reason) {
    error.value = accountError(reason)
  } finally {
    busy.value = false
  }
}

async function publish() {
  if (!id.value || dirty.value || editorBusy.value) return
  const target = !listed.value
  if (!target && !window.confirm('下架后，公开目录及站内资源将暂停访问。确定下架？')) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await setItemListed('works', id.value, target)
    listed.value = target
    notice.value = target ? '作品已上架。' : '作品已下架。'
  } catch (reason) {
    error.value = accountError(reason)
  } finally { busy.value = false }
}
</script>

<template>
  <div class="editor-head">
    <div>
      <RouterLink class="editor-back" :to="{ name: 'manager', query: { type: draft?.type || route.query.type } }">‹ 返回馆藏管理</RouterLink>
      <h1>{{ id ? '编辑作品资料' : '新增作品' }}</h1>
    </div>
    <div v-if="draft && id" class="editor-actions">
      <a v-if="listed" :href="publicUrl('/works/' + id)">查看公开详情 ↗</a>
      <span class="publication">{{ listed ? '已上架' : '未上架' }}</span>
      <button type="button" :disabled="editorBusy || dirty" @click="publish">{{ listed ? '下架作品' : '上架作品' }}</button>
    </div>
  </div>
  <p v-if="loading" role="status">正在读取作品资料…</p>
  <p v-else-if="failure" role="alert" class="forum-error">
    {{ failure }} <button type="button" @click="version++">重试</button>
  </p>
  <div v-else-if="draft && options">
    <form class="editor" @submit.prevent="submit">
      <fieldset :disabled="editorBusy">
        <div class="editor-layout">
          <div class="editor-column">
            <section class="admin-panel">
              <h2>基本资料</h2>
              <div class="fields">
                <label>作品名称<input v-model.trim="draft.title" required maxlength="180"></label>
                <label>作品标识<input v-model.trim="draft.slug" required maxlength="90" pattern="[a-z0-9]+(-[a-z0-9]+)*" placeholder="例如 mist-harbor"></label>
                <label>作品分类
                  <select v-model.number="draft.categoryId" required>
                    <option :value="0" disabled>选择类别</option>
                    <option v-for="category in options.categories" :key="category.id" :value="category.id">{{ category.name }}</option>
                  </select>
                </label>
                <label>{{ draft.type === 'ANIME' ? '首播日期' : '发行日期' }}<input v-model="draft.releaseDate" type="date"></label>
                <label class="wide">作品简介<textarea v-model.trim="draft.summary" rows="5" required maxlength="900"></textarea></label>
              </div>
            </section>

            <section class="admin-panel">
              <h2>{{ draft.type === 'ANIME' ? '番剧资料' : '游戏资料' }}</h2>
              <div v-if="draft.anime" class="fields">
                <label>制作公司<input v-model.trim="draft.anime.studio" required maxlength="150"></label>
                <label>播出状态
                  <select v-model="draft.anime.airingStatus">
                    <option value="UPCOMING">待播出</option><option value="ONGOING">连载中</option><option value="COMPLETED">已完结</option>
                  </select>
                </label>
                <label>总集数<input v-model.number="draft.anime.episodeCount" type="number" min="1" max="99999" placeholder="未公布时留空"></label>
              </div>
              <div v-if="draft.game" class="fields">
                <label>开发商<input v-model.trim="draft.game.developer" required maxlength="150"></label>
                <label>年龄分级<input v-model.trim="draft.game.ageRating" maxlength="20"></label>
                <div class="wide">
                  <h3>游戏平台</h3>
                  <div class="option-list">
                    <label v-for="platform in options.platforms" :key="platform.id">
                      <input v-model="draft.game.platformIds" type="checkbox" :value="platform.id">{{ platform.name }}
                    </label>
                  </div>
                </div>
              </div>
            </section>

            <section class="admin-panel">
              <h2>标签 <small>{{ draft.tagIds.length }} 个已选</small></h2>
              <input v-model="tagSearch" type="search" aria-label="筛选标签" placeholder="筛选标签">
              <div class="option-list tags">
                <label v-for="tag in tags" :key="tag.id"><input v-model="draft.tagIds" type="checkbox" :value="tag.id">{{ tag.name }}</label>
                <p v-if="!tags.length">无匹配标签。</p>
              </div>
            </section>
          </div>
          <div class="editor-column">
            <WorkPictures v-model:cover="draft.cover" v-model:gallery="draft.gallery" v-model:uploading="picturesUploading" />
            <section v-if="id && draft.game" class="admin-panel">
              <div class="editor-actions nodes-heading">
                <h2>下载节点</h2>
                <a href="#work-resources">管理节点 ↓</a>
              </div>
            </section>
          </div>
        </div>
        <footer class="editor-actions editor-footer">
          <button class="primary" type="submit" :disabled="editorBusy || (!!id && !dirty)">{{ busy ? '正在保存…' : id ? '保存资料' : '保存为待上架作品' }}</button>
          <span v-if="dirty && id">有未保存的修改</span>
          <p v-if="error" role="alert" class="forum-error">{{ error }}</p>
          <p v-else-if="notice" role="status">{{ notice }}</p>
        </footer>
      </fieldset>
    </form>
    <div v-if="id" id="work-resources" ref="resourcePanel" tabindex="-1">
      <PlaybackEditor v-if="draft.anime" :key="id" :work-id="id" v-model:busy="resourceBusy" />
      <GameDownloadEditor v-else-if="draft.game" :key="id + ':' + savedPlatforms" :work-id="id" v-model:busy="resourceBusy" />
    </div>
    <p v-else-if="draft.anime">保存作品后可添加播放视频。</p>
  </div>
</template>

<style scoped lang="scss">
.editor-head,.editor-actions { display:flex; align-items:center; gap:.55rem .8rem; flex-wrap:wrap; }
.editor-head { justify-content:space-between; margin-bottom:.9rem; }
.editor-back { display:inline-block; font-size:.89rem; margin-bottom:.42rem; }
.editor-head h1 { margin:0; }
.publication { color:#355044; background:#e0eae0; border-radius:.2rem; padding:.32rem .6rem; }
.editor > fieldset { padding:0; border:0; margin:0; min-width:0; }
.editor-layout { display:grid; align-items:start; grid-template-columns:minmax(0,1.38fr) minmax(0,1fr); gap:.9rem; }
.editor-column { display:grid; min-width:0; gap:.9rem; }
.fields { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:.75rem; }
.fields > label { display:grid; align-content:start; gap:.35rem; }
.fields .wide { grid-column:1 / -1; }
.fields h3 { margin:0 0 .38rem; font-size:1rem; }
h2 small { color:var(--muted); font-size:.76rem; font-weight:400; }
.option-list { display:flex; flex-wrap:wrap; align-content:start; gap:.36rem .5rem; }
.option-list label { display:flex; align-items:center; gap:.18rem; padding:.2rem .34rem; border:1px solid var(--paper-line); border-radius:.24rem; cursor:pointer; }
.tags { margin-top:.6rem; max-height:12rem; overflow:auto; }
.nodes-heading { justify-content:space-between; }
.nodes-heading h2 { margin:0; }
.editor-footer { margin-top:1rem; padding:.65rem 0; }
.editor-footer p { margin:.15rem 0; }
@media(max-width:860px) { .editor-layout { grid-template-columns:1fr; } }
@media(max-width:470px) { .fields { grid-template-columns:1fr; } }
</style>
