import { CubismFramework, LogLevel, Option } from '@framework/live2dcubismframework'
import { CubismWebGLOffscreenManager } from '@framework/rendering/cubismoffscreenmanager'
import { Live2DCharacter } from './character/live2d-model'
import { ACTIVE_MODEL } from './character/model-config'

let loading: Promise<void> = Promise.resolve()

export function createMascot(canvas: HTMLCanvasElement, root: string, signal: AbortSignal, onError: () => void) {
  // A page can be left while its model is still loading. Let that setup release
  // its context before another canvas starts using the Cubism singleton.
  const task = loading.then(() => mountMascot(canvas, root, signal, onError))
  loading = task.then(() => undefined, () => undefined)
  return task
}

async function mountMascot(canvas: HTMLCanvasElement, root: string, signal: AbortSignal, onError: () => void) {
  signal.throwIfAborted()
  if (typeof Live2DCubismCore === 'undefined') throw new Error('Cubism Core is unavailable')

  const gl = canvas.getContext('webgl2', { alpha: true, premultipliedAlpha: true })
  if (!gl) throw new Error('WebGL 2 is unavailable')
  const option = new Option()
  option.loggingLevel = LogLevel.LogLevel_Warning
  CubismFramework.startUp(option)
  CubismFramework.initialize()

  const listeners = new AbortController()
  let model: Live2DCharacter | undefined
  let frame = 0
  let lastFrame = 0
  let active = false
  let tap: { x: number; y: number } | undefined
  const pixel = new Uint8Array(4)
  const observer = new ResizeObserver(resize)

  function resize() {
    const { clientWidth, clientHeight } = canvas
    if (!clientWidth || !clientHeight) return
    const scale = Math.min(window.devicePixelRatio || 1, 1.5)
    const width = Math.floor(clientWidth * scale)
    const height = Math.floor(clientHeight * scale)
    if (canvas.width === width && canvas.height === height) return
    canvas.width = width
    canvas.height = height
    gl!.viewport(0, 0, width, height)
    model?.updateViewport(width, height)
  }

  function stop() {
    cancelAnimationFrame(frame)
    frame = 0
    tap = undefined
    model?.resetLookTarget()
  }

  function render(now: number) {
    frame = 0
    if (!model || !active || document.hidden || signal.aborted || gl!.isContextLost()) return
    gl!.clearColor(0, 0, 0, 0)
    gl!.clear(gl!.COLOR_BUFFER_BIT | gl!.DEPTH_BUFFER_BIT)
    gl!.enable(gl!.BLEND)
    gl!.blendFunc(gl!.SRC_ALPHA, gl!.ONE_MINUS_SRC_ALPHA)
    model.update(Math.min((now - lastFrame) / 1000, 0.1))
    lastFrame = now
    const offscreen = CubismWebGLOffscreenManager.getInstance()
    offscreen.beginFrameProcess(gl!)
    model.draw()
    offscreen.endFrameProcess(gl!)
    offscreen.releaseStaleRenderTextures(gl!)
    if (tap) {
      // The model hit areas are broader than its visible outline.
      gl!.readPixels(tap.x, tap.y, 1, 1, gl!.RGBA, gl!.UNSIGNED_BYTE, pixel)
      if (pixel[3] > 24) model.playRandomInteraction()
      tap = undefined
    }
    frame = requestAnimationFrame(render)
  }

  function sync() {
    if (!active || document.hidden || gl!.isContextLost()) {
      stop()
    } else if (!frame) {
      resize()
      lastFrame = performance.now()
      frame = requestAnimationFrame(render)
    }
  }

  function coordinates(event: PointerEvent) {
    if (!active || document.hidden || signal.aborted || !model) return
    const rect = canvas.getBoundingClientRect()
    if (!rect.width || !rect.height) return
    return {
      x: (event.clientX - rect.left) / rect.width * 2 - 1,
      y: 1 - (event.clientY - rect.top) / rect.height * 2,
      aspect: rect.width / rect.height,
    }
  }

  function dispose() {
    stop()
    listeners.abort()
    observer.disconnect()
    CubismWebGLOffscreenManager.getInstance().removeContext(gl!)
    try {
      model?.release()
    } finally {
      CubismFramework.dispose()
      gl!.getExtension('WEBGL_lose_context')?.loseContext()
    }
  }

  try {
    resize()
    model = await Live2DCharacter.load({
      gl, config: { ...ACTIVE_MODEL, dir: root + 'model/', shaderPath: root + 'shaders/' },
      canvasWidth: canvas.width, canvasHeight: canvas.height,
    })
    model.setZoom(1.42)
    model.updateViewport(canvas.width, canvas.height)
    signal.throwIfAborted()
  } catch (error) {
    dispose()
    throw error
  }

  signal.addEventListener('abort', dispose, { once: true })
  const input = { signal: listeners.signal, passive: true }
  document.addEventListener('visibilitychange', sync, input)
  document.addEventListener('mouseleave', () => model?.resetLookTarget(), input)
  window.addEventListener('blur', () => model?.resetLookTarget(), input)
  window.addEventListener('pointermove', event => {
    if (event.pointerType === 'touch') return
    const point = coordinates(event)
    if (point) model?.setLookTarget(point.x, point.y)
  }, input)
  // The whole canvas passes pointer events through. Only a model hit reacts.
  window.addEventListener('pointerup', event => {
    if (event.button !== 0 || !event.isPrimary) return
    const crop = canvas.parentElement?.getBoundingClientRect()
    if (!crop || event.clientX < crop.left || event.clientX >= crop.right ||
        event.clientY < crop.top || event.clientY >= crop.bottom) return
    const point = coordinates(event)
    if (!point || Math.abs(point.x) > 1 || Math.abs(point.y) > 1) return
    if (!model?.hitTest(point.x * point.aspect, point.y)) return
    tap = {
      x: Math.min(canvas.width - 1, Math.floor((point.x + 1) * canvas.width / 2)),
      y: Math.min(canvas.height - 1, Math.floor((point.y + 1) * canvas.height / 2)),
    }
  }, input)
  canvas.addEventListener('webglcontextlost', () => {
    stop()
    onError()
  }, { signal: listeners.signal })
  observer.observe(canvas)

  return {
    setActive(value: boolean) {
      active = value
      sync()
    },
  }
}
