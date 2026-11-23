<script setup lang="ts">
import { onScopeDispose, ref, watch } from 'vue'
import { accountError } from '../api/account'
import { setNodeListed, type ResourceChange } from '../api/admin'
import { confirmResourceRemoval, resourceChangeMessage } from './resourcePublication'
import { loadDownloadArchive, saveDownload, type DownloadDraft, type DownloadEntry } from '../api/downloadEditorial'
import type { Option } from '../api/editorial'
import GameDownloadForm from './GameDownloadForm.vue'

const props = defineProps<{ workId: number }>()
const blocking = defineModel<boolean>('busy', { default: false })
const nodes = ref<DownloadEntry[]>([])
const platforms = ref<Option[]>([])
const form = ref<{ id?: number; draft: DownloadDraft }>()
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const error = ref('')
const status = ref('')
const revision = ref(0)

watch([saving, uploading], ([save, upload]) => { blocking.value = save || upload }, { flush: 'sync' })
onScopeDispose(() => { blocking.value = false })

watch([() => props.workId, revision], async ([workId], _, onCleanup) => {
  const request = new AbortController()
  onCleanup(() => request.abort())
  loading.value = true
  error.value = ''
  try {
    const result = await loadDownloadArchive(workId, request.signal)
    if (request.signal.aborted) return
    nodes.value = result.nodes
    platforms.value = result.platforms
  } catch (failure) {
    if (!request.signal.aborted) error.value = accountError(failure)
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
}, { immediate: true })

function edit(node?: DownloadEntry) {
  error.value = ''
  status.value = ''
  if (node) {
    const { id, ...draft } = node
    form.value = { id, draft }
  } else if (platforms.value[0]) {
    form.value = { draft: {
      platformId: platforms.value[0].id, label: '', externalUrl: null, fileName: null, listed: false,
    } }
  }
}

async function change(operation: () => Promise<void | ResourceChange>, message = '') {
  if (blocking.value || loading.value) return
  saving.value = true
  error.value = ''
  status.value = ''
  try {
    const result = await operation()
    status.value = result ? resourceChangeMessage(result) : message
    revision.value++
  } catch (failure) {
    error.value = accountError(failure)
  } finally {
    saving.value = false
  }
}

function persist(nodeId: number | undefined, draft: DownloadDraft) {
  void change(async () => {
    await saveDownload(props.workId, nodeId, draft)
    form.value = undefined
  }, '下载节点已保存。')
}

function changeNode(node: DownloadEntry, listed: boolean) {
  if (blocking.value || (!listed && !confirmResourceRemoval(node.label))) return
  if (listed) {
    const { id, ...draft } = node
    void change(() => saveDownload(props.workId, id, { ...draft, listed: true }), '下载节点已上架。')
    return
  }
  void change(async () => {
    const result = await setNodeListed({ id: node.id, kind: 'DOWNLOAD' }, false, true)
    if (form.value?.id === node.id) form.value = undefined
    return result
  })
}
</script>

<template>
  <section class="admin-panel downloads-editor">
    <div class="downloads-actions header">
      <h2>游戏下载节点</h2>
      <button type="button" :disabled="blocking || loading || !platforms.length" @click="edit()">新增下载节点</button>
    </div>
    <p v-if="loading" role="status">正在读取下载节点…</p>
    <p v-if="status" role="status">{{ status }}</p>
    <p v-if="error" class="forum-error" role="alert">{{ error }} <button type="button" @click="revision++">重试读取</button></p>
    <p v-else-if="!loading && !platforms.length">请先在作品资料中保存至少一个游戏平台。</p>
    <GameDownloadForm v-if="form" :key="form.id ?? 'new'" :initial="form.draft" :platforms="platforms"
      :busy="saving" :creating="form.id === undefined" @uploading="uploading = $event" @save="persist(form.id, $event)"
      @cancel="form = undefined" />
    <ul v-if="nodes.length">
      <li v-for="node in nodes" :key="node.id">
        <div>
          <strong>{{ node.label }}</strong>
          <small>{{ platforms.find(platform => platform.id === node.platformId)?.name }} ·
            {{ node.externalUrl ? '外部下载' : node.fileName?.split('/').pop() }} · {{ node.listed ? '已上架' : '待上架' }}</small>
        </div>
        <div class="downloads-actions">
          <button type="button" :disabled="blocking || loading" @click="edit(node)">编辑节点</button>
          <button v-if="!node.listed" type="button" :disabled="blocking || loading" @click="changeNode(node, true)">上架</button>
          <button type="button" :disabled="blocking || loading" @click="changeNode(node, false)">
            {{ node.listed ? '下架并删除' : '删除' }}
          </button>
        </div>
      </li>
    </ul>
    <p v-else-if="!loading && !error && platforms.length && !form">暂无下载节点。</p>
  </section>
</template>

<style scoped lang="scss">
.downloads-editor { display:grid; gap:.65rem; }
.downloads-editor h2,.downloads-editor p { margin:0; }
.downloads-actions { display:flex; align-items:center; flex-wrap:wrap; gap:.5rem; }
.header { justify-content:space-between; }
ul { list-style:none; margin:0; padding:0; }
li { display:flex; flex-wrap:wrap; justify-content:space-between; align-items:center; gap:.5rem;
  padding:.65rem 0; border-top:1px solid var(--paper-line); overflow-wrap:anywhere; }
small { display:block; margin-top:.2rem; color:var(--paper-muted); }
</style>
