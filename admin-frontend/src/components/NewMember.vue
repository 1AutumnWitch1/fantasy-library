<script setup lang="ts">
import { ref } from 'vue'
import { accountError } from '../api/account'
import { addAdminMember } from '../api/admin'

const emit = defineEmits<{ created: [username: string]; cancel: [] }>()
const username = ref('')
const displayName = ref('')
const password = ref('')
const busy = ref(false)
const error = ref('')

async function submit() {
  if (busy.value) return
  if (new TextEncoder().encode(password.value).length > 72) {
    error.value = '密码最多为 72 个 UTF-8 字节。'
    return
  }
  busy.value = true
  error.value = ''
  try {
    await addAdminMember({ username: username.value.trim(), displayName: displayName.value.trim(), password: password.value })
    password.value = ''
    emit('created', username.value.trim())
  } catch (reason) {
    error.value = accountError(reason)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="member-form" aria-label="新增用户" @submit.prevent="submit">
    <h2>新增用户</h2>
    <div class="member-fields">
      <label>账号名<input v-model="username" required pattern="[A-Za-z0-9_]{3,40}" maxlength="40"
        placeholder="3–40 位字母、数字或下划线" :disabled="busy"></label>
      <label>昵称<input v-model="displayName" required maxlength="60" :disabled="busy"></label>
      <label>初始密码<input v-model="password" type="password" autocomplete="new-password"
        required minlength="8" maxlength="72" :disabled="busy"></label>
    </div>
    <p v-if="error" role="alert">{{ error }}</p>
    <div class="member-actions">
      <button type="submit" :disabled="busy">{{ busy ? '正在创建…' : '创建普通用户' }}</button>
      <button type="button" :disabled="busy" @click="emit('cancel')">取消</button>
    </div>
  </form>
</template>

<style scoped lang="scss">
.member-form { margin: .6rem 0 .85rem; padding:.75rem; border:1px solid var(--paper-line); background:#f8e9c38a; }
h2 { margin:0 0 .45rem; font-size:1.15rem; }
.member-fields { display:grid; grid-template-columns:repeat(auto-fit,minmax(min(100%,13rem),1fr)); gap:.65rem; }
.member-fields label { display:grid; gap:.2rem; min-width:0; }
.member-actions { display:flex; flex-wrap:wrap; gap:.5rem; margin-top:.65rem; }
</style>
