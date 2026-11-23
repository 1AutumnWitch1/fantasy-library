<script setup lang="ts">
import { onScopeDispose, ref } from 'vue'
import { accountError } from '../api/account'
import { getForum, setForumOpen } from '../api/admin'

const open = ref<boolean | null>(null)
const loading = ref(false)
const pending = ref(false)
const confirming = ref(false)
const error = ref('')
let active: AbortController | undefined

async function load() {
  active?.abort()
  const ticket = new AbortController()
  active = ticket
  error.value = ''
  loading.value = true
  try {
    const result = await getForum(ticket.signal)
    if (!ticket.signal.aborted) open.value = result.open
  } catch (failure) {
    if (!ticket.signal.aborted) error.value = accountError(failure)
  } finally {
    if (!ticket.signal.aborted) loading.value = false
  }
}

async function change() {
  if (pending.value || open.value === null) return
  const desired = !open.value
  pending.value = true
  error.value = ''
  try {
    await setForumOpen(desired)
    open.value = desired
    confirming.value = false
  } catch (failure) {
    error.value = accountError(failure)
  } finally {
    pending.value = false
  }
}

void load()
onScopeDispose(() => active?.abort())
</script>

<template>
  <section class="admin-forum" aria-label="论坛开关">
    <div>
      <h2>社区论坛</h2>
      <p role="status">{{ open === null ? (loading ? '正在读取开放状态…' : '暂时无法读取状态') : open ? '公开开放' : '已暂停开放' }}</p>
    </div>
    <button v-if="open === true && !confirming" type="button" @click="confirming = true">下架论坛</button>
    <button v-if="open === false" type="button" :disabled="pending" @click="change">上架论坛</button>
    <div v-if="confirming" class="admin-confirm">
      <span>暂停后，馆友将暂时无法浏览或发帖。</span>
      <button type="button" class="forum-danger" :disabled="pending" @click="change">确认下架论坛</button>
      <button type="button" :disabled="pending" @click="confirming = false">取消</button>
    </div>
    <p v-if="error" role="alert" class="forum-error">{{ error }}
      <button v-if="open === null" type="button" @click="load">重试</button>
    </p>
  </section>
</template>

<style scoped lang="scss">
.admin-forum { display:flex; align-items:center; flex-wrap:wrap; justify-content:space-between;
  gap:.65rem; margin: .7rem 0; padding:.68rem .8rem; border:1px solid var(--paper-line); background:#f8e9c38a; }
.admin-forum h2 { font-size:1.15rem; }
p { margin:.12rem 0 0; font-size:.86rem; }
.admin-confirm { flex-basis:100%; display:flex; align-items:center; gap:.42rem; flex-wrap:wrap; }
</style>
