<script setup lang="ts">
import type { CollectionPage } from '../../reader/bookTypes'

defineProps<{ page: CollectionPage }>()
</script>

<template>
  <ul class="reader-collections reader-content" :class="'reader-collections--' + page.layout">
    <li v-for="entry in page.entries" :key="entry.title">
      <div class="collection-heading">
        <h4>{{ entry.title }}</h4>
        <span class="entry-label">{{ entry.kind }}</span>
      </div>
      <p>{{ entry.note }}</p>
      <div v-if="entry.progress" class="collection-progress">
        <progress :value="entry.progress.current" :max="entry.progress.total"
          :aria-label="entry.title + ' · ' + entry.progress.label"></progress>
        <small>{{ entry.progress.label }}</small>
      </div>
    </li>
  </ul>
</template>

<style scoped lang="scss">
.reader-collections {
  display: grid;
  align-content: start;
  padding: 0.1rem;
  list-style: none;

  li {
    padding: var(--entry-padding);
    border-bottom: 1px solid #8a603c60;
  }

  p { margin: 0.3em 0 0; }
}

.reader-collections--progress {
  gap: clamp(0.5rem, 1.65vh, 1.05rem);

  li {
    border: 1px solid #8a603c60;
    border-radius: 0.2rem;
    background: #f3dcad60;
    box-shadow: inset 0 1px #ffebc58a;
  }
}

.reader-collections--list {
  li { border-inline-start: 1px solid #8a603c60; }
}

.collection-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.15em 0.55em;
}

.collection-progress {
  display: grid;
  gap: 0.28em;
  margin-top: 0.6em;
  color: var(--entry-accent);

  progress {
    width: 100%;
    height: 0.52em;
    appearance: none;
    accent-color: var(--entry-accent);
    border: 0;
    border-radius: 1rem;
    background: #6d4d3038;

    &::-webkit-progress-bar { border-radius: inherit; background: #6d4d3038; }
    &::-webkit-progress-value { border-radius: inherit; background: var(--entry-accent); }
    &::-moz-progress-bar { border-radius: inherit; background: var(--entry-accent); }
  }
}
</style>
