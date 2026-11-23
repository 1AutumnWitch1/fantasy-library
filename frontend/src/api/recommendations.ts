import { apiClient } from './client'
import type { NamedItem, Work } from './catalog'
import type { Post } from './community'

export type RecommendationType = Work['type']
export type RecommendationMode = 'PERSONALIZED' | 'POPULAR' | 'EDITORIAL' | 'EMPTY'

export interface RecommendationWork {
  work: Work
  tags: NamedItem[]
  addedAt: string | null
}

export interface RecommendationFeed {
  mode: RecommendationMode
  forYou: { entry: RecommendationWork; reason: string }[]
  popular: { entry: RecommendationWork; heat: WorkHeat }[]
  newest: RecommendationWork[]
  discussions: Post[]
}

export interface WorkHeat {
  workId: number
  score: number
  favoriteCount: number
  wishlistCount: number
  commentCount: number
  likeCount: number
  viewCount: number
}

export async function readRecommendations(type: RecommendationType | null, member: boolean, signal: AbortSignal) {
  const { data } = await apiClient.get<{ code: string; data: RecommendationFeed }>(
    member ? '/me/recommendations' : '/recommendations',
    { params: { type: type ?? undefined }, signal },
  )
  if (data.code !== 'OK' || !data.data) throw new Error('无法读取推荐')
  return data.data
}
