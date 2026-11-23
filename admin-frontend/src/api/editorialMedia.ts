import { write } from './account'
import { apiClient } from './client'

export type Edition = 'EPISODE' | 'PREVIEW'
export type VideoFormat = 'MP4' | 'WEBM'
export interface MediaUploadResult { catalog: UploadedMedia; playback: UploadedMedia; download: { fileName: string } }
export type MediaKind = keyof MediaUploadResult
export interface UploadedMedia { url: string; format?: VideoFormat }
export interface PlaybackLineDraft { label: string; listed: boolean }
export interface PlaybackLine extends PlaybackLineDraft { id: number }
export interface EpisodeDraft { nodeId: number; edition: Edition; number: number; title: string; durationSeconds: number }
export interface SourceDraft { label: string; quality: string; url: string; format: VideoFormat }
export interface Source extends SourceDraft { id: number; listed: boolean }
export interface Episode extends EpisodeDraft { id: number; sources: Source[] }

const root = '/admin/library'
const player = (id: number) => root + '/works/' + id + '/playback'
const episode = (id: number, episodeId?: number) => player(id) + '/episodes' + (episodeId ? '/' + episodeId : '')

export async function listEditorialEpisodes(workId: number, signal: AbortSignal) {
  const { data } = await apiClient.get<{ code: string; data: Episode[] }>(player(workId), { signal })
  if (data.code !== 'OK' || !Array.isArray(data.data)) throw new Error('无法读取分集')
  return data.data
}

export async function listPlaybackLines(workId: number, signal: AbortSignal) {
  const { data } = await apiClient.get<{ code: string; data: PlaybackLine[] }>(player(workId) + '/nodes', { signal })
  if (data.code !== 'OK' || !Array.isArray(data.data)) throw new Error('无法读取播放线路')
  return data.data
}

export function savePlaybackLine(workId: number, nodeId: number | undefined, draft: PlaybackLineDraft) {
  return write<void>(nodeId ? 'PUT' : 'POST', player(workId) + '/nodes' + (nodeId ? '/' + nodeId : ''), draft)
}

export function saveEditorialEpisode(workId: number, episodeId: number | undefined, draft: EpisodeDraft) {
  return write<void>(episodeId ? 'PUT' : 'POST', episode(workId, episodeId), draft)
}

export function saveEditorialSource(workId: number, episodeId: number, sourceId: number | undefined, draft: SourceDraft) {
  const path = episode(workId, episodeId) + '/sources' + (sourceId ? '/' + sourceId : '')
  return write<void>(sourceId ? 'PUT' : 'POST', path, draft)
}

export function uploadMedia<K extends MediaKind>(kind: K, file: File, signal: AbortSignal, onProgress: (progress: number) => void) {
  const target = root + '/media/' + kind
  const options = {
    signal,
    timeout: kind === 'catalog' ? 120_000 : 0,
    onUploadProgress: ({ loaded, total }: { loaded: number; total?: number }) =>
      onProgress(Math.min(100, Math.round(loaded * 100 / (total || file.size)))),
  }

  if (kind !== 'catalog') {
    return write<MediaUploadResult[K]>('POST', target, file, { ...options, contentType: 'application/octet-stream',
      fileName: kind === 'download' ? file.name : undefined })
  }
  const data = new FormData()
  data.set('file', file)
  return write<MediaUploadResult[K]>('POST', target, data, options)
}
