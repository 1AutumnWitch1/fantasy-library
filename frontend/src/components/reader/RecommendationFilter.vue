<script setup lang="ts">
import type { RecommendationType } from '../../api/recommendations'

defineProps<{ selected: RecommendationType | null; disabled: boolean }>()
const emit = defineEmits<{ select: [type: RecommendationType | null] }>()
const filters = [{ value: null, label: '全部' }, { value: 'ANIME', label: '追番' }, { value: 'GAME', label: '游戏' }] as const
</script>

<template>
  <div class="recommendation-filter" role="group" aria-label="推荐作品类型">
    <button v-for="option in filters" :key="option.label" type="button" :disabled="disabled"
      :aria-pressed="selected === option.value" @click="emit('select', option.value)">{{ option.label }}</button>
  </div>
</template>

<style scoped lang="scss">
.recommendation-filter {
  display: inline-flex;
  justify-self: center;
  padding: .18rem;
  gap: .15rem;
  border: 1px solid #a67b4c84;
  border-radius: .2rem;
  background: #231810;
}

button {
  min-height: 2.4rem;
  min-width: 3.2rem;
  padding: 0 .68rem;
  border: 1px solid transparent;
  border-radius: .16rem;
  color: #e8cea0;
  background: transparent;
  font: inherit;
  cursor: pointer;

  &[aria-pressed='true'] { color: #302014; border-color: #c89c5a; background: #e6c48a; }
  &:not(:disabled):hover { border-color: #e6c48a; }
  &:disabled { cursor: default; opacity: .72; }
  &:focus-visible { outline: 2px solid #f4d4a2; outline-offset: 2px; }
}

@media (max-width: 46rem) {
  .recommendation-filter { justify-self: stretch; }
  button { flex: 1; min-height: 2.1rem; font-size: .82rem; }
}
</style>
