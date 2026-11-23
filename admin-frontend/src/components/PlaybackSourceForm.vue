<script setup lang="ts">
import { onScopeDispose, reactive, watch } from 'vue'
import type { SourceDraft } from '../api/editorialMedia'
import { useMediaUpload } from './useMediaUpload'

const props = defineProps<{ initial: SourceDraft; busy: boolean }>()
const emit = defineEmits<{ save: [SourceDraft]; cancel: []; uploading: [boolean] }>()
const value = reactive({ ...props.initial })
const { uploading, status, error, upload } = useMediaUpload()
watch(uploading, state => emit('uploading', state))
onScopeDispose(() => emit('uploading', false))

async function choose(event: Event) {
  const uploaded = await upload('playback', event)
  if (uploaded) {
    value.url = uploaded.url
    if (uploaded.format) value.format = uploaded.format
  }
}
</script>

<template>
  <form class="editor-form" @submit.prevent="emit('save', { ...value })">
    <fieldset :disabled="busy || uploading">
      <label>来源名称<input v-model.trim="value.label" maxlength="100" required></label>
      <label>画质<input v-model.trim="value.quality" maxlength="40" placeholder="1080p" required></label>
      <label class="wide">视频地址
        <input v-model.trim="value.url" maxlength="1000" placeholder="粘贴 HTTPS 视频地址，或上传本地视频" required>
      </label>
      <label>视频格式<select v-model="value.format"><option value="MP4">MP4</option><option value="WEBM">WebM</option></select></label>
      <label>上传本地视频（最多 5 GiB）
        <input type="file" accept=".mp4,.webm,video/mp4,video/webm" @change="choose">
      </label>
      <div class="editor-form-actions">
        <button class="primary" type="submit">保存视频</button>
        <button type="button" @click="emit('cancel')">取消</button>
      </div>
    </fieldset>
    <p v-if="uploading" role="status">{{ status }}</p>
    <p v-if="error" class="forum-error" role="alert">{{ error }}</p>
  </form>
</template>
