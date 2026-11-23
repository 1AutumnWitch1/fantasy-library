<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { accountError, needsLogin, register } from '../api/account'
import { accountReturnPath } from '../router/accountReturn'
import { useAccountStore } from '../stores/account'
import ProfileAvatarEditor from '../components/account/ProfileAvatarEditor.vue'
import BookSigil from '../components/shelf/BookSigil.vue'

const route = useRoute()
const router = useRouter()
const account = useAccountStore()
const creating = ref(false)
const username = ref('')
const password = ref('')
const displayName = ref('')
const bio = ref('')
const busy = ref(false)
const error = ref('')
const notice = ref('')
const previous = router.options.history.state.back
const destination = computed(() => accountReturnPath(router, route.query.next, previous))
const title = computed(() => account.profile ? '个人资料' : creating.value ? '创建藏书账号' : '登录图书馆')

onMounted(() => window.scrollTo(0, 0))

watch([() => account.profile?.id, () => account.profile?.displayName, () => account.profile?.bio], () => {
  displayName.value = account.profile?.displayName ?? ''
  bio.value = account.profile?.bio ?? ''
}, { immediate: true })
void account.restore()

function switchMode() {
  creating.value = !creating.value
  password.value = ''
  error.value = ''
  notice.value = ''
}

function validateCredentials(loginName: string, secret: string, nickname: string) {
  if (creating.value && !nickname) return '请输入昵称，昵称可以用中文。'
  if (!loginName) return '请输入账号名。'
  if (creating.value && !/^[A-Za-z0-9_]{3,40}$/.test(loginName)) {
    return '账号名须为 3–40 位英文字母、数字或下划线，例如 momo2026。中文可填在昵称里。'
  }
  if (!secret.trim()) return '请输入密码。'
  if (creating.value && secret.length < 8) return '密码至少需要 8 个字符。'
  if (secret.includes('\0') || new TextEncoder().encode(secret).length > 72) {
    return '密码最多 72 个 UTF-8 字节，且不能包含空字符。'
  }
  return ''
}

async function submit() {
  if (busy.value) return
  const credentials = { username: username.value.trim(), password: password.value }
  const nickname = displayName.value.trim()
  notice.value = ''
  error.value = validateCredentials(credentials.username, credentials.password, nickname)
  if (error.value) return
  const loginPage = route.fullPath
  const returnPath = destination.value
  busy.value = true
  try {
    if (creating.value) {
      await register({ ...credentials, displayName: nickname })
      creating.value = false
      notice.value = '账号已创建，请登录。'
    } else {
      await account.signIn(credentials)
      if (route.fullPath === loginPage) {
        if (router.options.history.state.back === returnPath) router.back()
        else await router.replace(returnPath)
      }
    }
    password.value = ''
  } catch (reason) {
    error.value = account.status === 'error' ? account.error : accountError(reason)
  } finally {
    busy.value = false
  }
}

async function update() {
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await account.saveProfile({ displayName: displayName.value.trim(), bio: bio.value.trim() })
    notice.value = '资料已保存。'
  } catch (reason) {
    if (needsLogin(reason)) account.signedOut()
    error.value = accountError(reason)
  } finally {
    busy.value = false
  }
}

async function signOut() {
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await account.signOut()
    username.value = ''
    password.value = ''
    notice.value = '已退出登录。'
  } catch (reason) {
    error.value = accountError(reason)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main class="account-room">
    <section class="account-ledger" aria-labelledby="account-title">
      <div class="ledger-art">
        <RouterLink class="ledger-back" :to="destination">
          <span aria-hidden="true">‹</span> {{ destination === '/' ? '返回馆藏' : '返回上一页' }}
        </RouterLink>
      </div>
      <div class="ledger-paper">
        <div class="ledger-content">
          <header class="account-heading">
            <span class="registry-seal" aria-hidden="true"><BookSigil kind="key" /></span>
            <h1 id="account-title">{{ title }}</h1>
            <p v-if="account.profile" class="account-context">{{ account.profile.username }}</p>
          </header>

          <p v-if="error || account.status === 'error'" class="account-message account-message--error" role="alert">
            {{ error || account.error }}
          </p>
          <p v-if="notice" class="account-message" role="status">{{ notice }}</p>
          <p v-if="account.status === 'loading' && !busy" class="account-message" role="status">正在查验账号…</p>
          <template v-else-if="account.profile">
            <ProfileAvatarEditor />
            <form class="account-form" @submit.prevent="update">
              <div class="form-field">
                <label for="profile-name">昵称</label>
                <input id="profile-name" v-model="displayName" required maxlength="60" :disabled="busy">
              </div>
              <div class="form-field">
                <label for="profile-bio">个人简介</label>
                <textarea id="profile-bio" v-model="bio" rows="4" maxlength="240" :disabled="busy"></textarea>
              </div>
              <div class="account-actions">
                <button type="submit" class="account-primary" :disabled="busy">保存资料</button>
                <button type="button" class="account-secondary" :disabled="busy" @click="signOut">退出登录</button>
              </div>
            </form>
            <div class="account-actions">
              <RouterLink class="account-link" :to="{ name: 'book', params: { bookId: 'personal' } }">打开私人书架 ›</RouterLink>
            </div>
          </template>
          <template v-else>
            <form class="account-form" novalidate @submit.prevent="submit">
              <div v-if="creating" class="form-field">
                <label for="register-name">昵称</label>
                <input id="register-name" v-model="displayName" placeholder="你在馆中的称呼" required
                  maxlength="60" autocomplete="nickname" aria-describedby="nickname-hint" :disabled="busy">
                <small id="nickname-hint">可以填写中文</small>
              </div>
              <div class="form-field">
                <label for="account-username">账号名</label>
                <input id="account-username" v-model="username" placeholder="请输入账号名" required maxlength="40"
                  autocomplete="username" autocapitalize="none" :pattern="creating ? '[A-Za-z0-9_]{3,40}' : undefined"
                  :aria-describedby="creating ? 'username-hint' : undefined" :disabled="busy">
                <small v-if="creating" id="username-hint">3–40 位英文字母、数字或下划线</small>
              </div>
              <div class="form-field">
                <label for="account-password">密码</label>
                <input id="account-password" v-model="password" placeholder="请输入密码" type="password" required maxlength="72"
                  :minlength="creating ? 8 : undefined" :autocomplete="creating ? 'new-password' : 'current-password'"
                  :aria-describedby="creating ? 'password-hint' : undefined" :disabled="busy">
                <small v-if="creating" id="password-hint">至少 8 个字符</small>
              </div>
              <button type="submit" class="account-primary account-submit" :disabled="busy">
                {{ creating ? '注册' : '登录' }} <span aria-hidden="true">›</span>
              </button>
            </form>
            <div class="account-switch">
              <span>{{ creating ? '已有账号？' : '还没有账号？' }}</span>
              <button type="button" :disabled="busy" @click="switchMode">{{ creating ? '返回登录' : '创建账号' }}</button>
            </div>
          </template>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped lang="scss" src="../styles/account-page.scss"></style>
