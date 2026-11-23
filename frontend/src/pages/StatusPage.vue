<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { animateStatusPanel } from '../animations/entrance'
import HealthStatus from '../components/HealthStatus.vue'
import BookSigil from '../components/shelf/BookSigil.vue'
import { useHealthStore } from '../stores/health'

const panel = ref<HTMLElement | null>(null)
const healthStore = useHealthStore()
let revertAnimation: (() => void) | undefined

onMounted(() => {
  if (panel.value) revertAnimation = animateStatusPanel(panel.value)
  void healthStore.refresh()
})

onUnmounted(() => revertAnimation?.())
</script>

<template>
  <main class="status-page">
    <section ref="panel" class="status-panel" aria-labelledby="status-title">
      <span class="status-seal" aria-hidden="true"><BookSigil kind="compass" /></span>
      <h1 id="status-title">系统连接状态</h1>
      <HealthStatus />
    </section>
  </main>
</template>

<style scoped lang="scss">
.status-page {
  display: grid;
  min-height: calc(100vh - var(--library-header-height));
  min-height: calc(100svh - var(--library-header-height));
  place-items: center;
  padding: clamp(1rem, 6vw, 4rem);
  background: radial-gradient(ellipse at 50% 28%, #4e382764, #160e0ae8), var(--library-hall) center / cover, var(--wood-shadow);
}

.status-panel {
  width: min(100%, 34rem);
  padding: clamp(1.2rem, 5vw, 2.7rem);
  border: 1px solid var(--brass-frame);
  border-radius: .42rem;
  outline: 1px solid var(--brass-line);
  outline-offset: -0.35rem;
  background: linear-gradient(#2e2018e8, #23170ff7), var(--leather-grain) center / 14rem;
  box-shadow: var(--surface-shadow);
}

.status-seal {
  width: 2.75rem; aspect-ratio: 1; display: grid; place-items: center;
  margin-bottom: .8rem; border: 1px solid var(--brass-frame); border-radius: 50%;
  background: var(--pine); color: var(--foil); font-size: 1.6rem;
}

h1 {
  margin: 0;
  font-size: clamp(1.55rem, 5vw, 2.25rem);
  font-family: var(--display-font);
  font-weight: 700;
}
</style>
