<script setup lang="ts">
defineProps<{
  title: string
  options: readonly { id: string; label: string }[]
  selectedTags: readonly string[]
  disabled: boolean
}>()
const emit = defineEmits<{ select: [slug: string] }>()
</script>

<template>
  <nav class="genre-rail" :aria-label="title">
    <h3>{{ title }}</h3>
    <p class="genre-help">可多选 · 同时匹配所选标签</p>
    <div class="genre-options">
      <button type="button" :aria-pressed="selectedTags.length === 0" :disabled="disabled"
        @click="emit('select', '')">全部</button>
      <button v-for="genre in options" :key="genre.id" type="button"
        :aria-pressed="selectedTags.includes(genre.id)" :disabled="disabled"
        @click="emit('select', genre.id)">
        {{ genre.label }}
      </button>
    </div>
  </nav>
</template>

<style scoped lang="scss">
.genre-rail {
  position: relative;
  padding: 0.72rem clamp(0.36rem, 0.55vw, 0.62rem) 0.54rem;
  border: 1px solid #8a6a42;
  border-radius: 0.35rem;
  background:
    linear-gradient(145deg, #223229f7, #161f1a 72%),
    var(--leather-grain) center / 11rem auto;
  color: var(--foil);
  box-shadow: inset 0 0 0 3px #141a15, 0 0.3rem 0.85rem #090604b0;
}

h3 {
  display: flex;
  align-items: center;
  gap: clamp(0.25rem, 0.7vw, 0.5rem);
  margin: 0 0 0.42rem;
  padding-inline: 0.24rem;
  font: 700 clamp(0.95rem, 1.28vw, 1.28rem) / 1.3 var(--display-font);
  letter-spacing: 0.1em;

  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: linear-gradient(90deg, var(--brass-frame), transparent);
  }
}

.genre-help {
  margin: 0 0 0.46rem;
  padding-inline: 0.24rem;
  color: #e4c89d;
  font-size: 0.72rem;
}

.genre-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: clamp(0.22rem, 0.36vw, 0.34rem);
}

button {
  display: grid;
  place-items: center;
  min-width: 0;
  min-height: 2.05rem;
  padding: 0.23rem 0.16rem;
  border: 1px solid #a07d528a;
  border-radius: 0.18rem 0.34rem 0.34rem 0.18rem;
  background: linear-gradient(105deg, #282c21, #1e2b22);
  font: 600 clamp(0.73rem, 0.92vw, 0.9rem) / 1.24 var(--reading-font);
  color: #e9cc9b;
  white-space: nowrap;
  cursor: pointer;
  transition: border-color 160ms ease, background-color 160ms ease;

  &:hover:enabled { border-color: #e6c188; background: #2d3e32; }
  &:focus-visible { outline: 2px solid #f4d396; outline-offset: -2px; }
  &:disabled { cursor: default; opacity: 0.6; }

  &[aria-pressed='true'] {
    background: linear-gradient(105deg, #d9b678, #edd097);
    border-color: #f0ca8a;
    color: #39281a;
    font-weight: 700;
    box-shadow: inset 0 0 0 1px #9f6a397a;
  }
}

@media (max-width: 46rem) {
  .genre-rail { padding: 0.3rem; }
  h3 { display: none; }
  .genre-help { margin-bottom: 0.26rem; }

  .genre-options {
    grid-auto-flow: column;
    grid-auto-columns: max-content;
    grid-template-columns: none;
    gap: 0.32rem;
    overflow-x: auto;
    overscroll-behavior-inline: contain;
    scrollbar-color: #987951 #252a20;
    scrollbar-width: thin;
  }

  button {
    min-height: 2.45rem;
    padding: 0.28rem 0.56rem;
    font-size: clamp(0.76rem, 2.7vw, 0.92rem);
  }
}

@media (prefers-reduced-motion: reduce) {
  button { transition: none; }
}
</style>
