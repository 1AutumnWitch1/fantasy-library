<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { avatarUrl } from '../../api/account'
import BookSigil from '../shelf/BookSigil.vue'

const props = defineProps<{ avatarKey?: string | null; preview?: string }>()
const source = computed(() => props.preview || (props.avatarKey ? avatarUrl(props.avatarKey) : ''))
const broken = ref(false)
watch(source, () => { broken.value = false })
</script>

<template>
  <span class="author-avatar" aria-hidden="true">
    <img v-if="source && !broken" :src="source" alt="" @error="broken = true">
    <BookSigil v-else kind="compass" />
  </span>
</template>

<style scoped lang="scss">
.author-avatar {
  display: inline-grid;
  place-items: center;
  width: var(--author-avatar-size, 1.75rem);
  aspect-ratio: 1;
  flex: 0 0 auto;
  border-radius: 50%;
  border: 1px solid #a58154;
  box-shadow: inset 0 0 0 2px #c7a46b4d;
  overflow: hidden;
  color: #e9c689;
  background: #394c40;
  font-size: .99rem;
  img { width: 100%; height: 100%; object-fit: cover; }
  :deep(.sigil) { width: 63%; height: 63%; }
}
</style>
