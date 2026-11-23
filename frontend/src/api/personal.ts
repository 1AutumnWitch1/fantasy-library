import { write } from './account'
import type { WatchStatus } from './account'
import { apiClient } from './client'
import type { Page } from './community'

export type PersonalChapter = 'anime' | 'games' | 'history' | 'inbox'
export type NoticeKind = 'POST_COMMENT' | 'COMMENT_REPLY' | 'POST_LIKE'

export interface WatchEvent {
  id: number
  workId: number
  title: string
  coverUrl: string | null
  watchStatus: WatchStatus
  watchedEpisodes: number
  episodeCount: number | null
  recordedAt: string
}

export interface Notice {
  id: number
  kind: NoticeKind
  actorName: string
  avatarKey: string | null
  postId: number
  screeningWorkId: number | null
  postTitle: string
  excerpt: string | null
  createdAt: string
  read: boolean
}

export interface Inbox {
  page: Page<Notice>
  unread: number
}

async function read<T>(path: string, page: number, size: number, signal: AbortSignal) {
  const { data } = await apiClient.get<{ code: string; data: T }>(path, { params: { page, size }, signal })
  if (data.code !== 'OK' || data.data == null) throw new Error('无法读取私人档案')
  return data.data
}

export const getHistory = (page: number, size: number, signal: AbortSignal) =>
  read<Page<WatchEvent>>('/me/reading-history', page, size, signal)

export const getInbox = (page: number, size: number, signal: AbortSignal) =>
  read<Inbox>('/me/inbox', page, size, signal)

export const markNoticeRead = (id?: number) =>
  write<void>('PUT', id === undefined ? '/me/inbox/read' : '/me/inbox/' + id + '/read')
