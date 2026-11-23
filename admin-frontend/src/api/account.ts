import { isAxiosError, type AxiosRequestConfig } from 'axios'
import { apiClient } from './client'

export interface Profile { id: number; username: string; displayName: string; bio: string | null; accountRole: 'MEMBER' | 'ADMIN'; avatarKey: string | null }
export interface Credentials { username: string; password: string }
interface Response<T> { code: string; data: T }
async function request<T>(config: AxiosRequestConfig) {
  const { data } = await apiClient.request<Response<T>>(config)
  if (data.code !== 'OK') throw new Error('请求未完成')
  return data.data
}

type WriteOptions = Pick<AxiosRequestConfig, 'timeout' | 'signal' | 'onUploadProgress'> & { contentType?: string; fileName?: string }

export async function write<T>(method: 'POST' | 'PUT' | 'DELETE', url: string, data?: unknown, options?: WriteOptions) {
  const { contentType, fileName, ...settings } = options ?? {}
  const proof = await request<{ headerName: string; token: string }>({ url: '/admin/auth/csrf', signal: settings.signal })
  return request<T>({
    method, url, data, ...settings,
    // Keep Axios from overwriting the masked proof with an older readable cookie.
    withXSRFToken: false,
    headers: { ...(contentType ? { 'Content-Type': contentType } : {}),
      ...(fileName ? { 'X-File-Name': encodeURIComponent(fileName) } : {}), [proof.headerName]: proof.token },
  })
}

export const login = (data: Credentials) => write<void>('POST', '/admin/auth/login', data)
export const logout = () => write<void>('POST', '/admin/auth/logout')
export const getProfile = () => request<Profile>({ url: '/admin/auth/profile' })
export const needsLogin = (error: unknown) => isAxiosError(error) && error.response?.data?.code === 'LOGIN_REQUIRED'
export function accountError(error: unknown) {
  if (isAxiosError(error)) {
    const body = error.response?.data
    return body?.errors?.[0]?.message || body?.message || '无法连接服务，请重试'
  }
  return '无法完成请求，请重试'
}
