<script setup lang="ts">
import { ref, watch } from 'vue'
import { accountError } from '../api/account'
import { getAdminNodes, setNodeListed, type SourceNode } from '../api/admin'
import { confirmResourceRemoval, resourceChangeMessage } from './resourcePublication'

const props = defineProps<{ workId: number }>()
const nodes = ref<SourceNode[]>([])
const loading = ref(true)
const error = ref('')
const busy = ref(false)
const retry = ref(0)
const notice = ref('')

watch([() => props.workId, retry], async ([id], _, onCleanup) => {
  const request = new AbortController()
  onCleanup(() => request.abort())
  loading.value = true
  error.value = ''
  try {
    const list = await getAdminNodes(id, request.signal)
    if (!request.signal.aborted) nodes.value = list
  } catch (failure) {
    if (!request.signal.aborted) error.value = accountError(failure)
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
}, { immediate: true })

async function change(node: SourceNode, listed: boolean) {
  if (busy.value || (!listed && !confirmResourceRemoval(node.label))) return
  busy.value = true
  error.value = ''
  try {
    const result = await setNodeListed(node, listed, !listed)
    notice.value = resourceChangeMessage(result)
    if (listed) node.listed = true
    else nodes.value = nodes.value.filter(item => item.kind !== node.kind || item.id !== node.id)
  } catch (failure) {
    error.value = accountError(failure)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="admin-nodes" aria-label="节点资源">
    <p v-if="loading" role="status">正在读取节点…</p>
    <p v-if="error" role="alert" class="forum-error">{{ error }} <button type="button" @click="retry++">重试</button></p>
    <p v-else-if="!loading && !nodes.length">暂未登记节点。</p>
    <p v-if="notice" role="status">{{ notice }}</p>
    <ul v-if="nodes.length">
      <li v-for="node in nodes" :key="node.kind + node.id">
        <div><strong>{{ node.label }}</strong><small>{{ node.context }}</small></div>
        <span role="status" :class="{ 'node-live': node.listed }">{{ node.listed ? '已上架' : '已下架' }}</span>
        <div class="node-actions">
          <button v-if="!node.listed" type="button" :disabled="busy || loading" @click="change(node, true)">上架</button>
          <button type="button" :disabled="busy || loading" @click="change(node, false)">
            {{ node.listed ? '下架并删除' : '删除' }}
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped lang="scss">
.admin-nodes { padding:.56rem .69rem; margin-top:.45rem; border:1px solid var(--paper-line); background:#f7e7b69a; }
.admin-nodes ul { margin:0; padding:0; list-style:none; }
.admin-nodes li { display:grid; grid-template-columns:minmax(0,1fr) auto auto; align-items:center; gap:.45rem;
  min-width:0; padding:.3rem 0; }
.admin-nodes li+li { border-top:1px solid var(--paper-line); }
.admin-nodes strong { overflow-wrap:anywhere; }
.admin-nodes small { display:block; }
.admin-nodes span,.admin-nodes small { color:var(--paper-muted); font-size:.8rem; }
.admin-nodes .node-live { color:#2b523a; }
.node-actions { display:flex; flex-wrap:wrap; gap:.32rem; }
p { margin:0; }
</style>
