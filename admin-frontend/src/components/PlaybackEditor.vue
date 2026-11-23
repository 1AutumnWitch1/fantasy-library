<script setup lang="ts">
import { computed, onScopeDispose, ref, watch } from 'vue'
import { accountError } from '../api/account'
import { setNodeListed, type ResourceChange } from '../api/admin'
import { confirmResourceRemoval, resourceChangeMessage } from './resourcePublication'
import { listEditorialEpisodes, listPlaybackLines, saveEditorialEpisode, saveEditorialSource, savePlaybackLine,
  type Episode, type EpisodeDraft, type PlaybackLine, type PlaybackLineDraft, type Source, type SourceDraft } from '../api/editorialMedia'
import PlaybackEpisodeForm from './PlaybackEpisodeForm.vue'
import PlaybackSourceForm from './PlaybackSourceForm.vue'

const props = defineProps<{ workId: number }>()
const blocking = defineModel<boolean>('busy', { default: false })
const episodes = ref<Episode[]>([])
const lines = ref<PlaybackLine[]>([])
const lineId = ref<number>()
const activeLine = computed(() => lines.value.find(line => line.id === lineId.value))
const lineEpisodes = computed(() => episodes.value.filter(item => item.nodeId === lineId.value))
const lineEditor = ref<{ id?: number; draft: PlaybackLineDraft }>()
const activeId = ref<number>()
const active = computed(() => lineEpisodes.value.find(item => item.id === activeId.value))
const episodeEditor = ref<{ id?: number; draft: EpisodeDraft }>()
const sourceEditor = ref<{ id?: number; draft: SourceDraft }>()
const revision = ref(0)
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const error = ref('')
const notice = ref('')
let preferred: EpisodeDraft | undefined
let preferredLine: string | undefined

watch([saving, uploading], ([isSaving, isUploading]) => { blocking.value = isSaving || isUploading }, { flush: 'sync' })
onScopeDispose(() => { blocking.value = false })

watch([() => props.workId, revision], async ([workId], _, onCleanup) => {
  const request = new AbortController()
  onCleanup(() => request.abort())
  loading.value = true
  error.value = ''
  try {
    const [result, available] = await Promise.all([
      listEditorialEpisodes(workId, request.signal), listPlaybackLines(workId, request.signal),
    ])
    if (request.signal.aborted) return
    episodes.value = result
    lines.value = available
    if (preferredLine) {
      lineId.value = available.find(line => line.label === preferredLine)?.id
      preferredLine = undefined
    }
    if (!activeLine.value) lineId.value = available[0]?.id
    if (preferred) {
      const selection = preferred
      lineId.value = selection.nodeId
      activeId.value = result.find(item => item.nodeId === selection.nodeId &&
        item.edition === selection.edition && item.number === selection.number)?.id
      preferred = undefined
    }
    if (!active.value) activeId.value = lineEpisodes.value[0]?.id
  } catch (failure) {
    if (!request.signal.aborted) error.value = accountError(failure)
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
}, { immediate: true })

function selectLine() {
  lineEditor.value = undefined
  activeId.value = lineEpisodes.value[0]?.id
  selectEpisode()
}

function editLine(line?: PlaybackLine) {
  selectEpisode()
  lineEditor.value = line
    ? { id: line.id, draft: { label: line.label, listed: line.listed } }
    : { draft: { label: '', listed: true } }
}

function persistLine() {
  const editor = lineEditor.value
  if (!editor) return
  const draft = { ...editor.draft }
  void change(async () => {
    await savePlaybackLine(props.workId, editor.id, draft)
    preferredLine = draft.label.trim()
    lineEditor.value = undefined
  }, '播放线路已保存。')
}

function editEpisode(episode?: Episode) {
  if (!lineId.value) return
  lineEditor.value = undefined
  sourceEditor.value = undefined
  error.value = ''
  notice.value = ''
  episodeEditor.value = episode
    ? { id: episode.id, draft: { nodeId: episode.nodeId, edition: episode.edition, number: episode.number, title: episode.title, durationSeconds: episode.durationSeconds } }
    : { draft: { nodeId: lineId.value, edition: 'EPISODE', number: Math.min(999999, 1 + Math.max(0,
      ...lineEpisodes.value.filter(item => item.edition === 'EPISODE').map(item => item.number))),
      title: '', durationSeconds: 60 } }
}

function editSource(source?: Source) {
  lineEditor.value = undefined
  episodeEditor.value = undefined
  error.value = ''
  notice.value = ''
  sourceEditor.value = source
    ? { id: source.id, draft: { label: source.label, quality: source.quality, url: source.url, format: source.format } }
    : { draft: { label: '', quality: '', url: '', format: 'MP4' } }
}

function selectEpisode() {
  episodeEditor.value = undefined
  sourceEditor.value = undefined
  error.value = ''
  notice.value = ''
}

async function change(operation: () => Promise<void | ResourceChange>, success = '') {
  if (saving.value || uploading.value) return
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const result = await operation()
    notice.value = result ? resourceChangeMessage(result) : success
    revision.value++
  } catch (failure) {
    error.value = accountError(failure)
  } finally {
    saving.value = false
  }
}

function persistEpisode(draft: EpisodeDraft) {
  const editor = episodeEditor.value
  if (!editor) return
  void change(async () => {
    await saveEditorialEpisode(props.workId, editor.id, draft)
    preferred = draft
    episodeEditor.value = undefined
  }, '分集已保存。')
}

function persistSource(draft: SourceDraft) {
  const editor = sourceEditor.value
  const episode = active.value
  if (!episode || !editor) return
  void change(async () => {
    await saveEditorialSource(props.workId, episode.id, editor.id, draft)
    sourceEditor.value = undefined
  }, '本集来源已保存。新来源上架后可播放。')
}

function changeSource(source: Source, listed: boolean) {
  if (blocking.value || (!listed && !confirmResourceRemoval(source.label))) return
  void change(async () => {
    const result = await setNodeListed({ id: source.id, kind: 'PLAYBACK' }, listed, !listed)
    if (!listed && sourceEditor.value?.id === source.id) sourceEditor.value = undefined
    return result
  })
}
</script>

<template>
  <section class="admin-panel playback-editor">
    <div class="playback-actions playback-header">
      <h2>番剧播放来源</h2>
      <button type="button" :disabled="blocking || loading" @click="editLine()">新增播放来源</button>
    </div>
    <p v-if="loading" role="status">正在读取来源和分集…</p>
    <p v-if="notice" role="status">{{ notice }}</p>
    <p v-if="error" class="forum-error" role="alert">{{ error }} <button type="button" @click="revision++">重试读取</button></p>
    <form v-if="lineEditor" class="editor-form" @submit.prevent="persistLine">
      <fieldset :disabled="saving">
        <label>来源名称<input v-model.trim="lineEditor.draft.label" maxlength="100" required></label>
        <label class="line-public"><input v-model="lineEditor.draft.listed" type="checkbox">显示此来源</label>
        <div class="editor-form-actions">
          <button class="primary" type="submit">保存播放来源</button>
          <button type="button" @click="lineEditor = undefined">取消</button>
        </div>
      </fieldset>
    </form>
    <div v-if="activeLine" class="playback-actions episode-choice">
      <label>播放来源
        <select v-model.number="lineId" :disabled="blocking || loading" @change="selectLine">
          <option v-for="line in lines" :key="line.id" :value="line.id">{{ line.label }}{{ line.listed ? '' : '（已隐藏）' }}</option>
        </select>
      </label>
      <button type="button" :disabled="blocking || loading" @click="editLine(activeLine)">编辑播放来源</button>
      <button type="button" :disabled="blocking || loading" @click="editEpisode()">新增分集</button>
    </div>
    <PlaybackEpisodeForm v-if="episodeEditor" :key="episodeEditor.id ?? 'new'" :initial="episodeEditor.draft"
      :lines="lines" :busy="saving" @save="persistEpisode" @cancel="episodeEditor = undefined" />
    <template v-if="lineEpisodes.length">
      <div class="playback-actions episode-choice">
        <label>选择分集
          <select v-model="activeId" :disabled="blocking || loading" @change="selectEpisode">
            <option v-for="episode in lineEpisodes" :key="episode.id" :value="episode.id">
              {{ episode.edition === 'EPISODE' ? '正片' : '预告' }} {{ episode.number }} · {{ episode.title }}
            </option>
          </select>
        </label>
        <button type="button" :disabled="blocking || loading" @click="editEpisode(active)">编辑分集</button>
        <button type="button" :disabled="blocking || loading" @click="editSource()">添加视频</button>
      </div>
      <PlaybackSourceForm v-if="sourceEditor" :key="sourceEditor.id ?? 'new'" :initial="sourceEditor.draft"
        :busy="saving" @uploading="uploading = $event" @save="persistSource" @cancel="sourceEditor = undefined" />
      <ul v-if="active?.sources.length" class="playback-sources">
        <li v-for="source in active.sources" :key="source.id">
          <div>
            <strong>{{ source.label }}</strong>
            <span>{{ source.quality }} · {{ source.format }} · {{ source.listed ? '已上架' : '待上架' }}</span>
          </div>
          <div class="playback-actions">
            <button type="button" :disabled="blocking || loading" @click="editSource(source)">编辑视频</button>
            <button v-if="!source.listed" type="button" :disabled="blocking || loading" @click="changeSource(source, true)">上架</button>
            <button type="button" :disabled="blocking || loading" @click="changeSource(source, false)">
              {{ source.listed ? '下架并删除' : '删除' }}
            </button>
          </div>
        </li>
      </ul>
      <p v-else>本集暂无视频来源。</p>
    </template>
    <p v-else-if="!loading && !error && !episodeEditor && !lineEditor">
      {{ activeLine ? '当前线路还没有分集。' : '先新增线路，再录入分集和视频来源。' }}
    </p>
  </section>
</template>

<style scoped lang="scss">
.playback-editor { display:grid; gap:.65rem; }
.playback-editor h2, .playback-editor p { margin:0; }
.playback-actions { display:flex; align-items:end; flex-wrap:wrap; gap:.5rem; }
.playback-editor .line-public { display:flex; align-items:center; gap:.45rem; }
.playback-header { justify-content:space-between; align-items:center; }
.episode-choice label { flex:1 1 15rem; display:grid; gap:.3rem; }
.playback-sources { list-style:none; margin:0; padding:0; }
.playback-sources li { display:flex; flex-wrap:wrap; justify-content:space-between; align-items:center; gap:.5rem; padding:.65rem 0; border-top:1px solid var(--paper-line); }
.playback-sources span { display:block; margin-top:.2rem; color:var(--paper-muted); font-size:.86rem; }
:deep(.editor-form) { background:#f6e8cd; border:1px solid var(--paper-line); border-radius:.35rem; padding:.7rem; }
:deep(.editor-form fieldset) { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:.55rem; margin:0; padding:0; border:0; }
:deep(.editor-form label) { display:grid; gap:.3rem; min-width:0; }
:deep(.editor-form .wide), :deep(.editor-form-actions) { grid-column:1 / -1; }
:deep(.editor-form-actions) { display:flex; gap:.5rem; flex-wrap:wrap; }
@media(max-width:470px) { :deep(.editor-form fieldset) { grid-template-columns:1fr; } }
</style>
