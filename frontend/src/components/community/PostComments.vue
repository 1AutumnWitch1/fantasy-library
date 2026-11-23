<script setup lang="ts">
import { nextTick, ref, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'
import { accountError, needsLogin } from '../../api/account'
import { addComment, dateLabel, listComments, type Comment, type Page } from '../../api/community'
import { useAccountStore } from '../../stores/account'
import AuthorAvatar from './AuthorAvatar.vue'
import ReportContent from './ReportContent.vue'

const props=defineProps<{ threadId: number }>()
const account=useAccountStore()
const route=useRoute()
const result=shallowRef<Page<Comment> | null>(null)
const reply=shallowRef<Comment | null>(null)
const body=ref('')
const page=ref(1)
const refresh=ref(0)
const loading=ref(false)
const sending=ref(false)
const error=ref('')
const sendError=ref('')
const editor=ref<HTMLTextAreaElement | null>(null)
let showLastPage=false

void account.restore()

watch([() => props.threadId, page, refresh], async ([id, current], _, onCleanup) => {
  const request=new AbortController()
  onCleanup(() => request.abort())
  loading.value=true
  error.value=''
  try {
    const data=await listComments(id, current, request.signal)
    if (request.signal.aborted) return
    const last=Math.max(data.totalPages, 1)
    const target=showLastPage ? last : Math.min(current, last)
    showLastPage=false
    if (current !== target) page.value=target
    else result.value=data
  } catch (failure) {
    if (!request.signal.aborted) error.value=accountError(failure)
  } finally { if (!request.signal.aborted) loading.value=false }
}, { immediate:true })

async function comment() {
  if (sending.value || !body.value.trim()) return
  sending.value=true
  sendError.value=''
  try {
    await addComment(props.threadId, body.value.trim(), reply.value?.id ?? null)
    body.value=''
    reply.value=null
    showLastPage=true
    refresh.value++
  } catch (failure) {
    if (needsLogin(failure)) account.signedOut()
    sendError.value=accountError(failure)
  } finally { sending.value=false }
}

async function startReply(item: Comment) {
  reply.value=item
  await nextTick()
  editor.value?.focus()
}
</script>

<template>
  <section class="forum-sheet thread" aria-labelledby="thread-title">
    <h2 id="thread-title">页边批注 <small v-if="result">{{ result.total }} 条</small></h2>
    <p v-if="loading" role="status">正在读取评论…</p>
    <p v-else-if="error" class="forum-error" role="alert">{{ error }} <button type="button" @click="refresh++">重试</button></p>
    <template v-else>
      <ol v-if="result?.items.length">
        <li v-for="item in result.items" :key="item.id">
          <div class="forum-meta comment-byline">
            <AuthorAvatar :avatar-key="item.avatarKey" />
            <span>
              <strong>{{ item.authorName }}</strong>
              <template v-if="item.parentId"> · 回复 {{ item.replyTo || '已不可见的批注' }}</template>
              · {{ dateLabel(item.createdAt) }}
            </span>
          </div>
          <p class="forum-body">{{ item.body }}</p>
          <div v-if="account.profile" class="comment-actions">
            <button type="button" :disabled="sending" @click="startReply(item)">回复</button>
            <ReportContent target="COMMENT" :id="item.id" />
          </div>
        </li>
      </ol>
      <p v-else>尚无评论。</p>
      <nav v-if="result && result.totalPages > 1" class="forum-pages" aria-label="评论分页">
        <button type="button" :disabled="page <= 1" @click="page--">上一页</button>
        <span>{{ page }} / {{ result.totalPages }}</span>
        <button type="button" :disabled="page >= result.totalPages" @click="page++">下一页</button>
      </nav>
    </template>
    <hr class="forum-divider">
    <p v-if="sendError" role="alert" class="forum-error">{{ sendError }}</p>
    <form v-if="account.profile" class="forum-form" @submit.prevent="comment">
      <div v-if="reply" class="forum-commands">回复 {{ reply.authorName }} <button type="button" :disabled="sending" @click="reply = null">取消回复</button></div>
      <label>评论<textarea ref="editor" v-model="body" rows="3" maxlength="900" required :disabled="sending" /></label>
      <button type="submit" class="forum-main-button" :disabled="sending || !body.trim()">发送{{ reply ? '回复' : '评论' }}</button>
    </form>
    <RouterLink v-else :to="{ name: 'account', query: { next: route.fullPath } }">登录后参与讨论 ›</RouterLink>
  </section>
</template>

<style scoped lang="scss">
.thread h2 small { font:400 .85rem var(--reading-font); color:var(--paper-muted); }
.thread ol { list-style:none; margin:0; padding:0; }
.thread li { padding:.75rem 0; border-bottom:1px solid var(--forum-line); }
.comment-byline { display:flex; gap:.38rem; align-items:center; overflow-wrap:anywhere; }
.thread .forum-body { margin:.45rem 0; }
.thread form > button { justify-self:start; }
.comment-actions { display:flex; flex-wrap:wrap; align-items: start; gap:.45rem; }
.comment-actions :deep(.report-control) { margin:0; }
</style>
