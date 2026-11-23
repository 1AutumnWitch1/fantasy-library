<script setup lang="ts">
import { onBeforeUnmount, ref, shallowRef } from 'vue'
import { accountError, needsLogin } from '../../api/account'
import { useAccountStore } from '../../stores/account'
import AuthorAvatar from '../community/AuthorAvatar.vue'

const account = useAccountStore()
const upload = ref<HTMLInputElement | null>(null)
const chosen = shallowRef<File | null>(null)
const preview = ref('')
const busy = ref(false)
const error = ref('')
const notice = ref('')

function clearPreview() {
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = ''
  chosen.value = null
}
onBeforeUnmount(clearPreview)

function selectFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  error.value = ''
  notice.value = ''
  if (!['image/png', 'image/jpeg'].includes(file.type) || file.size > 2 * 1024 * 1024) {
    error.value = '请选择不超过 2 MB 的 JPEG 或 PNG。'
    return
  }
  clearPreview()
  chosen.value = file
  preview.value = URL.createObjectURL(file)
}

async function save(file: File | null) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await account.saveAvatar(file)
    clearPreview()
    notice.value = file ? '头像已保存。' : '已恢复默认头像。'
  } catch (reason) {
    if (needsLogin(reason)) account.signedOut()
    error.value = accountError(reason)
  } finally { busy.value = false }
}
</script>

<template>
  <section class="avatar-editor" aria-label="头像设置">
    <AuthorAvatar :avatar-key="account.profile?.avatarKey" :preview="preview" />
    <div class="avatar-controls">
      <div class="avatar-actions">
        <input ref="upload" type="file" accept="image/jpeg,image/png" hidden @change="selectFile">
        <button type="button" :disabled="busy" @click="upload?.click()">选择头像</button>
        <button v-if="chosen" type="button" :disabled="busy" @click="save(chosen)">保存头像</button>
        <button v-if="chosen" type="button" :disabled="busy" @click="clearPreview">取消</button>
        <button v-else-if="account.profile?.avatarKey" type="button" :disabled="busy" @click="save(null)">移除头像</button>
      </div>
      <p v-if="error" role="alert" class="avatar-error">{{ error }}</p>
      <p v-else-if="notice" role="status">{{ notice }}</p>
      <p v-else>JPEG 或 PNG，最多 2 MB</p>
    </div>
  </section>
</template>

<style scoped lang="scss">
.avatar-editor {
  --author-avatar-size: clamp(2.65rem, 5.7vw, 3.65rem);
  display: flex; align-items: center; gap: .65rem; padding-block: 0 .7rem; margin-bottom: .75rem;
  border-bottom: 1px solid #95754d75;
}
.avatar-controls { min-width: 0; }
.avatar-actions { display: flex; flex-wrap: wrap; align-items: center; gap: .2rem .42rem; }
button {
  border: 1px solid #9c7747; color: #315044; background: #fff0c670;
  padding: .22rem .41rem; border-radius: .18rem; font: inherit; font-size: .84rem; font-weight: 600; cursor: pointer;
  &:disabled { opacity: .55; cursor: wait; }
  &:focus-visible { outline: 2px solid #496851; outline-offset: 2px; }
}
p { font-size: .77rem; color:#624731; margin: .25rem 0 0; }
.avatar-error { color:#773027; }
</style>
