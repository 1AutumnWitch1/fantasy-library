<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadMascot, type Mascot } from '../../live2d/loadMascot'

const props = defineProps<{ active: boolean }>()
const preference = 'fantasy-library:show-mascot'
const shown = ref(readPreference())
const canvas = ref<HTMLCanvasElement>()
const unavailable = ref(false)
const lifetime = new AbortController()
let mascot: Mascot | undefined
let loading = false

function readPreference() {
  try { return localStorage.getItem(preference) !== 'false' } catch { return true }
}

function toggle() {
  shown.value = !shown.value
  try { localStorage.setItem(preference, String(shown.value)) } catch { /* storage can be blocked */ }
}

async function sync() {
  const active = shown.value && props.active
  if (mascot) {
    mascot.setActive(active && !unavailable.value)
    return
  }
  if (!active || !canvas.value || loading || unavailable.value) return

  loading = true
  try {
    mascot = await loadMascot(canvas.value, lifetime.signal, () => { unavailable.value = true })
    mascot.setActive(shown.value && props.active && !lifetime.signal.aborted)
  } catch {
    if (!lifetime.signal.aborted) unavailable.value = true
  } finally {
    loading = false
  }
}

onMounted(sync)
watch([shown, () => props.active], sync, { flush: 'post' })
onBeforeUnmount(() => lifetime.abort())
</script>

<template>
  <aside class="library-mascot" :class="{ 'library-mascot--shown': shown && !unavailable }" aria-label="看板娘">
    <div v-show="shown && active && !unavailable" class="mascot-stage">
      <canvas id="library-mascot" ref="canvas" aria-label="桃濑日和"></canvas>
    </div>
    <button v-show="active" type="button" :aria-pressed="shown" aria-controls="library-mascot" @click="toggle">
      {{ shown ? '隐藏看板娘' : '显示看板娘' }}
    </button>
    <small v-if="active && shown && unavailable" role="status">看板娘暂不可用</small>
  </aside>
</template>

<style scoped lang="scss">
.library-mascot {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: .55rem;
  min-width: 0;
  padding: 0 .85rem .85rem;
}

.mascot-stage {
  position: relative;
  width: min(100%, 21rem);
  height: clamp(15rem, 42vh, 24rem);
  overflow: hidden;
  pointer-events: none;
}

canvas {
  position: relative;
  inset-block-start: .45rem;
  display: block;
  width: 100%;
  height: calc(100% - .65rem);
  background: transparent;
}

button {
  min-height: 2.45rem;
  padding: .25rem .75rem;
  border: 1px solid var(--brass-frame);
  border-radius: .25rem;
  background: #252b20;
  color: var(--foil);
  font: inherit;
  font-size: .88rem;
  cursor: pointer;
}

button:hover { background: #34382a; }

small { color: var(--foil); }

@media (min-width: 72rem) {
  .library-mascot--shown {
    position: relative;
    inset-inline-start: calc(-1 * clamp(1.35rem, 3vw, 3.6rem));
    gap: .8rem;
  }

  .mascot-stage {
    width: 100%;
    height: clamp(27rem, 77svh, 50rem);
    mask-image: linear-gradient(#000 calc(100% - .9rem), transparent);
  }

  canvas {
    position: absolute;
    inset-block-start: 3%;
    inset-inline-start: -34%;
    width: 168%;
    height: 154%;
  }
}
</style>
