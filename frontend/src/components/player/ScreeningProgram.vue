<script setup lang="ts">
import type { Episode, PlaybackLine, PlaybackSource } from '../../api/playback'
import EpisodePicker from './EpisodePicker.vue'

defineProps<{
  lines: PlaybackLine[]
  episode?: Episode
  source?: PlaybackSource
  autoNext: boolean
  busy: boolean
}>()
const emit = defineEmits<{ line: [id: number]; episode: [value: Episode]; source: [value: PlaybackSource]; autoNext: [value: boolean] }>()
</script>

<template>
  <div class="program-sheet">
    <EpisodePicker id="screening-nodes" :lines="lines" :active-line-id="episode?.nodeId"
      :active-episode-id="episode?.id" :busy="busy" @line="emit('line', $event)" @episode="emit('episode', $event)" />
    <label v-if="(lines.find(line => line.id === episode?.nodeId)?.episodes.length ?? 0) > 1" class="autonext">
      <input type="checkbox" :checked="autoNext" @change="emit('autoNext', ($event.target as HTMLInputElement).checked)">
      自动播放下一段
    </label>
    <fieldset v-if="(episode?.sources.length ?? 0) > 1" class="program-nodes">
      <legend>本集备用文件</legend>
      <div class="node-list">
        <label v-for="option in episode?.sources" :key="option.id" class="node-option">
          <input type="radio" name="screening-node" :checked="source?.id === option.id" @change="emit('source', option)">
          <span>{{ option.label }}</span>
          <small>{{ option.quality }}</small>
        </label>
      </div>
    </fieldset>
  </div>
</template>

<style scoped lang="scss">
.program-sheet {
  margin:.7rem 0;
  color:var(--foil); color-scheme:light;
}
.autonext { display:flex; align-items:center; gap:.34rem; min-height:2.65rem; font-size:.86rem; }
input { accent-color:#395346; }
.program-nodes { min-width:0; padding:.5rem; margin:.5rem 0 0; color:var(--paper-ink); background:var(--paper-base);
  border:1px solid var(--brass-frame); border-radius:.28rem; }
.program-nodes legend { padding:0 .34rem; color:var(--foil); background:var(--wood-shadow); border-radius:.18rem; }
.node-list { display:grid; gap:.33rem; }
.node-option {
  display:grid; grid-template-columns:auto minmax(0,1fr) auto; gap:.53rem; align-items:center;
  min-height:2.6rem; padding:.26rem .58rem; color:#49311d; background:#f8e7bd90; border:1px solid #a5805175;
  border-radius:.22rem; cursor:pointer;
}
.node-option:has(:checked) { color:#263d30; border-color:#526d4f; background:#c8cb9766; }
.node-option:hover { border-color:#826648; }
.node-option small { color:#604629; font-size:.8rem; }
</style>
