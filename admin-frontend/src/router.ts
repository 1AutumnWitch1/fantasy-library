import { createRouter, createWebHistory } from 'vue-router'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'manager', component: () => import('./components/LibraryManager.vue') },
    { path: '/works/new', name: 'work-new', component: () => import('./components/WorkEditor.vue') },
    { path: '/works/:id/edit', name: 'work-edit', component: () => import('./components/WorkEditor.vue') },
    { path: '/reports', name: 'community-review', component: () => import('./components/CommunityReview.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
export function publicUrl(path = '/') {
  const origin = import.meta.env.VITE_PUBLIC_SITE_URL || `${location.protocol}//${location.hostname}:5173`
  return new URL(path, origin).href
}
