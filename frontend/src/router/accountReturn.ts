import type { Router } from 'vue-router'

export function accountReturnPath(router: Router, requested: unknown, previous: unknown) {
  for (const path of [requested, previous]) {
    if (typeof path !== 'string' || !path.startsWith('/') || path.startsWith('//') || path.includes('\\')) continue
    const target = router.resolve(path)
    if (target.name && target.name !== 'account' && target.matched.length) return target.fullPath
  }
  return router.resolve({ name: 'shelf' }).fullPath
}
