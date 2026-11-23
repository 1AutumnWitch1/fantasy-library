import { apiClient } from './client'
import { write } from './account'

export const topics = {
  GENERAL: '馆内闲谈', ANIME: '追番讨论', GAME: '游戏讨论', GUIDE: '攻略与资料',
} as const
export type Topic = keyof typeof topics
export type Target = 'POST' | 'COMMENT'
export type ReviewAction = 'HIDE' | 'DELETE' | 'DISMISS'
export interface Page<T> { items: T[]; page: number; size: number; total: number; totalPages: number }
export interface Report {
  id: number; targetType: Target; targetId: number; postId: number
  content: string; reporter: string; reason: string; createdAt: string
}
async function read<T>(url: string, params?: object, signal?: AbortSignal): Promise<T> {
  const { data } = await apiClient.get<{ code: string; data: T }>(url, { params, signal })
  if (data.code !== 'OK') throw new Error('社区暂不可用')
  return data.data
}

export const listReports = (page: number, signal?: AbortSignal) =>
  read<Page<Report>>('/admin/community/reports', { page }, signal)
export const review = (id: number, action: ReviewAction) => write<void>('PUT', '/admin/community/reports/' + id, { action })

export const dateLabel = (value: string) =>
  new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
