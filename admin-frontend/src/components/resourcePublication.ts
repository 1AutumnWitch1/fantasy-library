import type { ResourceChange } from '../api/admin'

export function confirmResourceRemoval(label: string) {
  return window.confirm('永久删除「' + label + '」的资源记录？对应的站内文件若没有其他引用，也会从 MinIO 删除。')
}

export function resourceChangeMessage(result: ResourceChange) {
  switch (result) {
    case 'PUBLISHED': return '资源已上架。'
    case 'REMOVED': return '资源已删除。'
    case 'SHARED_FILE_RETAINED': return '记录已删除。文件仍被其他资源引用，已保留。'
    case 'CLEANUP_PENDING': return '记录已删除。MinIO 文件等待自动重试清理。'
  }
}
