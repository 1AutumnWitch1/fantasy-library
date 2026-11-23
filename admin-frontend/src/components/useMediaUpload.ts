import { computed, onScopeDispose, ref } from 'vue'
import { accountError } from '../api/account'
import { uploadMedia, type MediaKind } from '../api/editorialMedia'

export function useMediaUpload() {
  const uploading = ref(false)
  const percent = ref(0)
  const error = ref('')
  const status = computed(() => percent.value >= 100 ? '上传完成，正在写入存储…' : '正在上传 ' + percent.value + '%')
  let request: AbortController | undefined
  onScopeDispose(() => request?.abort())

  async function upload<K extends MediaKind>(kind: K, event: Event) {
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]
    input.value = ''
    if (!file || uploading.value) return
    error.value = ''
    const image = kind === 'catalog'
    const maximumBytes = 5 * 1024 ** (image ? 2 : 3)
    if (file.size === 0 || file.size > maximumBytes) {
      error.value = image ? '请选择 5 MiB 以内的非空图片。' : '请选择 5 GiB 以内的非空文件。'
      return
    }
    const controller = new AbortController()
    request = controller
    percent.value = 0
    uploading.value = true
    try {
      return await uploadMedia(kind, file, controller.signal, progress => { percent.value = progress })
    } catch (failure) {
      if (!controller.signal.aborted) error.value = accountError(failure)
    } finally {
      request = undefined
      uploading.value = false
    }
  }

  return { uploading, status, error, upload }
}
