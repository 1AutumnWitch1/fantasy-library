<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { accountError, needsLogin } from '../api/account'
import { communityLink, dateLabel, deletePost, getPost, getReaction, isTopic, mediaUrl,
  readId, setReaction, topics, type PostDetail, type Reaction } from '../api/community'
import { useAccountStore } from '../stores/account'
import AuthorAvatar from '../components/community/AuthorAvatar.vue'
import ForumRoom from '../components/community/ForumRoom.vue'
import PostComments from '../components/community/PostComments.vue'
import ReportContent from '../components/community/ReportContent.vue'

const route=useRoute()
const router=useRouter()
const account=useAccountStore()
const id=computed(() => readId(route.params.id))
const previousBook = computed(() => route.query.from === 'personal' || route.query.from === 'recommendations' ? route.query.from : null)
const back = computed(() => previousBook.value
  ? { name: 'book', params: { bookId: previousBook.value } }
  : communityLink(readId(route.query.workId), isTopic(route.query.topic) ? route.query.topic : undefined))
const detail=shallowRef<PostDetail | null>(null)
const reaction=shallowRef<Reaction | null>(null)
const reload=ref(0)
const reactionReload=ref(0)
const loadingError=ref('')
const likeError=ref('')
const error=ref('')
const busy=ref(false)
const confirmDelete=ref(false)
const owns=computed(() => detail.value?.post.authorId === account.profile?.id && !!account.profile)
void account.restore()

watch([id, reload], async ([current], _, onCleanup) => {
  const request=new AbortController()
  onCleanup(() => request.abort())
  detail.value=null
  confirmDelete.value=false
  loadingError.value=''
  error.value=''
  if (!current) { loadingError.value='帖子地址无效'; return }
  try {
    const data=await getPost(current, request.signal, true)
    if (!request.signal.aborted) detail.value=data
  } catch (failure) {
    if (!request.signal.aborted) loadingError.value=accountError(failure)
  }
}, { immediate:true })

watch([id, () => account.profile?.id, reactionReload], async ([current,user], _, onCleanup) => {
  const request=new AbortController()
  onCleanup(() => request.abort())
  reaction.value=null
  likeError.value=''
  if (!current || !user) return
  try {
    const data=await getReaction(current, request.signal)
    if (!request.signal.aborted) reaction.value=data
  } catch (failure) {
    if (!request.signal.aborted) {
      if (needsLogin(failure)) account.signedOut()
      else likeError.value=accountError(failure)
    }
  }
}, { immediate:true })

async function like() {
  if (!id.value || !reaction.value || busy.value) return
  const current=id.value
  busy.value=true
  error.value=''
  try {
    const updated=await setReaction(current, !reaction.value.liked)
    if (id.value===current) reaction.value=updated
  } catch (failure) {
    if (needsLogin(failure)) account.signedOut()
    error.value=accountError(failure)
  } finally { busy.value=false }
}

async function erase() {
  if (!id.value || !owns.value || busy.value) return
  busy.value=true
  error.value=''
  try {
    await deletePost(id.value)
    await router.replace(back.value)
  } catch (failure) {
    if (needsLogin(failure)) account.signedOut()
    error.value=accountError(failure)
  } finally { busy.value=false }
}
</script>

<template>
  <ForumRoom :back="back" :back-label="previousBook === 'personal' ? '返回私人书架' : previousBook ? '返回推荐书' : undefined">
    <article v-if="detail" class="forum-sheet manuscript">
      <header>
        <p class="forum-topic">{{ topics[detail.post.topic] }}</p>
        <h1>{{ detail.post.title }}</h1>
        <div class="manuscript-author forum-meta">
          <AuthorAvatar :avatar-key="detail.post.avatarKey" />
          <span>{{ detail.post.authorName }} · {{ dateLabel(detail.post.createdAt) }} · {{ detail.post.viewCount }} 浏览</span>
        </div>
      </header>
      <p v-if="detail.post.workId" class="forum-meta">
        <RouterLink :to="{name:'work-detail', params:{id:detail.post.workId}}">关联作品《{{ detail.post.workTitle }}》 ›</RouterLink>
      </p>
      <hr class="forum-divider">
      <p class="forum-body">{{ detail.post.body }}</p>
      <div v-if="detail.images.length" class="forum-images">
        <a v-for="(key, i) in detail.images" :key="key" :href="mediaUrl(key)" target="_blank" rel="noopener">
          <img :src="mediaUrl(key)" :alt="'帖子插图 ' + (i+1)" loading="lazy" decoding="async">
        </a>
      </div>
      <hr class="forum-divider">
      <div class="forum-commands">
        <button v-if="account.profile" type="button" :aria-pressed="reaction?.liked ?? false" :disabled="busy || !reaction" @click="like">
          {{ reaction?.liked ? '取消点赞' : '点赞' }} · {{ reaction?.count ?? detail.post.likeCount }}
        </button>
        <RouterLink v-else class="forum-button" :to="{name:'account',query:{next:route.fullPath}}">登录后点赞 · {{ detail.post.likeCount }}</RouterLink>
        <RouterLink v-if="owns" class="forum-button" :to="{name:'community-edit',params:{id:detail.post.id},query:route.query}">编辑手稿</RouterLink>
        <button v-if="owns" type="button" :disabled="busy" @click="confirmDelete = !confirmDelete">删除帖子</button>
      </div>
      <p v-if="likeError" class="forum-error" role="alert">{{ likeError }} <button type="button" @click="reactionReload++">重试点赞状态</button></p>
      <div v-if="confirmDelete" class="forum-commands delete-confirm">
        <span>确定删除这篇帖子？</span>
        <button type="button" class="forum-danger" :disabled="busy" @click="erase">确认删除</button>
        <button type="button" :disabled="busy" @click="confirmDelete = false">取消</button>
      </div>
      <p v-if="error" class="forum-error" role="alert">{{ error }}</p>
      <ReportContent v-if="account.profile" :key="detail.post.id" target="POST" :id="detail.post.id" />
    </article>
    <section v-else class="forum-sheet">
      <p v-if="loadingError" class="forum-error" role="alert">{{ loadingError }} <button type="button" @click="reload++">重试</button></p>
      <p v-else role="status">正在调阅手稿…</p>
    </section>
    <PostComments v-if="detail" :key="detail.post.id" :thread-id="detail.post.id" />
  </ForumRoom>
</template>

<style scoped lang="scss">
.manuscript-author { display:flex; align-items:center; gap:.45rem; overflow-wrap:anywhere; }
.manuscript .forum-body { margin-block:1rem; }
.delete-confirm { margin-top:.65rem; }
</style>
