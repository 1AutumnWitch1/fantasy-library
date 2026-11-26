<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { episodeLabel, type Episode, type PlaybackLine } from '../../api/playback'

const props = defineProps<{
  lines: PlaybackLine[]
  activeLineId?: number
  activeEpisodeId?: number
  busy: boolean
  compact?: boolean
  completedEpisode?: number
}>()
const emit = defineEmits<{ line: [id: number]; episode: [item: Episode] }>()
const reversed = ref(false)
const grid = ref<HTMLUListElement | null>(null)
const currentLine = computed(() => props.lines.find(item => item.id === props.activeLineId) ?? props.lines[0])
const entries = computed(() => {
  const episodes = currentLine.value?.episodes ?? []
  return reversed.value ? [...episodes].reverse() : episodes
})
watch([grid, entries, () => props.activeEpisodeId], ([list]) => {
  if (!props.compact || !list) return
  const active = list.querySelector('[aria-current="true"]')
  if (!active) return
  const bounds = list.getBoundingClientRect()
  const selected = active.getBoundingClientRect()
  if (selected.top < bounds.top) list.scrollTop += selected.top - bounds.top
  else if (selected.bottom > bounds.bottom) list.scrollTop += selected.bottom - bounds.bottom
}, { flush: 'post' })
</script>

<template>
  <section class="episode-picker" :class="{ 'episode-picker--compact': compact }"
    :aria-label="compact ? '选集与线路' : '在线播放'">
    <header>
      <h2>{{ compact ? '选集与线路' : '播放来源与分集' }}</h2>
      <span v-if="compact && entries.length" class="episode-count">{{ entries.length }} {{ entries.every(item => item.edition === 'EPISODE') ? '集' : '段' }}</span>
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
      <ul ref="grid" class="episode-grid" aria-label="当前来源的分集"
        :tabindex="compact ? 0 : undefined">
        <li v-for="stage in entries" :key="stage.id">
          <button type="button" :aria-current="activeEpisodeId === stage.id ? 'true' : undefined"
            :title="stage.title" :aria-label="compact ? episodeLabel(stage) + (stage.edition === 'EPISODE' && completedEpisode === stage.number ? '，已观看' : '') : undefined"
            :disabled="busy" @click="emit('episode', stage)">
            <span>{{ compact && stage.edition === 'EPISODE' ? stage.number : episodeLabel(stage) }}</span>
            <small v-if="!compact">{{ stage.title }}</small>
            <span v-if="compact && stage.edition === 'EPISODE' && completedEpisode === stage.number" class="viewed-icon" aria-hidden="true">✓</span>
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

.episode-picker--compact {
  padding: 0;
  border: 0;
  background: none;
  box-shadow: none;
  h2 { font-size: 1.28rem; }
  header { justify-content: flex-start; gap: .38rem .5rem; margin-bottom: .55rem; }
  .picker-tools { margin-inline-start: auto; }
  .episode-count { color: var(--paper-muted); font-size: .77rem; }
  .source-block { gap: .32rem .55rem; margin-bottom: .68rem; }
  .line-tabs { flex: 1; min-width: 0; }
  .line-tabs button { min-height: 2.1rem; padding: .24rem .55rem; overflow-wrap: anywhere; font-size: .82rem; }
  .episode-grid {
    grid-template-columns: repeat(auto-fill, minmax(min(100%, 3.65rem), 1fr));
    gap: .36rem;
    max-height: clamp(9rem, 36vh, 19rem);
    overflow-y: auto;
    overscroll-behavior-y: contain;
    scrollbar-color: #95815c #97734522;
    padding: .38rem;
    background: #a67c4218;
  }
  .episode-grid button {
    position: relative;
    min-height: 2.7rem;
    padding-inline: .18rem;
    background-color: #e6cc9b;
    font-size: .91rem;
    font-weight: 600;
  }
  .episode-grid button:hover:enabled { background: #f3dbad; }
  .episode-grid button[aria-current='true'] { color: #ffe5a6; background: var(--pine); }
  .viewed-icon { position: absolute; inset: .07rem .18rem auto auto; font-size: .64rem; }
}
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
