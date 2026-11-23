<script setup lang="ts">
import { computed } from 'vue'
import type { CatalogKind, NamedItem } from '../../api/catalog'
import ReaderGenreRail from './ReaderGenreRail.vue'
const props = defineProps<{
  kind: CatalogKind
  tags: NamedItem[]
  selectedTags: readonly string[]
  disabled: boolean
  loading: boolean
  error: string
}>()
const emit = defineEmits<{ tag: [value: string]; retry: [] }>()

const tagOptions = computed(() => {
  const priority = props.kind === 'games'
    ? ['动作', '角色扮演', '冒险', '开放世界', '射击', '策略', '模拟经营', '解谜']
    : ['悬疑', '恋爱', '推理', '奇幻', '冒险', '热血', '校园', '日常']
  const rank = (name: string) => {
    const index = priority.indexOf(name)
    return index < 0 ? priority.length : index
  }
  const tags = [...props.tags].sort((a, b) => rank(a.name) - rank(b.name))
  return tags.map(tag => ({ id: tag.slug, label: tag.name }))
})
</script>

<template>
  <aside class="catalog-filters" aria-label="馆藏筛选">
    <p v-if="error" role="alert">{{ error }} <button type="button" @click="emit('retry')">重试筛选项</button></p>
    <ReaderGenreRail :title="kind === 'games' ? '游戏标签' : '番剧标签'"
      :options="tagOptions"
      :selected-tags="selectedTags" :disabled="disabled || loading" @select="emit('tag', $event)" />
  </aside>
</template>

<style scoped lang="scss">
.catalog-filters { display: grid; gap: 0.48rem; min-width: 0; color: #eed7ab; }
p { margin: 0; font-size: 0.8rem; }
@media(max-width:46rem) {
  .catalog-filters { gap: 0.2rem; }
}
</style>
