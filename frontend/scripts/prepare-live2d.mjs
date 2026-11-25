import { access, cp, mkdir } from 'node:fs/promises'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { build } from 'vite'
import { resolve, sep } from 'node:path'

const root = new URL('../', import.meta.url)
const demo = process.env.LIVE2D_ASSET_ROOT
  ? pathToFileURL(resolve(process.env.LIVE2D_ASSET_ROOT) + sep)
  : new URL('../demo/', root)
const sdk = new URL('CubismSdkForWeb-5-r.5/', demo)
const out = new URL('public/live2d/', root)

try {
  await Promise.all([
    access(new URL('Core/live2dcubismcore.min.js', sdk)),
    access(new URL('Framework/src/live2dcubismframework.ts', sdk)),
    access(new URL('hiyori_zh-Hans/hiyori_pro/runtime/hiyori_pro_t11.model3.json', demo)),
  ])
} catch (error) {
  if (error.code !== 'ENOENT') throw error
  console.log('Live2D assets are not installed. Building the bookshelf without the mascot.')
  process.exit(0)
}

await mkdir(out, { recursive: true })
await Promise.all([
  cp(new URL('Core/live2dcubismcore.min.js', sdk), new URL('core.js', out)),
  cp(new URL('Framework/Shaders/WebGL/', sdk), new URL('shaders/', out), { recursive: true }),
  cp(new URL('hiyori_zh-Hans/hiyori_pro/runtime/', demo), new URL('model/', out), { recursive: true }),
  cp(new URL('LICENSE.md', sdk), new URL('SDK-LICENSE.md', out)),
  cp(new URL('Core/LICENSE.md', sdk), new URL('Core-LICENSE.md', out)),
  cp(new URL('hiyori_zh-Hans/hiyori_pro/ReadMe.txt', demo), new URL('Model-ReadMe.txt', out)),
])

await build({
  configFile: false,
  publicDir: false,
  resolve: { alias: { '@framework': fileURLToPath(new URL('Framework/src/', sdk)) } },
  build: {
    outDir: fileURLToPath(out),
    emptyOutDir: false,
    lib: { entry: fileURLToPath(new URL('live2d/runtime.ts', root)), formats: ['es'], fileName: () => 'runtime.js' },
  },
})
