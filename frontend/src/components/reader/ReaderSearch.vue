<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{ disabled: boolean; label?: string; placeholder?: string }>(), {
  label: '搜索视频',
  placeholder: '搜视频名或题材',
})
const query = defineModel<string>({ required: true })
const input = ref<HTMLInputElement | null>(null)

function clear() {
  query.value = ''
  input.value?.focus()
}

function handleEscape(event: KeyboardEvent) {
  if (event.isComposing || !query.value) return
  event.preventDefault()
  event.stopPropagation()
  clear()
}
</script>

<template>
  <div class="reader-search">
    <span class="reader-search__icon" aria-hidden="true"></span>
    <input ref="input" v-model="query" type="search" :aria-label="label"
      :disabled="disabled" :placeholder="placeholder" autocomplete="off" @keydown.esc="handleEscape" />
    <button v-if="query" type="button" :disabled="disabled" aria-label="清空搜索" @click="clear">
      <span aria-hidden="true">×</span>
    </button>
  </div>
</template>

<style scoped lang="scss">
.reader-search {
  display: flex;
  align-items: center;
  gap: 0.44rem;
  min-width: 0;
  min-height: 2.18rem;
  padding-inline: 0.66rem 0.32rem;
  border: 1px solid #987443b3;
  border-radius: 0.23rem;
  color: #52381f;
  background: #ffefbe89;
  box-shadow: inset 0 1px 3px #68401b1e;
  color-scheme: light;

  &:focus-within {
    border-color: #7d633b;
    box-shadow: 0 0 0 2px #72613f78;
  }
}

.reader-search__icon {
  position: relative;
  flex: none;
  width: 0.66rem;
  height: 0.66rem;
  margin-inline-end: 0.11rem;
  border: 1px solid currentColor;
  border-radius: 50%;

  &::after {
    content: '';
    position: absolute;
    top: 0.51rem;
    left: 0.46rem;
    width: 0.29rem;
    height: 1px;
    transform: rotate(45deg);
    transform-origin: left center;
    background: currentColor;
  }
}

input {
  flex: 1 1 auto;
  width: 100%;
  min-width: 0;
  padding: 0.35rem 0;
  border: 0;
  outline: none;
  background: transparent;
  color: inherit;
  font: 600 clamp(0.78rem, 1.03vw, 0.98rem) / 1.2 var(--display-font);

  &::placeholder { color: #755b3a; opacity: 1; }
  &::-webkit-search-cancel-button { appearance: none; }
  &:disabled { opacity: 1; -webkit-text-fill-color: currentColor; }
}

button {
  flex: none;
  min-width: 1.55rem;
  min-height: 1.55rem;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 0.16rem;
  color: inherit;
  background: transparent;
  font: 1.23rem / 1 Georgia, serif;
  cursor: pointer;

  &:hover:enabled { background: #a5773a28; }
  &:focus-visible { outline: 1px solid #7d633b; }
  &:disabled { cursor: default; }
}

@media (max-width: 46rem) {
  .reader-search { min-height: 1.8rem; padding-inline-start: 0.44rem; gap: 0.3rem; }
  input { padding-block: 0.26rem; font-size: clamp(0.68rem, 2.4vw, 0.78rem); }
}
</style>
