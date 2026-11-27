<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { accountError, needsLogin } from '../../api/account'
import { dateLabel, likeScreeningComment, type Reaction, type ScreeningComment } from '../../api/community'
import { useAccountStore } from '../../stores/account'
import AuthorAvatar from '../community/AuthorAvatar.vue'
import ReportContent from '../community/ReportContent.vue'

const props = defineProps<{ comment: ScreeningComment; workId: number; game?: boolean; loginTo: { name: string; query: { next: string } } }>()
const emit = defineEmits<{ reply: [comment: ScreeningComment] }>()
const account = useAccountStore()
const changed = shallowRef<Reaction>()
const liked = computed(() => changed.value?.liked ?? props.comment.liked)
const count = computed(() => changed.value?.count ?? props.comment.likeCount)
const saving = ref(false)
const error = ref('')
watch(() => props.comment, () => { changed.value = undefined })

async function like() {
  if (saving.value) return
  saving.value = true
  error.value = ''
  try {
    changed.value = await likeScreeningComment(props.workId, props.comment.id, !liked.value, props.game)
  } catch (reason) {
    if (needsLogin(reason)) account.signedOut()
    error.value = accountError(reason)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <p v-if="comment.removed" class="removed-comment">这条评论已移除</p>
  <div v-else class="comment-line">
    <AuthorAvatar :avatar-key="comment.avatarKey" />
    <div class="comment-text">
      <div class="comment-message">
        <strong>{{ comment.authorName }}</strong>
        <template v-if="comment.rootId !== null">
          <span v-if="comment.parentId !== comment.rootId" class="reply-recipient">回复 {{ comment.replyTo || '已移除的评论' }}</span>
          <p>{{ comment.body }}</p>
        </template>
      </div>
      <p v-if="comment.rootId === null" class="comment-body">{{ comment.body }}</p>
      <div class="comment-tools">
        <time :datetime="comment.createdAt">{{ dateLabel(comment.createdAt) }}</time>
        <template v-if="account.profile">
          <button class="comment-like" type="button" :aria-pressed="liked" :disabled="saving" @click="like">
            {{ liked ? '已点赞' : '点赞' }} <span v-if="count">{{ count }}</span>
          </button>
          <button type="button" @click="emit('reply', comment)">回复</button>
          <ReportContent target="COMMENT" :id="comment.id" />
        </template>
        <template v-else>
          <RouterLink :to="loginTo" aria-label="登录后点赞">点赞<span v-if="count"> {{ count }}</span></RouterLink>
          <RouterLink :to="loginTo" aria-label="登录后回复">回复</RouterLink>
        </template>
      </div>
      <p v-if="error" class="forum-error" role="alert">{{ error }}</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.comment-line { display:grid; grid-template-columns:auto minmax(0,1fr); gap:.65rem; align-items:start; }
.comment-text { min-width:0; overflow-wrap:anywhere; }
.comment-message { display:flex; flex-wrap:wrap; align-items:baseline; gap:.2rem .6rem; color:var(--paper-ink); }
.comment-message strong { color:var(--pine); }
.comment-message p, .comment-body { margin:0; white-space:pre-wrap; line-height:1.55; }
.comment-body { margin-top:.2rem; color:var(--paper-ink); }
.reply-recipient, .removed-comment { color:var(--paper-muted); }
.removed-comment { font-style:italic; margin:.15rem 0; }
.comment-tools { display:flex; align-items:center; flex-wrap:wrap; gap:.1rem .6rem; margin-top:.3rem; color:var(--paper-muted); font-size:.82rem; }
.comment-tools button, .comment-tools a, .comment-tools :deep(.report-control > button) {
  min-height:2rem; padding:.2rem .28rem; border-color:transparent; background:transparent; color:var(--pine); font:inherit;
}
.comment-tools :deep(.report-control) { margin:0; }
.comment-tools time { margin-inline-end:.1rem; }
.comment-tools .comment-like[aria-pressed="true"] { color:#754027; font-weight:700; }
@media (max-width:38rem) {
  .comment-tools { gap:.1rem .38rem; }
  .comment-tools button, .comment-tools a, .comment-tools :deep(.report-control > button) { min-height:2.45rem; }
}
</style>
