import { write } from './account'
import { apiClient } from './client'
import { dateLabel, topics, type Page, type Topic } from './community'

export type AdminTab = 'works' | 'posts' | 'comments' | 'members'
export type CommentScope = 'SCREENING' | 'COMMUNITY'
export type WorkType = 'ANIME' | 'GAME'
export type NodeKind = 'PLAYBACK' | 'DOWNLOAD'

export interface AdminRow {
  id: number
  title: string
  caption: string
  listed?: boolean
  protected?: boolean
  body?: string
  coverUrl?: string | null
  slug?: string
}
interface Work { id: number; title: string; type: WorkType; listed: boolean; slug: string; coverUrl: string | null }
interface Post { id: number; title: string; author: string; topic: Topic; listed: boolean; createdAt: string }
interface Comment {
  id: number; postTitle: string; scope: CommentScope; workTitle: string | null
  author: string; body: string; createdAt: string
}
interface Member { id: number; username: string; displayName: string; role: 'ADMIN' | 'MEMBER' }
export interface SourceNode { id: number; kind: NodeKind; label: string; context: string; listed: boolean }
const prefix = '/admin/library'

async function read<T>(path: string, signal: AbortSignal, params?: object) {
  const { data } = await apiClient.get<{ code: string; data: T }>(prefix + path, { signal, params })
  if (data.code !== 'OK' || data.data == null) throw new Error('无法调阅管理档案')
  return data.data
}

function rows<T>(page: Page<T>, row: (item: T) => AdminRow): Page<AdminRow> {
  return { ...page, items: page.items.map(row) }
}

export async function listAdminRows(tab: AdminTab, type: WorkType, q: string, page: number,
                                    signal: AbortSignal, listed?: boolean, scope?: CommentScope): Promise<Page<AdminRow>> {
  const params = { q: q || undefined, page }
  switch (tab) {
    case 'works':
      return rows(await read<Page<Work>>('/works', signal, { ...params, type, listed }), work => ({
        id: work.id, title: work.title, caption: work.type === 'GAME' ? '游戏' : '番剧', listed: work.listed,
        slug: work.slug, coverUrl: work.coverUrl,
      }))
    case 'posts':
      return rows(await read<Page<Post>>('/posts', signal, params), post => ({
        id: post.id, title: post.title, caption: post.author + ' · ' + topics[post.topic] + ' · ' + dateLabel(post.createdAt),
        listed: post.listed,
      }))
    case 'comments':
      return rows(await read<Page<Comment>>('/comments', signal, { ...params, scope }), comment => ({
        id: comment.id, title: comment.scope === 'SCREENING' ? comment.workTitle ?? comment.postTitle : comment.postTitle,
        caption: [comment.scope === 'SCREENING' ? '番剧评论' : '社区评论', comment.author, dateLabel(comment.createdAt)].join(' · '),
        body: comment.body,
      }))
    case 'members':
      return rows(await read<Page<Member>>('/members', signal, params), member => ({
        id: member.id, title: member.displayName,
        caption: '@' + member.username,
        protected: member.role === 'ADMIN',
      }))
  }
}

export const getAdminNodes = (id: number, signal: AbortSignal) => read<SourceNode[]>('/works/' + id + '/nodes', signal)
export const getForum = (signal: AbortSignal) => read<{ open: boolean }>('/forum', signal)
export const setForumOpen = (open: boolean) => write<void>('PUT', prefix + '/forum', { open })
export const setItemListed = (tab: 'works' | 'posts', id: number, listed: boolean) =>
  write<void>('PUT', prefix + '/' + tab + '/' + id + '/publication', { listed })
export type ResourceChange = 'PUBLISHED' | 'REMOVED' | 'SHARED_FILE_RETAINED' | 'CLEANUP_PENDING'
export const setNodeListed = (node: Pick<SourceNode, 'id' | 'kind'>, listed: boolean, confirmRemoval = false) =>
  write<ResourceChange>('PUT', prefix + '/nodes/' + node.kind + '/' + node.id + '/publication', { listed, confirmRemoval })
export const removeAdminRow = (tab: 'comments' | 'members', id: number) =>
  write<void>('DELETE', prefix + '/' + tab + '/' + id)
export const addAdminMember = (data: { username: string; displayName: string; password: string }) =>
  write<void>('POST', prefix + '/members', data)
