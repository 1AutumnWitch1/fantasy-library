<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'
withDefaults(defineProps<{ back: RouteLocationRaw; backLabel?: string }>(), { backLabel: '返回讨论档案' })
</script>

<template>
  <main class="forum-room">
    <div class="forum-frame">
      <RouterLink class="forum-back" :to="back">‹ {{ backLabel }}</RouterLink>
      <slot />
    </div>
  </main>
</template>

