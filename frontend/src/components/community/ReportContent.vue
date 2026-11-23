<script setup lang="ts">
import { computed, ref } from 'vue'
import { reportContent, type Target } from '../../api/community'
import { accountError, needsLogin } from '../../api/account'
import { useAccountStore } from '../../stores/account'

const props=defineProps<{ target: Target; id: number }>()
const account=useAccountStore()
const label=computed(() => '举报理由-' + props.target + '-' + props.id)
const open=ref(false)
const reason=ref('')
const busy=ref(false)
const done=ref(false)
const error=ref('')

async function submit() {
  if (!reason.value.trim() || busy.value) return
  busy.value=true
  error.value=''
  try {
    await reportContent(props.target, props.id, reason.value.trim())
    done.value=true
    open.value=false
  } catch (failure) {
    if (needsLogin(failure)) account.signedOut()
    error.value=accountError(failure)
  } finally { busy.value=false }
}
</script>

<template>
  <div class="report-control">
    <p v-if="done" role="status">举报已提交。</p>
    <button v-else type="button" :aria-expanded="open" @click="open = !open">举报{{ target === 'POST' ? '帖子' : '评论' }}</button>
    <form v-if="open" class="forum-form" @submit.prevent="submit">
      <label :for="label">举报理由<textarea :id="label" v-model="reason" rows="2" maxlength="240" required :disabled="busy" /></label>
      <p v-if="error" class="forum-error forum-message" role="alert">{{ error }}</p>
      <button type="submit" class="forum-main-button" :disabled="busy || !reason.trim()">提交举报</button>
    </form>
  </div>
</template>

<style scoped lang="scss">
.report-control { margin-top:.65rem; }
.report-control p { margin:.2rem 0; }
.report-control form { max-width:27rem; }
.report-control button { justify-self:start; }
</style>
