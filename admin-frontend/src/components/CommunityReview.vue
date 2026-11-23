<script setup lang="ts">
import { ref, shallowRef, watch } from 'vue'
import { publicUrl } from '../router'
import { accountError, needsLogin } from '../api/account'
import { dateLabel, listReports, review, type Page, type Report, type ReviewAction } from '../api/community'
import { useAccountStore } from '../stores/account'

const account=useAccountStore()
const result=shallowRef<Page<Report> | null>(null)
const page=ref(1)
const refresh=ref(0)
const loading=ref(false)
const saving=ref(false)
const error=ref('')
const notice=ref('')
const confirm=shallowRef<{id:number; action:ReviewAction} | null>(null)
const actionNames={HIDE:'隐藏内容',DELETE:'删除内容',DISMISS:'驳回举报'} as const
void account.restore()

watch([() => account.profile?.accountRole, page, refresh], async ([role,current], _, onCleanup) => {
  const request=new AbortController()
  onCleanup(() => request.abort())
  result.value=null
  loading.value=role==='ADMIN'
  if (role!=='ADMIN') return
  error.value=''
  try {
    const data=await listReports(current,request.signal)
    if (request.signal.aborted) return
    const last=Math.max(1,data.totalPages)
    if (current>last) page.value=last
    else result.value=data
  } catch (failure) {
    if (!request.signal.aborted) error.value=accountError(failure)
  } finally { if (!request.signal.aborted) loading.value=false }
}, { immediate:true })

async function resolveReport() {
  if (!confirm.value || saving.value) return
  saving.value=true
  error.value=''
  notice.value=''
  try {
    await review(confirm.value.id,confirm.value.action)
    confirm.value=null
    notice.value='举报已处理。'
    refresh.value++
  } catch (failure) {
    if (needsLogin(failure)) account.signedOut()
    error.value=accountError(failure)
  } finally { saving.value=false }
}
</script>

<template>
  <section class="admin-panel review">
      <RouterLink to="/">‹ 返回管理列表</RouterLink>
      <h1>举报审核</h1>
      <p v-if="loading" role="status">正在调阅举报…</p>
      <template v-else>
        <p v-if="error" class="forum-error" role="alert">{{ error }} <button type="button" @click="refresh++">刷新举报</button></p>
        <p v-if="notice" role="status">{{ notice }}</p>
        <ol v-if="result?.items.length">
          <li v-for="item in result.items" :key="item.id">
            <h2>{{ item.targetType==='POST'?'帖子':'评论' }}举报</h2>
            <p class="forum-meta">{{ item.reporter }} · {{ dateLabel(item.createdAt) }}</p>
            <blockquote class="forum-body">{{ item.content }}</blockquote>
            <p class="forum-body"><strong>举报理由</strong> {{ item.reason }}</p>
            <a :href="publicUrl('/community/posts/' + item.postId)">查阅讨论 ›</a>
            <div class="forum-commands review-actions">
              <button v-for="(label,action) in actionNames" :key="action" type="button" :disabled="saving"
                @click="confirm = {id:item.id,action}">{{ label }}</button>
            </div>
            <div v-if="confirm?.id===item.id" class="forum-commands review-actions">
              <span>确定{{ actionNames[confirm.action] }}？</span>
              <button type="button" class="forum-danger" :disabled="saving" @click="resolveReport">确认处理</button>
              <button type="button" :disabled="saving" @click="confirm = null">取消</button>
            </div>
          </li>
        </ol>
        <p v-else-if="result">没有待处理的举报。</p>
        <nav v-if="result && result.totalPages>1" class="forum-pages" aria-label="审核分页">
          <button type="button" :disabled="page<=1 || saving" @click="page--">上一页</button>
          <span>{{ page }} / {{ result.totalPages }}</span>
          <button type="button" :disabled="page>=result.totalPages || saving" @click="page++">下一页</button>
        </nav>
      </template>
    </section>
</template>

<style scoped lang="scss">
.review ol { list-style:none; margin:0; padding:0; }
.review li { padding:1rem 0; border-bottom:1px solid var(--forum-line); }
.review blockquote { margin:.8rem 0; padding:.55rem .75rem; background:#f6e3b77d; border-left:.2rem solid #8a6742; }
.review-actions { margin-top:.65rem; }
</style>
