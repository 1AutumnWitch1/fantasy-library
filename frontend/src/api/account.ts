import { isAxiosError, type AxiosRequestConfig } from 'axios'
import { apiClient } from './client'

export interface Profile { id: number; username: string; displayName: string; bio: string | null; avatarKey: string | null }
export interface Credentials { username: string; password: string }
export type WatchStatus = 'PLAN_TO_WATCH' | 'WATCHING' | 'COMPLETED' | 'ON_HOLD'
export const watchLabels: Record<WatchStatus, string> = {
  PLAN_TO_WATCH: '想看', WATCHING: '在看', COMPLETED: '已看', ON_HOLD: '搁置',
}
export interface SaveWork {
  favorite: boolean
  wishlist: boolean
  watchStatus: WatchStatus | null
  watchedEpisodes: number
}
export interface SavedWork {
  workId: number
  workType: 'ANIME' | 'GAME'
  title: string
  coverUrl: string | null
  coverAlt: string | null
  episodeCount: number | null
  favorite: 0 | 1
  wishlist: 0 | 1
  watchStatus: WatchStatus | null
  watchedEpisodes: number
}

interface Response<T> { code: string; data: T }
export async function request<T>(config: AxiosRequestConfig) {
  const { data } = await apiClient.request<Response<T>>(config)
  if (data.code !== 'OK') throw new Error('请求未完成')
  return data.data
}

export async function write<T>(method: 'POST' | 'PUT' | 'DELETE', url: string, data?: unknown, options?: Pick<AxiosRequestConfig, 'timeout'>) {
  const proof = await request<{ headerName: string; token: string }>({ url: '/auth/csrf' })
  return request<T>({
    method, url, data, ...options,
    // Keep Axios from overwriting the masked proof with an older readable cookie.
    withXSRFToken: false,
    headers: { [proof.headerName]: proof.token },
  })
}

export const register = (data: Credentials & { displayName: string }) => write<void>('POST', '/auth/register', data)
export const login = (data: Credentials) => write<void>('POST', '/auth/login', data)
export const logout = () => write<void>('POST', '/auth/logout')
export const getProfile = () => request<Profile>({ url: '/me/profile' })
export const updateProfile = (data: { displayName: string; bio: string }) => write<Profile>('PUT', '/me/profile', data)
export const avatarUrl = (key: string) => '/api/avatars/' + encodeURIComponent(key)
export function uploadAvatar(file: File) {
  const data = new FormData()
  data.append('avatar', file)
  return write<Profile>('PUT', '/me/avatar', data, { timeout: 30000 })
}
export const removeAvatar = () => write<Profile>('DELETE', '/me/avatar')
export const listSaved = (signal: AbortSignal) => request<SavedWork[]>({ url: '/me/collection', signal })
export const getSaved = (id: number, signal: AbortSignal) =>
  request<{ item: SavedWork | null }>({ url: '/me/collection/' + id, signal })
export type CollectionSelection = 'FOLLOW' | 'FAVORITE' | 'WISHLIST'
export const selectWork = (id: number, kind: CollectionSelection, selected: boolean) =>
  write<{ item: SavedWork | null }>('PUT', '/me/collection/' + id + '/selection/' + kind, { selected })
export const saveWork = (id: number, data: SaveWork) => write<SavedWork>('PUT', '/me/collection/' + id, data)
export const removeSaved = (id: number) => write<void>('DELETE', '/me/collection/' + id)
export const needsLogin = (error: unknown) => isAxiosError(error) && error.response?.data?.code === 'LOGIN_REQUIRED'
export function accountError(error: unknown) {
  if (isAxiosError(error)) {
    const body = error.response?.data
    return body?.errors?.[0]?.message || body?.message || '无法连接服务，请重试'
  }
  return '无法完成请求，请重试'
}
