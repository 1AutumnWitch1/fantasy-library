import { createRouter, createWebHistory } from 'vue-router'
import { findReaderBook } from '../data/readerBooks'
import ShelfPage from '../pages/ShelfPage.vue'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'shelf',
      component: ShelfPage,
      meta: { title: '幻想图书馆 · 馆藏书架' },
      children: [
        // ShelfPage owns the reader so a category change can finish the current animation.
        { path: 'books/:bookId', name: 'book', components: {} },
      ],
    },
    {
      path: '/community/posts/new',
      name: 'community-new',
      component: () => import('../pages/CommunityEditPage.vue'),
      meta: { title: '撰写手稿 · 幻想图书馆' },
    },
    {
      path: '/community/posts/:id/edit',
      name: 'community-edit',
      component: () => import('../pages/CommunityEditPage.vue'),
      meta: { title: '修订手稿 · 幻想图书馆' },
    },
    {
      path: '/community/posts/:id',
      name: 'community-post',
      component: () => import('../pages/CommunityPostPage.vue'),
      meta: { title: '社区手稿 · 幻想图书馆' },
    },
    {
      path: '/works/:id/watch',
      name: 'work-watch',
      component: () => import('../pages/WatchPage.vue'),
      meta: { title: '放映室 · 幻想图书馆' },
    },
    {
      path: '/works/:id',
      name: 'work-detail',
      component: () => import('../pages/WorkDetailPage.vue'),
      meta: { title: '作品资料 · 幻想图书馆' },
    },
    {
      path: '/account',
      name: 'account',
      component: () => import('../pages/AccountPage.vue'),
      meta: { title: '馆员资料 · 幻想图书馆' },
    },
    {
      path: '/status',
      name: 'status',
      component: () => import('../pages/StatusPage.vue'),
      meta: { title: '幻想图书馆 · 系统连接状态' },
    },
  ],
})

router.beforeEach((to) => {
  if (to.name === 'book' && !findReaderBook(to.params.bookId)) return { name: 'shelf', replace: true }
})

router.afterEach((to) => {
  const book = to.name === 'book' ? findReaderBook(to.params.bookId) : undefined
  document.title = book ? book.spine.title + ' · 幻想图书馆' : String(to.meta.title ?? '幻想图书馆')
})
