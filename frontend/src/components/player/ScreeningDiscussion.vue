<script setup lang="ts">
import { computed, nextTick, ref, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'
import { accountError, needsLogin } from '../../api/account'
import { addScreeningComment, listScreeningThreads, type Page, type ScreeningComment, type ScreeningThread } from '../../api/community'
import { useAccountStore } from '../../stores/account'
import AuthorAvatar from '../community/AuthorAvatar.vue'
import ScreeningCommentRow from './ScreeningCommentRow.vue'
import ScreeningReplies from './ScreeningReplies.vue'

const props = defineProps<{ workId: number; game?: boolean }>()
const route = useRoute()
const account = useAccountStore()
const member = computed(() => !!account.profile)
const discussionId = computed(() => props.game ? 'game-discussion' : 'screening-discussion')
const loginTo = computed(() => ({ name: 'account', query: { next: route.fullPath.split('#', 1)[0] + '#' + discussionId.value } }))
const page = ref(1)
const refresh = ref(0)
const result = shallowRef<Page<ScreeningThread>>()
const loading = ref(true)
const error = ref('')
const sending = ref(false)
const sendError = ref('')
const body = ref('')
const replyBody = ref('')
const replyTo = shallowRef<ScreeningComment>()
const replyEditor = ref<HTMLTextAreaElement[]>()
const repliesChanged = shallowRef<{ root: number; version: number }>()
let pendingRoot: number | undefined

void account.restore()

watch([page, refresh, member], async ([number, version, loggedIn], _, onCleanup) => {
  const request = new AbortController()
  onCleanup(() => request.abort())
  loading.value = true
  error.value = ''
  try {
    const data = await listScreeningThreads(props.workId, number, loggedIn, request.signal, props.game)
    if (request.signal.aborted) return
    if (number > Math.max(data.totalPages, 1)) {
      page.value = Math.max(data.totalPages, 1)
      return
    }
    result.value = data
    if (pendingRoot !== undefined) {
      repliesChanged.value = { root: pendingRoot, version }
      pendingRoot = undefined
    }
  } catch (reason) {
    if (!request.signal.aborted) error.value = accountError(reason)
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
}, { immediate: true })

async function openReply(item: ScreeningComment) {
  replyTo.value = item
  sendError.value = ''
  await nextTick()
  replyEditor.value?.[0]?.focus()
}

async function submit(target?: ScreeningComment) {
  const draft = target ? replyBody : body
  if (!member.value || sending.value || !draft.value.trim()) return
  sending.value = true
  sendError.value = ''
  try {
    await addScreeningComment(props.workId, draft.value.trim(), target?.id ?? null, props.game)
    draft.value = ''
    if (target) {
      replyTo.value = undefined
      pendingRoot = target.rootId ?? target.id
    } else {
      page.value = 1
    }
    refresh.value++
  } catch (reason) {
    if (needsLogin(reason)) account.signedOut()
    sendError.value = accountError(reason)
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="screening-comments">
    <section :id="discussionId" class="forum-sheet screening-thread" :aria-labelledby="discussionId + '-title'">
      <header class="discussion-heading">
        <h2 :id="discussionId + '-title'">{{ game ? '游戏评论' : '番剧评论' }} <small v-if="result">{{ result.total }} 条</small></h2>
        <span v-if="result" class="discussion-order">最新</span>
      </header>
      <div v-if="account.profile" class="comment-intake">
        <AuthorAvatar :avatar-key="account.profile.avatarKey" />
        <form class="forum-form" @submit.prevent="submit()">
          <label class="sr-comment-label"  :for="discussionId + '-new-comment'">发表评论</label>
          <textarea :id="discussionId + '-new-comment'" v-model="body" rows="2" maxlength="900" required :disabled="sending"
             :placeholder="game ? '说说你的游戏体验……' : '说说你的观后感…'" />
          <button type="submit" class="forum-main-button" :disabled="sending || !body.trim()">{{ game ? '发表评论' : '发送评论' }}</button>
        </form>
      </div>
      <p v-else class="comment-intake"><RouterLink :to="loginTo">登录后发表评论 ›</RouterLink></p>
      <p v-if="loading && !result" role="status">正在读取评论…</p>
      <p v-if="error" role="alert" class="forum-error">{{ error }} <button type="button" @click="refresh++">重试</button></p>
      <p v-if="sendError && !replyTo" class="forum-error" role="alert">{{ sendError }}</p>
      <ol v-if="result?.items.length" class="screening-roots" aria-label="评论">
        <li v-for="group in result.items" :key="group.comment.id" class="comment-thread">
          <ScreeningCommentRow :comment="group.comment"  :work-id="workId" :game="game" :login-to="loginTo" @reply="openReply" />
          <ScreeningReplies :group="group"  :work-id="workId" :game="game" :member="member" :login-to="loginTo"
            :refresh="repliesChanged?.root === group.comment.id ? repliesChanged.version : 0" @reply="openReply" />
          <form v-if="account.profile && replyTo && (replyTo.rootId ?? replyTo.id) === group.comment.id" class="forum-form reply-form"
            @submit.prevent="submit(replyTo)">
            <label>
              回复 {{ replyTo.authorName }}
              <textarea ref="replyEditor" v-model="replyBody" rows="2" maxlength="900" required :disabled="sending" />
            </label>
            <p v-if="sendError" class="forum-error" role="alert">{{ sendError }}</p>
            <div class="forum-commands">
              <button type="submit" class="forum-main-button" :disabled="sending || !replyBody.trim()">发送回复</button>
              <button type="button" :disabled="sending" @click="replyTo = undefined; sendError = ''">取消</button>
            </div>
          </form>
        </li>
      </ol>
      <p v-else-if="result && !error && !loading">还没有评论。</p>
      <nav v-if="result && result.totalPages > 1" class="forum-pages" aria-label="评论分页">
        <button type="button" :disabled="loading || sending || page <= 1" @click="page--">上一页</button>
        <span>{{ page }} / {{ result.totalPages }}</span>
        <button type="button" :disabled="loading || sending || page >= result.totalPages" @click="page++">下一页</button>
      </nav>
    </section>
  </div>
</template>

<style scoped lang="scss">
.screening-thread {
  scroll-margin-top: calc(var(--library-header-height) + 1rem);
  margin: 0;
  padding: clamp(.8rem, 1.8vw, 1.38rem);
  border-radius: 0 0 .34rem .34rem;
}
.discussion-heading { display: flex; align-items: baseline; flex-wrap: wrap; gap: .4rem 1rem; padding-bottom: .52rem; border-bottom: 1px solid var(--forum-line); }
#game-discussion { border-radius: .34rem; }
#game-discussion .discussion-heading {
  padding: .55rem .75rem;
  background: var(--pine);
  border-radius: .24rem;
}
#game-discussion :is(h2, small, .discussion-order) { color: var(--foil); }
.discussion-heading h2 { margin: 0; font: 700 1.23rem/1.3 var(--display-font); }
.discussion-order { padding-inline-start: .8rem; border-inline-start: 1px solid var(--forum-line); color: var(--paper-muted); font-size: .84rem; }
.discussion-heading small { margin-inline-start:.25rem; font:400 .9rem var(--reading-font); color:var(--paper-muted); }
.comment-intake { display:flex; align-items:start; gap:.7rem; padding:.85rem 0; margin:0; }
.comment-intake .forum-form { position:relative; flex:1; min-width:0; margin:0; grid-template-columns:minmax(0,1fr) auto; align-items:end; }
.sr-comment-label { position:absolute; width:1px; height:1px; padding:0; margin:-1px; overflow:hidden; clip-path:inset(50%); white-space:nowrap; }
.screening-roots { list-style:none; margin:0; padding:0; }
.comment-thread { padding: .72rem 0; }
.comment-thread + .comment-thread { border-top: 1px solid var(--forum-line); }
.reply-form { margin-inline-start:2.65rem; }
@media (max-width:34rem) {
  .comment-intake .forum-form { grid-template-columns:1fr; gap:.5rem; }
  .comment-intake button { justify-self:end; }
  .reply-form { margin-inline-start:.9rem; }
}
</style>
