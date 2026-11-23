<script setup lang="ts">
import { ref } from 'vue'
import type { MotionPreference } from '../../reader/preferences'

defineProps<{ motion: MotionPreference; sound: boolean; forced: boolean; systemReduced: boolean; simple: boolean }>()
const emit = defineEmits<{ motion: [value: MotionPreference]; sound: [value: boolean] }>()
const menu = ref<HTMLDetailsElement | null>(null)
const trigger = ref<HTMLElement | null>(null)

function dismiss() {
  if (!menu.value?.open) return false
  menu.value.open = false
  trigger.value?.focus({ preventScroll: true })
  return true
}

defineExpose({ dismiss })
</script>

<template>
  <details ref="menu" class="reader-settings">
    <summary ref="trigger" aria-label="阅读设置">设置</summary>
    <div class="settings-panel">
      <label for="reader-motion">翻页动画</label>
      <select id="reader-motion" :value="motion" :disabled="forced"
        @change="emit('motion', ($event.target as HTMLSelectElement).value as MotionPreference)">
        <option value="auto">跟随设备</option>
        <option value="full">标准翻书</option>
        <option value="simple">简化过渡</option>
      </select>
      <small v-if="forced">{{ systemReduced ? '已遵循系统的减少动态设置' : '窄屏使用单页简化过渡' }}</small>
      <small v-else-if="motion === 'auto' && simple">当前设备使用简化过渡</small>
      <label class="sound-option">
        <input type="checkbox" :checked="sound" @change="emit('sound', ($event.target as HTMLInputElement).checked)">
        翻页音效
      </label>
    </div>
  </details>
</template>

<style scoped lang="scss">
.reader-settings { position: relative; flex: none; }
summary {
  display: grid; place-items: center; min-height: var(--control-height); padding: .24rem .72rem;
  border: 1px solid var(--brass-frame); border-radius: var(--control-radius);
  background: var(--control-dark); color: var(--foil); cursor: pointer; list-style: none; white-space: nowrap;
  font: .89rem / 1.3 var(--reading-font);
  &::-webkit-details-marker { display: none; }
}
summary:hover, [open] summary { background: var(--control-dark-hover); }
.settings-panel {
  position: absolute; inset: calc(100% + .42rem) 0 auto auto;
  width: min(14.25rem, 82vw); display: grid; gap: .46rem; padding: .85rem;
  border: 1px solid var(--brass-frame); border-radius: .3rem; color: var(--ink);
  background: var(--leather-shadow); box-shadow: 0 .5rem 1.5rem #130804b0;
  font: .88rem / 1.45 var(--reading-font);
  select { width: 100%; padding: .33rem .45rem; min-height: 2.55rem; font: inherit; color: inherit;
    background: var(--control-dark); border: 1px solid var(--brass-frame); border-radius: .2rem; }
  small { color: var(--muted); }
}
.sound-option { display: flex; min-height: 2.35rem; align-items: center; gap: .48rem; cursor: pointer; }
input { width: 1.12rem; height: 1.12rem; accent-color: var(--brass); }
@media (max-width: 46rem) { summary { font-size: .79rem; padding-inline: .42rem; } }
</style>
