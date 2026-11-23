import { apiClient } from './client'
import { write } from './account'

export const topics = {
  GENERAL: '馆内闲谈', ANIME: '追番讨论', GAME: '游戏讨论', GUIDE: '攻略与资料',
} as const
export type Topic = keyof typeof topics
export type Target = 'POST' | 'COMMENT'
export interface Page<T> { items: T[]; page: number; size: number; total: number; totalPages: number }
export interface Post {
  id: number; authorId: number; authorName: string; workId: number | null; workTitle: string | null
  topic: Topic; title: string; body: string; createdAt: string; updatedAt: string
  likeCount: number; commentCount: number; viewCount: number; previewImage: string | null; avatarKey: string | null
}
export interface PostDetail { post: Post; images: string[] }
export interface Comment {
  id: number; postId: number; parentId: number | null; replyTo: string | null
  authorId: number; authorName: string; body: string; createdAt: string; avatarKey: string | null
}
export interface ScreeningComment {
  id: number; rootId: number | null; parentId: number | null; replyTo: string | null
  authorName: string | null; avatarKey: string | null; body: string | null; createdAt: string; removed: boolean
  replyCount: number; likeCount: number; liked: boolean
}
export interface ScreeningThread { comment: ScreeningComment; replies: ScreeningComment[] }

export interface Draft { title: string; body: string; topic: Topic; workId: number | null; keepImages: string[] }
export interface Reaction { liked: boolean; count: number }
interface Created { id: number }

async function read<T>(url: string, params?: object, signal?: AbortSignal): Promise<T> {
  const { data } = await apiClient.get<{ code: string; data: T }>(url, { params, signal })
  if (data.code !== 'OK') throw new Error('社区暂不可用')
  return data.data
}

export const listPosts = (page: number, topic?: Topic, workId?: number, signal?: AbortSignal) =>
  read<Page<Post>>('/community/posts', { page, topic, workId, size: 6 }, signal)
export const listPopularPosts = (topic?: Topic, workId?: number, signal?: AbortSignal) =>
  read<Page<Post>>('/community/posts', { page: 1, topic, workId, size: 3, sort: 'HOT' }, signal)
export const getPost = (id: number, signal?: AbortSignal, countView = false) =>
  read<PostDetail>('/community/posts/' + id, countView ? { view: true } : undefined, signal)
export const listComments = (id: number, page: number, signal?: AbortSignal) =>
  read<Page<Comment>>('/community/posts/' + id + '/comments', { page }, signal)
const screeningPath = (id: number, member: boolean) => (member ? '/me' : '') + '/works/' + id
export const listScreeningThreads = (id: number, page: number, member: boolean, signal: AbortSignal) =>
  read<Page<ScreeningThread>>(screeningPath(id, member) + '/comment-threads', { page }, signal)
export const listScreeningReplies = (id: number, root: number, page: number, member: boolean, signal: AbortSignal) =>
  read<Page<ScreeningComment>>(screeningPath(id, member) + '/comments/' + root + '/replies', { page }, signal)
const memberPost = (id: number) => '/me/community/posts/' + id
export const mediaUrl = (key: string) => '/api/community/media/' + encodeURIComponent(key)

function upload(draft: Draft, files: File[]) {
  const data = new FormData()
  data.append('post', new Blob([JSON.stringify(draft)], { type: 'application/json' }))
  files.forEach(file => data.append('images', file))
  return data
}

export const publish = (draft: Draft, files: File[]) => write<Created>('POST', '/me/community/posts', upload(draft, files), { timeout: 60000 })
export const edit = (id: number, draft: Draft, files: File[]) => write<void>('PUT', memberPost(id), upload(draft, files), { timeout: 60000 })
export const deletePost = (id: number) => write<void>('DELETE', memberPost(id))
export const addComment = (id: number, body: string, parentId: number | null) =>
  write<Created>('POST', memberPost(id) + '/comments', { body, parentId })
export const addScreeningComment = (id: number, body: string, parentId: number | null) =>
  write<Created>('POST', '/me/works/' + id + '/comments', { body, parentId })
export const likeScreeningComment = (work: number, id: number, liked: boolean) =>
  write<Reaction>('PUT', screeningPath(work, true) + '/comments/' + id + '/like', { liked })
export const getReaction = (id: number, signal?: AbortSignal) => read<Reaction>(memberPost(id) + '/like', undefined, signal)
export const setReaction = (id: number, liked: boolean) =>
  write<Reaction>(liked ? 'PUT' : 'DELETE', memberPost(id) + '/like')
export const reportContent = (targetType: Target, targetId: number, reason: string) =>
  write<void>('POST', '/me/community/reports', { targetType, targetId, reason })

export function readId(value: unknown) {
  if (typeof value !== 'string' || !/^[1-9][0-9]*$/.test(value)) return
  const id = Number(value)
  return Number.isSafeInteger(id) ? id : undefined
}
export const isTopic = (value: unknown): value is Topic => typeof value === 'string' && Object.hasOwn(topics, value)
export const communityLink = (workId?: number, topic?: Topic) => ({
  name: 'book', params: { bookId: 'community' }, query: { workId: workId?.toString(), topic },
})
export const dateLabel = (value: string) =>
  new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
