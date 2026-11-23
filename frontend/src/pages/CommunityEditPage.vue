<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { accountError, needsLogin } from '../api/account'
import { communityLink, edit, getPost, isTopic, mediaUrl, publish, readId, topics, type Topic } from '../api/community'
import { getWork } from '../api/catalog'
import { useAccountStore } from '../stores/account'
import ForumRoom from '../components/community/ForumRoom.vue'

const route=useRoute()
const router=useRouter()
const account=useAccountStore()
const id=computed(() => readId(route.params.id))
const editing=computed(() => route.name==='community-edit')
const contextWork=computed(() => readId(route.query.workId))
const back=computed(() => communityLink(contextWork.value, isTopic(route.query.topic) ? route.query.topic : undefined))
const title=ref('')
const body=ref('')
const topic=ref<Topic>('GENERAL')
const work=ref<{id:number; title:string} | null>(null)
const keep=ref<string[]>([])
const files=ref<{file:File; preview:string}[]>([])
const loading=ref(false)
const busy=ref(false)
const loadError=ref('')
const error=ref('')
const attempt=ref(0)

void account.restore()

function clearFiles() {
  files.value.forEach(item=>URL.revokeObjectURL(item.preview))
  files.value=[]
}
onBeforeUnmount(clearFiles)

watch([id, editing, contextWork, () => account.profile?.id, attempt], async ([postId, update, workId, user], _, onCleanup) => {
  const request=new AbortController()
  onCleanup(() => request.abort())
  clearFiles()
  keep.value=[]
  title.value=''
  body.value=''
  work.value=null
  topic.value='GENERAL'
  loadError.value=''
  error.value=''
  loading.value=!!user
  if (!user) return
  try {
    if (update) {
      if (!postId) { loadError.value='帖子地址无效'; return }
      const result=await getPost(postId, request.signal)
      if (request.signal.aborted) return
      if (result.post.authorId!==user) { loadError.value='只有作者可以编辑这篇手稿。'; return }
      title.value=result.post.title
      body.value=result.post.body
      topic.value=result.post.topic
      keep.value=result.images
      if (result.post.workId && result.post.workTitle) work.value={id:result.post.workId,title:result.post.workTitle}
    } else if (workId) {
      const result=await getWork(String(workId),request.signal)
      if (request.signal.aborted) return
      work.value={id:workId,title:result.work.title}
      topic.value=result.work.type==='GAME'?'GAME':'ANIME'
    } else if (isTopic(route.query.topic)) {
      topic.value=route.query.topic
    }
  } catch (failure) {
    if (!request.signal.aborted) loadError.value=accountError(failure)
  } finally { if (!request.signal.aborted) loading.value=false }
}, { immediate:true })

function selectImages(event: Event) {
  const input=event.target as HTMLInputElement
  const selected=Array.from(input.files ?? [])
  input.value=''
  error.value=''
  if (selected.length + keep.value.length + files.value.length > 4) {
    error.value='每帖最多可附 4 张图片。'
    return
  }
  if (selected.some(file=>!['image/jpeg','image/png'].includes(file.type) || file.size>5*1024*1024)) {
    error.value='请选择不超过 5 MB 的 PNG 或 JPEG 图片。'
    return
  }
  files.value.push(...selected.map(file=>({file,preview:URL.createObjectURL(file)})))
}

function removeNew(index:number) {
  URL.revokeObjectURL(files.value[index].preview)
  files.value.splice(index,1)
}

async function submit() {
  if (busy.value) return
  if (!title.value.trim() || !body.value.trim()) { error.value='请输入标题和正文。'; return }
  error.value=''
  busy.value=true
  try {
    const draft={title:title.value.trim(),body:body.value.trim(),topic:topic.value,workId:work.value?.id ?? null,keepImages:keep.value}
    const uploads=files.value.map(item=>item.file)
    let saved=id.value
    if (editing.value && saved) await edit(saved,draft,uploads)
    else saved=(await publish(draft,uploads)).id
    await router.replace({name:'community-post',params:{id:saved},query:route.query})
  } catch (failure) {
    if (needsLogin(failure)) account.signedOut()
    error.value=accountError(failure)
  } finally { busy.value=false }
}
</script>

<template>
  <ForumRoom :back="back">
    <section class="forum-sheet">
      <h1>{{ editing ? '修订手稿' : '撰写手稿' }}</h1>
      <p v-if="account.status==='error'" class="forum-error" role="alert">{{ account.error }} <button type="button" @click="account.restore(true)">重试</button></p>
      <p v-else-if="account.status==='loading' || loading" role="status">正在准备手稿…</p>
      <p v-else-if="!account.profile"><RouterLink :to="{name:'account',query:{next:route.fullPath}}">登录后继续 ›</RouterLink></p>
      <p v-else-if="loadError" class="forum-error" role="alert">{{ loadError }} <button type="button" @click="attempt++">重试</button></p>
      <form v-else class="forum-form" @submit.prevent="submit">
        <label>标题<input v-model="title" required maxlength="100" :disabled="busy"></label>
        <label>话题分类<select v-model="topic" :disabled="busy">
          <option v-for="(label,value) in topics" :key="value" :value="value">{{ label }}</option>
        </select></label>
        <div v-if="work" class="forum-commands">
          <span>关联作品《{{ work.title }}》</span>
          <button type="button" :disabled="busy" @click="work = null">取消关联</button>
        </div>
        <label>正文<textarea v-model="body" required rows="9" maxlength="6000" :disabled="busy" /></label>
        <label>附图<input type="file" multiple accept="image/png,image/jpeg" :disabled="busy" @change="selectImages">
          <small>最多 4 张 PNG 或 JPEG，每张最多 5 MB。</small>
        </label>
        <div v-if="keep.length || files.length" class="forum-images">
          <figure v-for="(key,i) in keep" :key="key">
            <img :src="mediaUrl(key)" :alt="'已有图片 ' + (i+1)">
            <button type="button" :disabled="busy" @click="keep.splice(i,1)">移除已有图片 {{ i+1 }}</button>
          </figure>
          <figure v-for="(item,i) in files" :key="item.preview">
            <img :src="item.preview" :alt="'待上传图片 ' + (i+1)">
            <button type="button" :disabled="busy" @click="removeNew(i)">移除新图片 {{ i+1 }}</button>
          </figure>
        </div>
        <p v-if="error" class="forum-error forum-message" role="alert">{{ error }}</p>
        <div class="forum-commands">
          <button type="submit" class="forum-main-button" :disabled="busy">{{ editing ? '保存手稿' : '发布手稿' }}</button>
          <RouterLink class="forum-button" :to="back">取消</RouterLink>
        </div>
      </form>
    </section>
  </ForumRoom>
</template>
