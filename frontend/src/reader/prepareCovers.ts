import type { WorkItem } from '../api/catalog'
import { coverPreview } from './coverPreview'

// Give incoming artwork time to decode while the previous page is still visible.
// An unavailable image must never block the book controls indefinitely.
export async function prepareCovers(items: WorkItem[], signal: AbortSignal) {
  if (signal.aborted || typeof Image === 'undefined') return
  const urls = [...new Set(items.flatMap(({ work }) => {
    if (!work.cover?.url) return []
    const small = coverPreview(work.cover.url)
    return small === work.cover.url ? [] : [small]
  }))].slice(0, 2)
  if (!urls.length) return
  let finish!: () => void
  const stop = new Promise<void>(resolve => { finish = resolve })
  signal.addEventListener('abort', finish, { once: true })
  const deadline = setTimeout(finish, 160)
  try {
    const images = urls.map(async url => {
      const image = new Image()
      image.decoding = 'async'
      image.src = url
      await image.decode()
    })
    await Promise.race([Promise.allSettled(images), stop])
  } finally {
    signal.removeEventListener('abort', finish)
    clearTimeout(deadline)
  }
}
