import { write } from './account'
import { apiClient } from './client'
import type { Option } from './editorial'

export interface DownloadDraft {
  platformId: number
  label: string
  externalUrl: string | null
  fileName: string | null
  listed: boolean
}

export interface DownloadEntry extends DownloadDraft { id: number }
interface Archive { platforms: Option[]; nodes: DownloadEntry[] }

const path = (workId: number) => '/admin/library/works/' + workId + '/downloads'

export async function loadDownloadArchive(workId: number, signal: AbortSignal) {
  const { data } = await apiClient.get<{ code: string; data: Archive }>(path(workId), { signal })
  if (data.code !== 'OK' || !data.data) throw new Error('无法读取下载节点')
  return data.data
}

export function saveDownload(workId: number, nodeId: number | undefined, draft: DownloadDraft) {
  return write<void>(nodeId ? 'PUT' : 'POST', path(workId) + (nodeId ? '/' + nodeId : ''), draft)
}
