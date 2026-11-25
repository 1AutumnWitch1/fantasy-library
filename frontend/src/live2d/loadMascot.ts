export interface Mascot {
  setActive(value: boolean): void
}

interface MascotRuntime {
  createMascot(canvas: HTMLCanvasElement, root: string, signal: AbortSignal, onError: () => void): Promise<Mascot>
}

let runtime: Promise<MascotRuntime> | undefined

export async function loadMascot(canvas: HTMLCanvasElement, signal: AbortSignal, onError: () => void) {
  const root = new URL(import.meta.env.BASE_URL + 'live2d/', location.origin).href
  // The SDK reads Core while evaluating its module, before creating a model.
  runtime ??= new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = root + 'core.js'
    script.onload = () => resolve()
    script.onerror = () => {
      script.remove()
      reject(new Error('Cubism Core could not be loaded'))
    }
    document.head.append(script)
  }).then(() => import(/* @vite-ignore */ root + 'runtime.js') as Promise<MascotRuntime>)
  const sdk = await runtime
  signal.throwIfAborted()
  return sdk.createMascot(canvas, root, signal, onError)
}
