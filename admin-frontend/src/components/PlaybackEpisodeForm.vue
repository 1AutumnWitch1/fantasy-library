<script setup lang="ts">
import { reactive } from 'vue'
import type { EpisodeDraft, PlaybackLine } from '../api/editorialMedia'

const props = defineProps<{ initial: EpisodeDraft; lines: PlaybackLine[]; busy: boolean }>()
const emit = defineEmits<{ save: [EpisodeDraft]; cancel: [] }>()
const value = reactive({ ...props.initial })
</script>

<template>
  <form class="editor-form" @submit.prevent="emit('save', { ...value })">
    <fieldset :disabled="busy">
      <label>播放来源
        <select v-model.number="value.nodeId" required>
          <option v-for="line in lines" :key="line.id" :value="line.id">{{ line.label }}</option>
        </select>
      </label>
      <label>分集类型
        <select v-model="value.edition">
          <option value="EPISODE">正片</option>
          <option value="PREVIEW">预告</option>
        </select>
      </label>
      <label>分集编号<input v-model.number="value.number" type="number" min="1" max="999999" required></label>
      <label>分集名称<input v-model.trim="value.title" maxlength="180" required></label>
      <label>时长（秒）<input v-model.number="value.durationSeconds" type="number" min="1" max="9999999" required></label>
      <div class="editor-form-actions">
        <button class="primary" type="submit">保存分集</button>
        <button type="button" @click="emit('cancel')">取消</button>
      </div>
    </fieldset>
  </form>
</template>
