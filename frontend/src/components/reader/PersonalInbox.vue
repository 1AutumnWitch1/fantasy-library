<script setup lang="ts">
import { dateLabel } from '../../api/community'
import type { Notice, NoticeKind } from '../../api/personal'
import AuthorAvatar from '../community/AuthorAvatar.vue'

defineProps<{ notices: Notice[]; unread: number; interactive: boolean; busy: boolean }>()
const emit = defineEmits<{ markRead: [id?: number] }>()
const descriptions: Record<NoticeKind, string> = {
  POST_COMMENT: '评论了你的帖子',
  COMMENT_REPLY: '回复了你的评论',
  POST_LIKE: '赞了你的帖子',
}
</script>

<template>
  <div v-if="unread" class="inbox-summary">
    <span>{{ unread }} 条未读</span>
    <button v-if="interactive" type="button" :disabled="busy" @click="emit('markRead')">全部标为已读</button>
  </div>
  <ul class="inbox-stack">
    <li v-for="notice in notices" :key="notice.id" :class="{ unread: !notice.read }">
      <header>
        <AuthorAvatar :avatar-key="notice.avatarKey" />
        <p><strong>{{ notice.actorName }}</strong> {{ descriptions[notice.kind] }}</p>
      </header>
      <h4>
        <RouterLink v-if="interactive" :to="notice.screeningWorkId
            ? { name: 'work-watch', params: { id: notice.screeningWorkId }, hash: '#screening-discussion' }
            : { name: 'community-post', params: { id: notice.postId }, query: { from: 'personal' } }">
          {{ notice.postTitle }}
        </RouterLink>
        <span v-else>{{ notice.postTitle }}</span>
      </h4>
      <p v-if="notice.excerpt" class="inbox-excerpt">{{ notice.excerpt }}</p>
      <footer>
        <time :datetime="notice.createdAt">{{ dateLabel(notice.createdAt) }}</time>
        <button v-if="!notice.read && interactive" type="button"
          :aria-label="'标为已读：' + notice.postTitle + ' · ' + descriptions[notice.kind]"
          :disabled="busy" @click="emit('markRead', notice.id)">标为已读</button>
        <span v-else-if="!notice.read">未读</span>
      </footer>
    </li>
  </ul>
</template>

<style scoped lang="scss">
.inbox-summary, footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.25rem 0.5rem;
  color: var(--paper-muted);
  font-size: 0.84em;
}

.inbox-summary { margin-bottom: 0.45rem; }
.inbox-stack { display: grid; gap: 0.52rem; list-style: none; margin: 0; padding: 0; }

li {
  padding: 0.56rem 0.68rem;
  border: 1px solid #9c744d66;
  border-radius: 0.22rem;
  background: var(--paper-card);
  overflow-wrap: anywhere;
}

li.unread { border-left: 3px solid #764257; background: #fff0c49e; }
header { display: flex; align-items: center; gap: 0.48rem; margin-bottom: 0.2rem; font-size: 0.86em; }
header p { margin: 0; color: var(--paper-ink); }
h4 { margin-top: 0.2rem; }
h4 a { color: var(--pine); text-decoration: none; }
h4 a:hover { text-decoration: underline; text-underline-offset: 0.16em; }
.inbox-excerpt { margin: 0.24rem 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

button {
  min-height: 2.25rem;
  padding: 0.22rem 0.55rem;
  font: inherit;
  color: var(--paper-ink);
  border: 1px solid #956d68;
  border-radius: 0.2rem;
  background: #f5e0b7;
  cursor: pointer;

  &:hover:enabled { background: #e8c796; }
  &:disabled { opacity: 0.65; cursor: default; }
}
</style>
