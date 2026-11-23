<script setup lang="ts">
import type { PersonalChapter } from '../../api/personal'

defineProps<{ selected: PersonalChapter; unread: number; disabled: boolean }>()
const emit = defineEmits<{ select: [chapter: PersonalChapter] }>()
const chapters = [
  { id: 'anime', label: '追番' },
  { id: 'games', label: '游戏愿望单' },
  { id: 'history', label: '观看历史' },
  { id: 'inbox', label: '收到的消息' },
] as const
</script>

<template>
  <nav class="personal-tabs" aria-label="私人书架目录">
    <button v-for="chapter in chapters" :key="chapter.id" type="button" :disabled="disabled"
      :aria-pressed="selected === chapter.id" @click="emit('select', chapter.id)">
      {{ chapter.label }}
      <span v-if="chapter.id === 'inbox' && unread" class="inbox-badge" :aria-label="unread + ' 条未读'">
        {{ unread > 99 ? '99+' : unread }}
      </span>
    </button>
  </nav>
</template>

<style scoped lang="scss">
.personal-tabs {
  display: flex;
  justify-content: center;
  justify-self: center;
  gap: 0.34rem;
  min-width: 0;
}

button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.28rem;
  padding: 0.38rem 0.78rem;
  min-height: 2.55rem;
  background: linear-gradient(130deg, #422b3e, #2b1e28);
  border: 1px solid #a07668;
  border-radius: 0.24rem 0.24rem 0.08rem 0.08rem;
  color: #f0d8af;
  font: inherit;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 160ms ease, border-color 160ms ease;

  &[aria-pressed='true'] {
    background: linear-gradient(180deg, #f2d8aa, #dec08c);
    border-color: #f4d8a5;
    color: #39281a;
    font-weight: 700;
    box-shadow: 0 2px 8px #0d060466;
  }

  &:hover:enabled:not([aria-pressed='true']) { border-color: #f2cd95; background: #543749; }
  &:focus-visible { outline: 2px solid #f4d295; outline-offset: 2px; }
  &:disabled { opacity: 0.65; cursor: default; }
}

.inbox-badge {
  display: inline-block;
  min-width: 1.35em;
  padding-inline: 0.28em;
  border-radius: 1em;
  background: #713b48;
  color: #fff0cb;
  font-size: 0.76em;
  line-height: 1.35;
}

@media (max-width: 46rem) {
  .personal-tabs { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); justify-self: stretch; gap: 0.32rem; }
  button { font-size: 0.84rem; padding: 0.35rem 0.45rem; min-height: 2.45rem; white-space: nowrap; }
}

@media (prefers-reduced-motion: reduce) {
  button { transition: none; }
}
</style>
