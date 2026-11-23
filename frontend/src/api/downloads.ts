import { apiClient } from './client'

export interface GameDownload {
  id: number
  label: string
  platform: string
  url: string
  external: boolean
}

export async function getGameDownloads(workId: number, signal: AbortSignal) {
  const { data } = await apiClient.get<{ code: string; data: GameDownload[] }>('/works/' + workId + '/downloads', { signal })
  if (data.code !== 'OK' || !Array.isArray(data.data)) throw new Error('下载节点暂不可用')
  return data.data
}
