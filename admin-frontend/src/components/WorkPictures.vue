<script setup lang="ts">
import { computed, onScopeDispose, ref, watch } from 'vue'
import type { WorkImage } from '../api/editorial'
import { useMediaUpload } from './useMediaUpload'

const cover = defineModel<WorkImage | null>('cover', { required: true })
const gallery = defineModel<WorkImage[]>('gallery', { required: true })
const failedCover = ref('')
const blocking = defineModel<boolean>('uploading', { default: false })
const { uploading, status, error, upload } = useMediaUpload()
const accept = '.jpg,.jpeg,.png,.webp,.avif,.gif'
watch(uploading, value => { blocking.value = value }, { flush: 'sync' })
onScopeDispose(() => { blocking.value = false })

async function choose(image: WorkImage, event: Event) {
  const uploaded = await upload('catalog', event)
  if (uploaded) {
    image.url = uploaded.url
    failedCover.value = ''
  }
}
function usable(url: string) {
  try {
    const address = new URL(url, location.origin)
    return (/^https:\/\//i.test(url) && address.hostname && !address.username && !address.password)
      || /^\/api\/media\/catalog\/(?:(?:editions|uploads)\/)?[a-z0-9_-]+\.(webp|jpe?g|png|avif|gif)$/i.test(url)
  } catch { return false }
}
const preview = computed(() => {
  const address = cover.value?.url.trim()
  return address && usable(address) && address !== failedCover.value ? address : undefined
})
function move(index: number, step: number) {
  const list = [...gallery.value]
  const [image] = list.splice(index, 1)
  list.splice(index + step, 0, image)
  gallery.value = list
}
</script>

<template>
  <section class="admin-panel artwork">
    <h2>封面与插图</h2>
    <p v-if="uploading" role="status">{{ status }}</p>
    <p v-if="error" class="forum-error" role="alert">{{ error }}</p>
    <div class="cover-layout">
      <div class="image-preview">
        <img v-if="preview" :src="preview" :alt="cover?.alt || '封面预览'" referrerpolicy="no-referrer"
          @error="failedCover = preview ?? ''">
        <span v-else>{{ cover?.url && failedCover === cover.url.trim() ? '封面加载失败' : '封面预览' }}</span>
      </div>
      <div v-if="cover" class="image-fields">
        <label>封面地址<input v-model.trim="cover.url" required maxlength="500" placeholder="粘贴 HTTPS 图片地址，或上传本地图片"></label>
        <label>上传封面<input type="file" :accept="accept" :disabled="uploading" @change="choose(cover, $event)"></label>
        <label>封面说明<input v-model.trim="cover.alt" required maxlength="180" placeholder="简述图片内容"></label>
        <button type="button" @click="cover = null">移除封面</button>
      </div>
      <button v-else type="button" @click="cover = { url: '', alt: '' }">设置封面</button>
    </div>

    <div class="gallery-heading">
      <h3>详情插图</h3>
      <button type="button" :disabled="gallery.length >= 8" @click="gallery = [...gallery, { url:'', alt:'' }]">添加插图</button>
    </div>
    <div v-for="(image, index) in gallery" :key="index" class="gallery-entry">
      <img v-if="image.url && usable(image.url)" :src="image.url" alt="" loading="lazy" referrerpolicy="no-referrer">
      <div class="image-fields">
        <label>插图 {{ index + 1 }} 地址<input v-model.trim="image.url" required maxlength="500" placeholder="粘贴 HTTPS 图片地址，或上传本地图片"></label>
        <label>上传插图 {{ index + 1 }}<input type="file" :accept="accept" :disabled="uploading" @change="choose(image, $event)"></label>
        <label>插图说明<input v-model.trim="image.alt" required maxlength="180"></label>
        <div class="image-actions">
          <button type="button" :disabled="index === 0" @click="move(index, -1)">上移</button>
          <button type="button" :disabled="index === gallery.length - 1" @click="move(index, 1)">下移</button>
          <button type="button" @click="gallery = gallery.filter((_, current) => current !== index)">移除</button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.artwork h2 { margin-bottom:.8rem; }
.cover-layout, .gallery-entry { display:flex; align-items:flex-start; gap:.85rem; }
.image-preview {
  display:grid; place-items:center; width:clamp(5.2rem, 17vw, 8.5rem); flex:none; aspect-ratio:3 / 4;
  border:1px solid var(--paper-line); border-radius:.32rem; overflow:hidden; background:#e9e8dd;
  color:var(--muted); text-align:center; font-size:.8rem;
}
.image-preview img { width:100%; height:100%; object-fit:cover; }
.image-fields { min-width:0; flex:1; display:grid; gap:.6rem; }
.image-fields label { display:grid; gap:.28rem; }
.image-fields > button { justify-self:start; }
.gallery-heading { display:flex; align-items:center; justify-content:space-between; gap:.55rem; margin-top:1.1rem; }
.gallery-heading h3 { margin:0; font-size:1rem; }
.gallery-entry { margin-top:.6rem; border-top:1px solid var(--paper-line); padding-top:.6rem; }
.gallery-entry img { width:4.2rem; aspect-ratio:4/3; object-fit:cover; border-radius:.25rem; }
.image-actions { display:flex; flex-wrap:wrap; gap:.38rem; }
@media(max-width:440px) { .cover-layout, .gallery-entry { flex-direction:column; } .image-fields { width:100%; } }
</style>
