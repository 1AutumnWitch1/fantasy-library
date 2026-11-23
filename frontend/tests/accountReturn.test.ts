import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { accountReturnPath } from '../src/router/accountReturn'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', name: 'shelf', component: {} },
    { path: '/status', name: 'status', component: {} },
    { path: '/account', name: 'account', component: {} },
    { path: '/works/:id', name: 'work-detail', component: {} },
    { path: '/books/:bookId', name: 'book', component: {} },
  ],
})

describe('login return path', () => {
  it('preserves the original path, query and fragment over browser history', () => {
    const path = '/works/4?from=games#info'
    expect(accountReturnPath(router, path, '/status')).toBe(path)
  })

  it('uses a valid previous route without an explicit destination', () => {
    expect(accountReturnPath(router, undefined, '/status')).toBe('/status')
    expect(accountReturnPath(router, undefined, '/books/personal')).toBe('/books/personal')
  })

  it('keeps external or looping redirects in the library', () => {
    expect(accountReturnPath(router, '//example.com', '/account?next=/status')).toBe('/')
    expect(accountReturnPath(router, '/\\example.com', '/status')).toBe('/status')
    expect(accountReturnPath(router, 'https://example.com', undefined)).toBe('/')
  })
})
