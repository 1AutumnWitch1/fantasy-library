<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { accountError } from './api/account'
import { publicUrl } from './router'
import { useAccountStore } from './stores/account'

const account = useAccountStore()
const router = useRouter()
const username = ref('')
const password = ref('')
const busy = ref(false)
const error = ref('')
void account.restore()

async function submit() {
  if (busy.value) return
  error.value = ''
  busy.value = true
  try {
    await account.signIn({ username: username.value.trim(), password: password.value })
    password.value = ''
  } catch (failure) {
    error.value = account.error || accountError(failure)
  } finally {
    busy.value = false
  }
}

async function exit() {
  busy.value = true
  error.value = ''
  try {
    await account.signOut()
    await router.replace('/')
  } catch (failure) {
    error.value = accountError(failure)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <header class="admin-header">
    <RouterLink class="admin-brand" to="/"><span aria-hidden="true">✧</span> 馆务管理</RouterLink>
    <div class="account-tools">
      <a :href="publicUrl()">访问馆藏 ↗</a>
      <template v-if="account.profile">
        <span class="account-name">{{ account.profile.displayName }}</span>
        <button type="button" :disabled="busy" @click="exit">退出登录</button>
      </template>
    </div>
  </header>

  <main class="admin-main">
    <p v-if="error && account.profile" class="forum-error" role="alert">{{ error }}</p>
    <RouterView v-if="account.profile?.accountRole === 'ADMIN'" />
    <section v-else class="admin-panel admin-login" aria-labelledby="login-heading">
      <h1 id="login-heading">管理员登录</h1>
      <p v-if="account.status === 'loading'" role="status">正在查验登录状态…</p>
      <p v-else-if="account.profile">此账号没有管理员权限。退出后可以切换账号。</p>
      <template v-else>
        <p v-if="error || account.error" role="alert" class="forum-error">{{ error || account.error }}</p>
        <form class="login-form" @submit.prevent="submit">
          <label>账号名<input v-model="username" autocomplete="username" required maxlength="40" :disabled="busy"></label>
          <label>密码<input v-model="password" type="password" autocomplete="current-password" required maxlength="72" :disabled="busy"></label>
          <button type="submit" class="primary" :disabled="busy">{{ busy ? '正在登录…' : '登录' }}</button>
        </form>
      </template>
    </section>
  </main>
</template>
