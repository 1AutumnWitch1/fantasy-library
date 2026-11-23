<script setup lang="ts">
import { computed } from 'vue'
import { useHealthStore } from '../stores/health'

const healthStore = useHealthStore()

const databaseLabels = {
  UP: '已连接',
  NOT_CONFIGURED: '未配置或配置不完整',
  DOWN: '连接失败',
}

const apiLabel = computed(() => {
  switch (healthStore.phase) {
    case 'ready': return '已连接'
    case 'failed': return '无法访问健康检查 API'
    default: return '检测中…'
  }
})

const databaseLabel = computed(() => {
  if (healthStore.health) return databaseLabels[healthStore.health.database]
  return healthStore.phase === 'failed' ? '状态未知' : '检测中…'
})
</script>

<template>
  <dl class="status-list" aria-live="polite">
    <div class="status-row">
      <dt>后端 API</dt>
      <dd :class="{ healthy: healthStore.phase === 'ready', warning: healthStore.phase === 'failed' }">
        {{ apiLabel }}
      </dd>
    </div>
    <div class="status-row">
      <dt>Oracle 数据库</dt>
      <dd :class="{ healthy: healthStore.health?.database === 'UP', warning: healthStore.health?.database === 'DOWN' }">
        {{ databaseLabel }}
      </dd>
    </div>
  </dl>

  <button type="button" :disabled="healthStore.phase === 'loading'" @click="healthStore.refresh">
    重新检测
  </button>
</template>

<style scoped lang="scss">
.status-list {
  margin: 1.6rem 0;
  border-block: 1px solid var(--line);
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.3rem 1rem;
  padding: 0.8rem 0;

  & + & {
    border-top: 1px solid var(--line);
  }
}

dd {
  margin: 0;
  color: var(--muted);

  &.healthy { color: var(--success); }
  &.warning { color: var(--warning); }
}

button {
  padding: 0.6rem 1.1rem;
  border: 1px solid var(--brass-frame);
  border-radius: var(--control-radius);
  background: var(--control-dark);
  color: var(--ink);
  font: inherit;
  cursor: pointer;

  &:hover:enabled { background: var(--control-dark-hover); }
  &:focus-visible { outline: 2px solid var(--brass); outline-offset: 4px; }
  &:disabled { opacity: 0.65; cursor: wait; }
}
</style>
