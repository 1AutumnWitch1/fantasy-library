import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv, type Connect, type Plugin } from 'vite'

function publicBoundary(): Plugin {
  const hideManagement: Connect.NextHandleFunction = (request, response, next) => {
    let path: string
    try {
      path = decodeURIComponent(new URL(request.url ?? '/', 'http://localhost').pathname).replace(/\/{2,}/g, '/')
    } catch {
      response.statusCode = 400
      response.end()
      return
    }
    if (/^\/(?:api\/(?:admin(?:[/;]|$)|openapi\.yaml(?:[/;]|$))|admin(?:[/;]|$)|community\/moderation(?:[/;]|$))/i.test(path)) {
      response.statusCode = 404
      response.end()
      return
    }
    next()
  }

  return {
    name: 'public-route-boundary',
    configureServer(server) { server.middlewares.use(hideManagement) },
    configurePreviewServer(server) { server.middlewares.use(hideManagement) },
  }
}


export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'API_PROXY_TARGET')
  const proxy = {
    '/api': {
      target: env.API_PROXY_TARGET || 'http://127.0.0.1:8080',
      changeOrigin: true,
    },
  }

  return {
    plugins: [publicBoundary(), vue()],
    server: { proxy, strictPort: true },
    preview: { proxy, strictPort: true },
  }
})
