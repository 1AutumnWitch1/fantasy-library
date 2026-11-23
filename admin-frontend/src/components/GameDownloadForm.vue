<script setup lang="ts">
import { onScopeDispose, reactive, ref, watch } from 'vue'
import type { DownloadDraft } from '../api/downloadEditorial'
import type { Option } from '../api/editorial'
import { useMediaUpload } from './useMediaUpload'

const props = defineProps<{ initial: DownloadDraft; platforms: Option[]; busy: boolean; creating: boolean }>()
const emit = defineEmits<{ save: [draft: DownloadDraft]; cancel: []; uploading: [state: boolean] }>()
const draft = reactive({ ...props.initial })
const mode = ref<'external' | 'file'>(draft.fileName ? 'file' : 'external')
const { uploading, status, error, upload } = useMediaUpload()
watch(uploading, value => emit('uploading', value), { flush: 'sync' })
onScopeDispose(() => emit('uploading', false))

async function choose(event: Event) {
  const result = await upload('download', event)
  if (result) draft.fileName = result.fileName
}

function save() {
  emit('save', {
    ...draft,
    externalUrl: mode.value === 'external' ? draft.externalUrl : null,
    fileName: mode.value === 'file' ? draft.fileName : null,
  })
}
</script>

<template>
  <form class="download-form" @submit.prevent="save">
    <fieldset :disabled="busy || uploading">
      <label>节点名称<input v-model.trim="draft.label" maxlength="100" required></label>
      <label>适用平台
        <select v-model.number="draft.platformId" required>
          <option v-for="platform in platforms" :key="platform.id" :value="platform.id">{{ platform.name }}</option>
        </select>
      </label>
      <label>资源类型
        <select v-model="mode">
          <option value="external">外部下载链接</option>
          <option value="file">站内安装包</option>
        </select>
      </label>
      <label v-if="creating" class="publish"><input v-model="draft.listed" type="checkbox">保存后上架</label>
      <label v-if="mode === 'external'" class="wide">下载地址
        <input v-model.trim="draft.externalUrl" type="url" maxlength="1000" placeholder="https://…" required>
      </label>
      <label v-else class="wide">上传或替换安装包（最多 5 GiB）
        <input type="file" @change="choose">
        <small v-if="draft.fileName">已选文件 · {{ draft.fileName.split('/').pop() }}</small>
      </label>
      <div class="form-actions">
        <button type="submit" class="primary" :disabled="mode === 'file' && !draft.fileName">保存节点</button>
        <button type="button" @click="emit('cancel')">取消</button>
      </div>
    </fieldset>
    <p v-if="uploading" role="status">{{ status }}</p>
    <p v-if="error" role="alert" class="forum-error">{{ error }}</p>
  </form>
</template>

<style scoped lang="scss">
.download-form { padding:.7rem; background:#f6e8cd; border:1px solid var(--paper-line); border-radius:.35rem; }
fieldset { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:.55rem; margin:0; padding:0; border:0; }
label { display:grid; gap:.3rem; min-width:0; }
.publish { display:flex; align-items:center; gap:.4rem; }
.wide,.form-actions { grid-column:1/-1; }
.form-actions { display:flex; flex-wrap:wrap; gap:.5rem; }
small { overflow-wrap:anywhere; }
@media(max-width:470px) { fieldset { grid-template-columns:1fr; } }
</style>
