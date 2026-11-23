<script setup lang="ts">
import type { NotesPage } from '../../reader/bookTypes'

defineProps<{ page: NotesPage }>()
</script>

<template>
  <ol class="reader-notes reader-content" :class="'reader-notes--' + page.layout">
    <li v-for="(entry, index) in page.entries" :key="entry.title">
      <span v-if="page.layout === 'recommendations'" class="note-index" aria-hidden="true">0{{ index + 1 }}</span>
      <div class="note-body">
        <span class="entry-label">{{ entry.label }}</span>
        <h4>{{ entry.title }}</h4>
        <p>{{ entry.text }}</p>
        <small>{{ entry.detail }}</small>
      </div>
    </li>
  </ol>
</template>

<style scoped lang="scss">
.reader-notes {
  display: grid;
  align-content: start;
  padding: 0.1rem;
  list-style: none;

  li {
    min-width: 0;
    padding: var(--entry-padding) 0.2rem;
    border-bottom: 1px solid #8a603c59;
  }

  h4 { margin-top: 0.23em; }
  p { margin: 0.36em 0; }
  small { color: #674831; }
}

.reader-notes--notice {
  gap: clamp(0.53rem, 2vh, 1.25rem);

  li {
    padding: calc(var(--entry-padding) * 1.18);
    border: 1px solid #8a603c75;
    border-inline-start: 0.2rem solid var(--entry-accent);
    border-radius: 0.15rem;
    background: #f8e1ad59;
    box-shadow: 0.1rem 0.2rem 0.45rem #62412225;
  }
}

.reader-notes--recommendations li {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  gap: var(--entry-padding);
}

.note-index {
  align-self: start;
  display: grid;
  place-items: center;
  width: clamp(1.24rem, 3vw, 2.6rem);
  aspect-ratio: 1;
  border: 1px solid currentColor;
  border-radius: 50%;
  color: var(--entry-accent);
  font: 700 clamp(0.7rem, 1.5vw, 1.2rem) / 1 Georgia, serif;
}
</style>
