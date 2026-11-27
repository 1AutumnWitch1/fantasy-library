<script setup lang="ts">
import type { PersonalPage } from '../../reader/bookTypes'
import PersonalEntries from './PersonalEntries.vue'
import PersonalInbox from './PersonalInbox.vue'
import PersonalWatchHistory from './PersonalWatchHistory.vue'

defineProps<{ page: PersonalPage; interactive: boolean; marking: boolean }>()
const emit = defineEmits<{ markRead: [id?: number] }>()
</script>

<template>
  <div v-if="page.guest" class="reader-content room-empty">
    <p>登录后可查看私人书架。</p>
    <RouterLink v-if="page.number % 2 === 1 && interactive"
      :to="{ name: 'account', query: { next: '/books/personal' } }">登录图书馆 ›</RouterLink>
  </div>
  <PersonalEntries v-else-if="page.section === 'anime' || (page.section === 'collection' && page.items.length)"
    :items="page.items" :interactive="interactive" :empty-message="page.message" />
  <div v-else-if="page.section === 'history' && page.events.length" class="reader-content">
    <PersonalWatchHistory :events="page.events" :interactive="interactive" />
  </div>
  <div v-else-if="page.section === 'inbox' && (page.notices.length || page.unread)" class="reader-content">
    <PersonalInbox :notices="page.notices" :unread="page.unread" :interactive="interactive"
      :busy="marking" @mark-read="emit('markRead', $event)" />
  </div>
  <p v-else-if="page.message" class="reader-content room-empty">{{ page.message }}</p>
</template>

<style scoped lang="scss">
.room-empty { align-self: center; text-align:center; color:#624629; }
.room-empty a { color:#254f3a; }
</style>
