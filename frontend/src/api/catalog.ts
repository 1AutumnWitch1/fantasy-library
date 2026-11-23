import { isAxiosError } from 'axios'
import { apiClient } from './client'

export type CatalogKind = 'anime' | 'games'
export interface NamedItem { id: number; slug: string; name: string }
export interface CatalogImage { id: number; kind: 'COVER' | 'GALLERY'; url: string; altText: string; sortOrder: number }
export interface Work {
  id: number
  type: 'ANIME' | 'GAME'
  title: string
  slug: string
  summary: string
  releaseDate: string | null
  category: NamedItem
  cover: CatalogImage | null
}
interface AnimeInfo { episodeCount: number | null; studio: string; airingStatus: 'UPCOMING' | 'ONGOING' | 'COMPLETED' }
interface GameInfo { developer: string; ageRating: string | null; platforms: NamedItem[] }
export interface WorkItem { work: Work; tags: NamedItem[]; anime?: AnimeInfo; game?: GameInfo }
export interface WorkDetail extends WorkItem { images: CatalogImage[] }
export interface CatalogPageResult { items: WorkItem[]; page: number; size: number; total: number; totalPages: number }
export interface CatalogQuery { page: number; size: number; q?: string; tags?: readonly string[] }

interface ApiResult<T> { code: string; message: string; data: T }
async function read<T>(path: string, signal: AbortSignal, params?: object): Promise<T> {
  const { data } = await apiClient.get<ApiResult<T>>(path, { signal, params })
  if (data.code !== 'OK' || data.data == null) throw new Error('馆藏服务返回了无法读取的数据')
  return data.data
}
export function listWorks(kind: CatalogKind, query: CatalogQuery, signal: AbortSignal) {
  const params = new URLSearchParams({ page: String(query.page), size: String(query.size) })
  if (query.q) params.set('q', query.q)
  query.tags?.forEach(tag => params.append('tags', tag))
  return read<CatalogPageResult>('/' + kind, signal, params)
}
export const getWork = (id: string, signal: AbortSignal) =>
  read<WorkDetail>('/works/' + encodeURIComponent(id), signal)
export const getTags = (kind: CatalogKind, signal: AbortSignal) =>
  read<NamedItem[]>('/tags', signal, { type: kind === 'games' ? 'GAME' : 'ANIME' })
export function catalogError(error: unknown) {
  if (isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === 'string' && message.trim()) return message
    if (error.code === 'ECONNABORTED') return '馆藏查询超时，请重试'
    return '无法连接馆藏服务，请确认后端已启动'
  }
  return '无法读取馆藏资料，请重试'
}
export const airingLabels = { UPCOMING: '未开播', ONGOING: '播出中', COMPLETED: '已完结' } as const
