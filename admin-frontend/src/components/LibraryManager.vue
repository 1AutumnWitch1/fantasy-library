<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'
import { accountError } from '../api/account'
import { listAdminRows, removeAdminRow, setItemListed, type AdminRow, type AdminTab, type CommentScope, type WorkType } from '../api/admin'
import { deleteWork } from '../api/editorial'
import type { Page } from '../api/community'
import AdminNodes from './AdminNodes.vue'
import ForumControl from './ForumControl.vue'
import NewMember from './NewMember.vue'

const tabs: { id: AdminTab; label: string; search: string }[] = [
  { id: 'works', label: '馆藏管理', search: '搜索作品名称' },
  { id: 'posts', label: '社区帖子', search: '搜索标题或作者' },
  { id: 'comments', label: '评论管理', search: '搜索评论、作者、番剧或帖子' },
  { id: 'members', label: '用户', search: '搜索账号或昵称' },
]

const route = useRoute()
const tab = ref<AdminTab>('works')
const type = ref<WorkType>(route.query.type === 'GAME' ? 'GAME' : 'ANIME')
const commentScope = ref<'all' | CommentScope>('all')
const publication = ref<'all' | 'listed' | 'unlisted'>('all')
const draft = ref('')
const query = ref('')
const page = ref(1)
const refresh = ref(0)
const result = shallowRef<Page<AdminRow>>()
const loading = ref(true)
const failure = ref('')
const notice = ref('')
const error = ref('')
const busy = ref(false)
const adding = ref(false)
const expanded = ref<number>()
const confirm = ref<number>()
const current = computed(() => tabs.find(item => item.id === tab.value)!)
const pages = computed(() => Math.max(1, result.value?.totalPages ?? 1))

watch([tab, type, query, page, refresh, publication, commentScope], async ([view, workType, search, index, , state, scope], _, onCleanup) => {
  const ticket = new AbortController()
  onCleanup(() => ticket.abort())
  expanded.value = undefined
  confirm.value = undefined
  result.value = undefined
  loading.value = true
  failure.value = ''
  try {
    const data = await listAdminRows(view, workType, search, index, ticket.signal, state === 'all' ? undefined : state === 'listed', scope === 'all' ? undefined : scope)
    if (ticket.signal.aborted) return
    if (index > Math.max(data.totalPages, 1)) page.value = Math.max(data.totalPages, 1)
    else result.value = data
  } catch (reason) {
    if (!ticket.signal.aborted) failure.value = accountError(reason)
  } finally {
    if (!ticket.signal.aborted) loading.value = false
  }
}, { immediate: true })

function select(next: AdminTab) {
  if (tab.value === next) return
  tab.value = next
  adding.value = false
  page.value = 1
  query.value = ''
  draft.value = ''
  error.value = ''
  notice.value = ''
}

function search() {
  page.value = 1
  query.value = draft.value.trim()
  error.value = ''
  notice.value = ''
}

function memberAdded(username: string) {
  adding.value = false
  if (tab.value !== 'members') return
  draft.value = username
  search()
  refresh.value++
  notice.value = '用户已创建。'
}

async function change(row: AdminRow, permanent = false) {
  if (busy.value || loading.value) return
  const view = tab.value
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    if (view === 'works' && permanent) {
      const removed = await deleteWork(row.id)
      refresh.value++
      const queued = removed.queuedFiles ? ' ' + removed.queuedFiles + ' 个站内文件已加入清理队列。' : ''
      const shared = removed.sharedFiles ? ' ' + removed.sharedFiles + ' 个共享文件已保留。' : ''
      notice.value = '《' + row.title + '》及其关联记录已删除。' + queued + shared
    } else if (view === 'works' || view === 'posts') {
      await setItemListed(view, row.id, !row.listed)
      if (result.value) {
        result.value = { ...result.value,
          items: result.value.items.map(item => item.id === row.id ? { ...item, listed: !row.listed } : item) }
      }
      if (view === 'works' && publication.value !== 'all') refresh.value++
      notice.value = row.listed ? '已下架。' : '已上架。'
    } else {
      await removeAdminRow(view, row.id)
      refresh.value++
      notice.value = view === 'comments' ? '评论已移除。' : '账号已注销。'
    }
  } catch (reason) {
    error.value = accountError(reason)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="admin-panel">
  <h1>{{ current.label }}</h1>
  <nav class="manager-nav" aria-label="管理项目">
    <button v-for="item in tabs" :key="item.id" type="button" :disabled="busy"
      :aria-pressed="tab === item.id" @click="select(item.id)">{{ item.label }}</button>
    <RouterLink v-if="tab === 'posts'" :to="{ name: 'community-review' }">举报审核 ›</RouterLink>
  </nav>

  <ForumControl v-if="tab === 'posts'" />

  <div class="manager-toolbar">
    <label v-if="tab === 'works'" class="manager-type">
      <span>作品类型</span>
      <select v-model="type" :disabled="busy" @change="page = 1; error = ''; notice = ''">
        <option value="ANIME">番剧</option>
        <option value="GAME">游戏</option>
      </select>
    </label>
    <label v-if="tab === 'works'" class="manager-type">
      <span>上架状态</span>
      <select v-model="publication" :disabled="busy" @change="page = 1">
        <option value="all">全部</option>
        <option value="listed">已上架</option>
        <option value="unlisted">已下架</option>
      </select>
    </label>
    <label v-if="tab === 'comments'" class="manager-type">
      <span>评论来源</span>
      <select v-model="commentScope" :disabled="busy" @change="page = 1; error = ''; notice = ''">
        <option value="all">全部评论</option>
        <option value="SCREENING">番剧评论</option>
        <option value="COMMUNITY">社区评论</option>
      </select>
    </label>
    <form class="manager-search" role="search" @submit.prevent="search">
      <input v-model="draft" type="search" maxlength="80" :placeholder="current.search" :aria-label="current.search"
        :disabled="busy" @search="!draft && search()">
      <button type="submit" :disabled="busy">搜索</button>
    </form>
    <RouterLink v-if="tab === 'works'" class="button-link primary" :to="{ name: 'work-new', query: { type } }">新增作品</RouterLink>
    <button v-if="tab === 'members' && !adding" type="button" :disabled="busy" @click="adding = true">新增用户</button>
  </div>

  <NewMember v-if="tab === 'members' && adding" @created="memberAdded" @cancel="adding = false" />

  <p v-if="error" class="manager-error" role="alert">{{ error }}</p>
  <p v-if="notice" class="manager-notice" role="status">{{ notice }}</p>
  <p v-if="loading" role="status">正在调阅档案…</p>
  <p v-else-if="failure" role="alert">{{ failure }} <button type="button" @click="refresh++">重试</button></p>
  <p v-else-if="!result?.items.length">没有找到相关记录。</p>

  <ul v-else class="manager-records" :aria-label="current.label">
    <li v-for="row in result.items" :key="row.id">
      <div class="manager-row">
        <div v-if="tab === 'works'" class="manager-cover">
          <img v-if="row.coverUrl" :src="row.coverUrl" alt="" loading="lazy" referrerpolicy="no-referrer"
            @error="(event) => (event.target as HTMLImageElement).hidden = true">
          <span v-else>无封面</span>
        </div>
        <div class="manager-summary">
          <h2>{{ row.title }}</h2>
          <p v-if="row.body" class="manager-body">{{ row.body }}</p>
          <p class="manager-caption">{{ row.caption }}<template v-if="row.slug"> · {{ row.slug }}</template></p>
        </div>
        <div v-if="typeof row.listed === 'boolean'" class="manager-actions">
          <span class="manager-label" :class="{ off: !row.listed }">{{ row.listed ? '已上架' : '已下架' }}</span>
          <RouterLink v-if="tab === 'works'" class="button-link" :to="{ name: 'work-edit', params: { id: row.id } }">编辑资料</RouterLink>
          <RouterLink v-if="tab === 'works'" class="button-link"
            :to="{ name: 'work-edit', params: { id: row.id }, hash: '#work-resources' }">管理节点</RouterLink>
          <button v-if="tab === 'works'" type="button" :aria-expanded="expanded === row.id"
            @click="expanded = expanded === row.id ? undefined : row.id">{{ expanded === row.id ? '收起状态' : '节点状态' }}</button>
          <button type="button" :disabled="busy" @click="change(row)">{{ row.listed ? '下架' : '上架' }}</button>
          <button v-if="tab === 'works' && confirm !== row.id" type="button" class="forum-danger" :disabled="busy"
            :aria-label="'删除作品 ' + row.title" @click="confirm = row.id">删除作品</button>
        </div>
        <span v-else-if="row.protected" class="manager-label">管理员</span>
        <button v-else-if="confirm !== row.id" type="button" class="forum-danger" :disabled="busy"
          @click="confirm = row.id">{{ tab === 'members' ? '删除账号' : '删除评论' }}</button>
      </div>
      <div v-if="confirm === row.id" class="manager-confirm">
        <template v-if="tab === 'works'">
          <p><strong>永久删除《{{ row.title }}》？</strong> 作品、关联的媒体记录、收藏和观看历史都会删除。</p>
          <p>独占的 MinIO 文件会自动清理。相关帖子和评论会保留，帖子会解除与本作品的关联。</p>
        </template>
        <p v-else>{{ tab === 'members' ? '注销后会撤销登录，清除收藏、历史及头像，已有发言会匿名保留。' : '这条评论将从公开评论区移除，其他人的回复会保留。' }}</p>
        <div class="manager-actions">
          <button type="button" class="forum-danger" :disabled="busy" @click="change(row, tab === 'works')">
            {{ tab === 'works' ? '确认永久删除' : '确认删除' }}
          </button>
          <button type="button" :disabled="busy" @click="confirm = undefined">取消</button>
        </div>
      </div>
      <AdminNodes v-if="tab === 'works' && expanded === row.id" :work-id="row.id" />
    </li>
  </ul>

  <nav v-if="pages > 1" class="manager-pagination" aria-label="管理列表分页">
    <button type="button" :disabled="page <= 1 || busy" @click="page--">上一页</button>
    <span>第 {{ page }} / {{ pages }} 页</span>
    <button type="button" :disabled="page >= pages || busy" @click="page++">下一页</button>
  </nav>
  </section>
</template>

<style scoped lang="scss">
.manager-nav, .manager-toolbar, .manager-type, .manager-search, .manager-actions, .manager-pagination {
  display:flex; flex-wrap:wrap; align-items:center; gap:.5rem;
}
.manager-nav { padding-bottom:.75rem; border-bottom:1px solid var(--paper-line); }
.manager-nav button[aria-pressed="true"] { color:#fff0c6; background:#304536; }
.manager-nav a { margin-left:auto; }
.manager-toolbar { margin:.85rem 0; gap:.65rem .8rem; }
.manager-search { flex:1 1 16rem; margin-left:auto; max-width:24rem; flex-wrap:nowrap; }
.manager-search input { min-width:0; flex:1; }
.manager-search button { flex:none; }
.manager-type { flex-wrap:nowrap; }
.manager-toolbar select { width:auto; color:inherit; font:inherit; background:#fae7bd; border:1px solid var(--paper-line); padding:.35rem .4rem; }
.manager-records { margin:0; padding:0; list-style:none; border-top:1px solid var(--paper-line); }
.manager-records > li { padding:.7rem 0; border-bottom:1px solid var(--paper-line); }
.manager-row { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:.45rem .85rem; }
.manager-cover { width:3.2rem; height:4.05rem; flex:none; display:grid; place-items:center; overflow:hidden;
  border:1px solid #c8cabd; border-radius:.25rem; background:#eeece0; color:#5c6357; font-size:.64rem; }
.manager-cover img { width:100%; height:100%; object-fit:cover; }
.manager-summary { min-width:0; flex:1 1 15rem; overflow-wrap:anywhere; }
.manager-summary h2 { margin:0; font-size:1.05rem; }
.manager-body { margin:.25rem 0; white-space:pre-wrap; }
.manager-caption { margin:.23rem 0 0; color:#64482d; font-size:.86rem; }
.manager-label { color:#294536; font-size:.86rem; }
.manager-label.off, .manager-error { color:#7d3025; }
.manager-notice { color:#294536; }
.manager-confirm { margin:.5rem 0 0; padding:.5rem .75rem; border:1px solid var(--paper-line); background:#eed49e8a; }
.manager-confirm p { margin:.25rem 0 .45rem; }
.manager-pagination { margin-top:.8rem; justify-content:center; }
@media(max-width:680px) {
  .manager-summary { flex-basis:calc(100% - 4.05rem); }
  .manager-row { gap:.55rem; }
  .manager-search { margin-inline:0; max-width:none; }
}
</style>
