<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { accountError } from '../../api/account'
import { listScreeningReplies, type Page, type ScreeningComment, type ScreeningThread } from '../../api/community'
import ScreeningCommentRow from './ScreeningCommentRow.vue'

const props = defineProps<{
  group: ScreeningThread; workId: number; game?: boolean; member: boolean; refresh: number
  loginTo: { name: string; query: { next: string } }
}>()
const emit = defineEmits<{ reply: [comment: ScreeningComment] }>()
const expanded = ref(false)
const page = ref(1)
const result = shallowRef<Page<ScreeningComment>>()
const error = ref('')
const loading = ref(false)
const retry = ref(0)
const preview = computed(() => props.group.replies)
const items = computed(() => expanded.value && result.value ? result.value.items : preview.value)

watch(() => props.refresh, () => {
  if (!props.refresh || props.group.comment.replyCount <= preview.value.length) return
  page.value = Math.max(1, Math.ceil(props.group.comment.replyCount / 6))
  expanded.value = true
})

watch([expanded, page, () => props.member, () => props.refresh, retry], async ([open, number], _, onCleanup) => {
  if (!open) return
  const request = new AbortController()
  onCleanup(() => request.abort())
  loading.value = true
  error.value = ''
  try {
    const data = await listScreeningReplies(props.workId, props.group.comment.id, number, props.member, request.signal, props.game)
    if (request.signal.aborted) return
    if (number > Math.max(1, data.totalPages)) page.value = Math.max(1, data.totalPages)
    else result.value = data
  } catch (reason) {
    if (!request.signal.aborted) error.value = accountError(reason)
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
})

function toggle() {
  page.value = 1
  result.value = undefined
  loading.value = false
  error.value = ''
  expanded.value = !expanded.value
}
</script>

<template>
  <ol v-if="items.length" class="screening-replies" aria-label="回复">
    <li v-for="item in items" :key="item.id">
      <ScreeningCommentRow :comment="item" :work-id="workId" :game="game" :login-to="loginTo" @reply="emit('reply', $event)" />
    </li>
  </ol>
  <div v-if="expanded || group.comment.replyCount > preview.length" class="reply-controls">
    <button type="button" :aria-expanded="expanded" @click="toggle">
      {{ expanded ? '收起回复' : '查看全部 ' + group.comment.replyCount + ' 条回复' }}
    </button>
    <span v-if="expanded && loading" role="status">正在读取回复…</span>
    <p v-else-if="expanded && error" role="alert" class="forum-error">{{ error }} <button type="button" @click="retry++">重试</button></p>
    <nav v-else-if="expanded && result && result.totalPages > 1" aria-label="回复分页">
      <button type="button" :disabled="page <= 1" @click="page--">上一页</button>
      <span>{{ page }} / {{ result.totalPages }}</span>
      <button type="button" :disabled="page >= result.totalPages" @click="page++">下一页</button>
    </nav>
  </div>
</template>

<style scoped lang="scss">
.screening-replies { list-style:none; margin:.25rem 0 .25rem 2.65rem; padding:0 0 0 .65rem; border-inline-start:1px solid var(--forum-line); }
.screening-replies li { padding:.3rem 0; }
.reply-controls, .reply-controls nav { display:flex; flex-wrap:wrap; align-items:center; gap:.35rem; }
.reply-controls { margin-inline-start:2.65rem; color:var(--paper-muted); font-size:.85rem; }
.reply-controls button { min-height:2rem; padding:.16rem .4rem; }
@media (max-width:34rem) {
  .screening-replies, .reply-controls { margin-inline-start:.9rem; }
}
</style>
