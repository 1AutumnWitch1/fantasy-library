<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { catalogError } from '../../api/catalog'
import { getGameDownloads, type GameDownload } from '../../api/downloads'

const props = defineProps<{ workId: number }>()
const nodes = shallowRef<GameDownload[]>([])
const selection = ref(0)
const loading = ref(true)
const error = ref('')
const attempt = ref(0)

const selected = computed(() => nodes.value.find(node => node.id === selection.value))
const groups = computed(() => {
  const platforms = new Map<string, GameDownload[]>()
  for (const node of nodes.value) {
    const siblings = platforms.get(node.platform)
    if (siblings) siblings.push(node)
    else platforms.set(node.platform, [node])
  }
  return platforms
})

watch([() => props.workId, attempt], async ([id], _, onCleanup) => {
  const request = new AbortController()
  onCleanup(() => request.abort())
  nodes.value = []
  selection.value = 0
  error.value = ''
  loading.value = true
  try {
    const result = await getGameDownloads(id, request.signal)
    if (!request.signal.aborted) {
      nodes.value = result
      selection.value = result[0]?.id ?? 0
    }
  } catch (failure) {
    if (!request.signal.aborted) error.value = catalogError(failure)
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
}, { immediate: true })
</script>

<template>
  <section class="game-download" aria-label="游戏下载">
    <div class="download-controls">
      <a v-if="selected" class="download-primary" :href="selected.url"
        :download="selected.external ? undefined : ''"
        :target="selected.external ? '_blank' : undefined"
        :rel="selected.external ? 'noopener noreferrer' : undefined">
        <span aria-hidden="true">↓</span> 下载游戏
      </a>
      <button v-else type="button" class="download-primary" disabled>
        <span aria-hidden="true">↓</span> 下载游戏
      </button>
      <select v-model.number="selection" aria-label="平台与下载节点" :disabled="!nodes.length">
        <option v-if="!nodes.length" :value="0">{{ loading ? '正在读取节点…' : error ? '节点读取失败' : '暂无下载节点' }}</option>
        <optgroup v-for="[platform, options] in groups" :key="platform" :label="platform">
          <option v-for="node in options" :key="node.id" :value="node.id">{{ platform }} · {{ node.label }}</option>
        </optgroup>
      </select>
    </div>
    <p v-if="error" class="download-note" role="alert">
      {{ error }} <button type="button" class="download-retry" @click="attempt++">重试</button>
    </p>
    <p v-else-if="!loading && !selected" class="download-note" role="status">暂未收录可用的下载资源。</p>
    <p v-else-if="selected?.external" class="download-note">将在新标签页打开所选节点。</p>
  </section>
</template>

<style scoped lang="scss">
.game-download { position: relative; min-width: 0; max-width: 100%; }
.download-controls { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }

.download-primary, select {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  min-height: 2.95rem;
  padding: 0.34rem 0.9rem;
  border: 1px solid var(--brass-frame);
  border-radius: var(--control-radius);
  font: inherit;
  line-height: 1.35;
}

.download-primary {
  padding-inline: 1.25rem;
  color: #2c1e12;
  font-weight: 700;
  border-color: #f0cf95;
  background: linear-gradient(125deg, #f5dba5, #cf9f5d);
  box-shadow: 0 4px 12px #080a0699;
  text-decoration: none;

  &:hover { background: linear-gradient(125deg, #fbe5b7, #dbb070); }
}

select {
  max-width: min(100%, 19rem);
  color: var(--foil);
  background: var(--pine);
  cursor: pointer;
}

option, optgroup { color: var(--ink); background: #203128; }
:disabled { opacity: 0.65; cursor: default; }
.download-note { margin: 0.34rem 0 0; color: #ebd6af; font-size: 0.82rem; }
.download-retry {
  min-height: 2rem;
  padding: 0.16rem 0.5rem;
  color: var(--foil);
  font: inherit;
  background: var(--control-dark);
  border: 1px solid var(--brass-frame);
  border-radius: 0.2rem;
  cursor: pointer;
}

@media (max-width: 46rem) {
  .game-download { flex-basis: 100%; }
  .download-controls { display: grid; grid-template-columns: auto minmax(0, 1fr); }
  .download-primary, select { font-size: 0.9rem; min-height: 2.75rem; }
  select { max-width: none; width: 100%; }
}
</style>
