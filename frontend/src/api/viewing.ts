import { request } from './account'
import { apiClient } from './client'

export interface PlaybackStats { followers: number; plays: number }
export interface SavedEpisode {
  episodeNo: number
  positionMs: number
  durationMs: number
  completed: boolean
  updatedAt: string
}
export interface ViewingProgress { latest: SavedEpisode | null; completedEpisodes: number }

interface Sample {
  positionMs: number
  durationMs: number
  playing: boolean
  discontinuity: boolean
}

export const playbackStats = (id: number, signal?: AbortSignal) =>
  request<PlaybackStats>({ url: '/works/' + id + '/playback-stats', signal })
export const viewingProgress = (id: number, signal?: AbortSignal) =>
  request<ViewingProgress>({ url: '/me/works/' + id + '/viewing-progress', signal })

let pending: Promise<void> = Promise.resolve()

export function afterViewingWrites() { return pending }

export function trackPlayback(workId: number, sourceId: number, counted: () => void) {
  const url = '/works/' + workId + '/playback-sessions'
  let session: string | undefined
  let proof: { headerName: string; token: string } | undefined

  function enqueue(action: () => Promise<void>) {
    pending = pending.then(action).catch(() => { session = undefined; proof = undefined })
  }

  async function refreshProof() {
    return request<{ headerName: string; token: string }>({ url: '/auth/csrf' })
  }

  async function connect(positionMs: number) {
    proof = await refreshProof()
    const result = await request<{ id: string }>({
      method: 'POST', url, data: { sourceId, positionMs }, withXSRFToken: false,
      headers: { [proof.headerName]: proof.token },
    })
    session = result.id
  }

  function send(sample: Sample) {
    if (!session || !proof) return Promise.resolve()
    const base = (apiClient.defaults.baseURL ?? '/api').replace(/\/$/, '')
    return fetch(base + url + '/' + session, {
      method: 'PUT', credentials: 'same-origin', keepalive: true,
      headers: { 'Content-Type': 'application/json', [proof.headerName]: proof.token },
      body: JSON.stringify(sample),
    }).then(async response => {
      if (!response.ok) throw new Error('观看记录请求失败')
      const result = await response.json() as { data?: { counted: boolean } }
      if (result.data?.counted) counted()
    })
  }

  return {
    begin(positionMs: number) { enqueue(() => session ? Promise.resolve() : connect(positionMs)) },
    sample(sample: Sample) {
      enqueue(async () => {
        if (!session) { await connect(sample.positionMs); return }
        proof = await refreshProof()
        const result = await request<{ counted: boolean }>({
          method: 'PUT', url: url + '/' + session, data: sample, withXSRFToken: false,
          headers: { [proof.headerName]: proof.token },
        })
        if (result.counted) counted()
      })
    },
    leave(sample: Sample) { void send(sample).catch(() => undefined) },
  }
}
