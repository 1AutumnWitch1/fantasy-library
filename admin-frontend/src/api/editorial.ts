import { write } from './account'
import { apiClient } from './client'
import type { WorkType } from './admin'

export type AiringStatus = 'UPCOMING' | 'ONGOING' | 'COMPLETED'
export interface Option { id: number; name: string }
export interface Options { categories: Option[]; tags: Option[]; platforms: Option[] }
export interface WorkImage { url: string; alt: string }
export interface AnimeFields { studio: string; airingStatus: AiringStatus; episodeCount: number | null }
export interface GameFields { developer: string; ageRating: string | null; platformIds: number[] }
export interface Draft {
  type: WorkType
  slug: string
  title: string
  summary: string
  releaseDate: string | null
  categoryId: number
  tagIds: number[]
  anime: AnimeFields | null
  game: GameFields | null
  cover: WorkImage | null
  gallery: WorkImage[]
}
export interface WorkRemoval { queuedFiles: number; sharedFiles: number }
interface Entry {
  id: number; type: WorkType; slug: string; title: string; summary: string; releaseDate: string | null
  categoryId: number; listed: boolean
}
interface Archive {
  entry: Entry; tagIds: number[]; anime: AnimeFields | null; game: GameFields | null
  cover: WorkImage | null; gallery: WorkImage[]
}
const prefix = '/admin/library'
async function read<T>(path: string, signal: AbortSignal) {
  const { data } = await apiClient.get<{ data: T }>(prefix + path, { signal })
  return data.data
}
export const getOptions = (signal: AbortSignal) => read<Options>('/work-options', signal)
export const getArchive = (id: number, signal: AbortSignal) => read<Archive>('/works/' + id + '/details', signal)
export const deleteWork = (id: number) => write<WorkRemoval>('DELETE', prefix + '/works/' + id)
export const saveArchive = (id: number | undefined, draft: Draft) => id
  ? write<{ id: number }>('PUT', prefix + '/works/' + id + '/details', draft)
  : write<{ id: number }>('POST', prefix + '/works', draft)
