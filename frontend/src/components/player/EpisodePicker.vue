<script setup lang="ts">
import { computed, ref } from 'vue'
import { episodeLabel, type Episode, type PlaybackLine } from '../../api/playback'

const props = defineProps<{
  lines: PlaybackLine[]
  activeLineId?: number
  activeEpisodeId?: number
  busy: boolean
}>()
const emit = defineEmits<{ line: [id: number]; episode: [item: Episode] }>()
const reversed = ref(false)
const currentLine = computed(() => props.lines.find(item => item.id === props.activeLineId) ?? props.lines[0])
const entries = computed(() => {
  const episodes = currentLine.value?.episodes ?? []
  return reversed.value ? [...episodes].reverse() : episodes
})
</script>

<template>
  <section class="episode-picker" aria-label="在线播放">
    <header>
      <h2>播放来源与分集</h2>
      <div class="picker-tools">
        <slot name="action" />
        <button v-if="entries.length > 1" class="sort-button" type="button"
          :aria-label="reversed ? '按集数正序排列' : '按集数倒序排列'" @click="reversed = !reversed">
          {{ reversed ? '↑ 倒序' : '↓ 正序' }}
        </button>
      </div>
    </header>
    <template v-if="currentLine">
      <div class="source-block">
        <span class="block-label">播放来源</span>
        <div class="line-tabs" role="group" aria-label="播放来源">
          <button v-for="line in lines" :key="line.id" type="button"
            :aria-pressed="currentLine.id === line.id" :disabled="busy" @click="emit('line', line.id)">
            {{ line.label }}
          </button>
        </div>
      </div>
      <ul class="episode-grid" aria-label="当前来源的分集">
        <li v-for="stage in entries" :key="stage.id">
          <button type="button" :aria-current="activeEpisodeId === stage.id ? 'true' : undefined"
            :title="stage.title" :disabled="busy" @click="emit('episode', stage)">
            <span>{{ episodeLabel(stage) }}</span>
            <small>{{ stage.title }}</small>
          </button>
        </li>
      </ul>
    </template>
    <p v-else role="status">{{ busy ? '正在读取分集…' : '暂无可播放的分集。' }}</p>
  </section>
</template>

<style scoped lang="scss">
.episode-picker {
  padding: clamp(0.75rem, 1.7vw, 1.2rem);
  color: var(--paper-ink);
  color-scheme: light;
  background: var(--paper-surface);
  border: 1px solid var(--brass-frame);
  border-radius: 0.4rem;
  box-shadow: var(--surface-shadow), var(--paper-inset);
  scroll-margin-top: 1rem;
}

header, .picker-tools, .line-tabs { display: flex; align-items: center; flex-wrap: wrap; gap: 0.45rem; }
header { justify-content: space-between; margin-bottom: 0.62rem; }
h2 { margin: 0; color: var(--paper-ink); font: 700 clamp(1.12rem, 1.9vw, 1.48rem) / 1.2 var(--display-font); }

.source-block {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.45rem 0.7rem;
  margin-bottom: 0.62rem;
}

.block-label {
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--paper-muted);
  letter-spacing: 0.04em;
}

.line-tabs { padding: 0; }

button {
  min-height: 2.6rem;
  padding: 0.25rem 0.68rem;
  color: var(--paper-ink);
  font: inherit;
  cursor: pointer;
  border: 1px solid #8a663e85;
  border-radius: 0.25rem;
  background: #f9e8bf9c;
  transition: background-color 150ms ease, border-color 150ms ease;
}

button:hover:enabled { border-color: #78572e; background: #f6dcab; }

button[aria-pressed='true'], button[aria-current='true'] {
  color: #ffe8bb;
  border-color: #b68b53;
  background: var(--pine);
  box-shadow: inset 0 1px #e4cc9c46;
}

.episode-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 7.4rem), 1fr));
  gap: 0.45rem;
  list-style: none;
  padding: 0.56rem;
  margin: 0;
  border: 1px solid #875f3640;
  border-radius: 0.26rem;
  background: #b88b4b1e;
}

.episode-grid li { min-width: 0; }
.episode-grid button {
  display: grid;
  place-content: center;
  width: 100%;
  min-height: 3.15rem;
  text-align: center;
}
.episode-grid small { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; opacity: 0.9; font-size: 0.76rem; }
.sort-button { min-height: 2.25rem; font-size: 0.82rem; }
p { margin: 0; }

@media (max-width: 35rem) {
  .episode-grid { grid-template-columns: repeat(auto-fill, minmax(min(100%, 6rem), 1fr)); }
}

@media (prefers-reduced-motion: reduce) {
  button { transition: none; }
}
</style>
