import { apiClient } from './client'

export type Edition = 'EPISODE' | 'PREVIEW'

export interface PlaybackSource {
  id: number
  label: string
  quality: string
  url: string
  format: 'MP4' | 'WEBM'
}

export interface Episode {
  id: number
  edition: Edition
  number: number
  title: string
  durationSeconds: number
  nodeId: number
  nodeLabel: string
  sources: PlaybackSource[]
}

export interface PlaybackLine { id: number; label: string; episodes: Episode[] }

export function groupPlayback(episodes: Episode[]): PlaybackLine[] {
  const lines = new Map<number, PlaybackLine>()
  for (const episode of episodes) {
    if (episode.sources.length === 0) continue
    const existing = lines.get(episode.nodeId)
    if (existing) existing.episodes.push(episode)
    else lines.set(episode.nodeId, { id: episode.nodeId, label: episode.nodeLabel, episodes: [episode] })
  }
  return [...lines.values()]
}

export async function listEpisodes(workId: number, signal: AbortSignal, allNodes = false) {
  const { data } = await apiClient.get<{ code: string; data: Episode[] }>('/works/' + workId + '/episodes', { signal, params: allNodes ? { allNodes } : undefined })
  if (data.code !== 'OK' || !Array.isArray(data.data)) throw new Error('放映目录暂不可用')
  return data.data
}

export function episodeLabel(episode: Episode) {
  return episode.edition === 'PREVIEW' ? '预告 ' + episode.number : '第 ' + episode.number + ' 集'
}

export function timeLabel(seconds: number) {
  const safe = Number.isFinite(seconds) ? Math.max(0, Math.floor(seconds)) : 0
  const pair = (number: number) => String(number).padStart(2, '0')
  const hours = Math.floor(safe / 3600)
  return (hours ? hours + ':' : '') + pair(Math.floor(safe / 60) % 60) + ':' + pair(safe % 60)
}
