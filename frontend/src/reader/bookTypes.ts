import type { Post, Topic } from '../api/community'
import type { SavedWork } from '../api/account'
import type { Notice, WatchEvent } from '../api/personal'
import type { CatalogKind, WorkItem } from '../api/catalog'
import type { RecommendationFeed, RecommendationMode } from '../api/recommendations'
import type { VideoPage } from '../data/animeReader'
import type { ShelfBook } from '../data/shelfBooks'

export interface Artwork {
  src: string
  tile?: number
  portrait?: string
}

export interface FeaturedItem {
  workId?: number
  title: string
  description: string
  artwork: Artwork
}

export interface FeaturedPage {
  kind: 'featured'
  number: number
  title?: string
  slides: readonly FeaturedItem[]
}

interface TitledPage {
  number: number
  title: string
}

export interface GameEntry {
  title: string
  description: string
  genre: string
  tags: readonly string[]
  platforms: readonly string[]
  artwork: Artwork
}

export interface GamePage extends TitledPage {
  kind: 'game'
  total: number
  entries: readonly GameEntry[]
}

export interface ReaderNote {
  title: string
  text: string
  label: string
  detail: string
}

export interface NotesPage extends TitledPage {
  kind: 'notes'
  layout: 'notice' | 'threads' | 'recommendations'
  entries: readonly ReaderNote[]
}

export interface CollectionEntry {
  title: string
  kind: string
  note: string
  progress?: { current: number; total: number; label: string }
}

export interface CollectionPage extends TitledPage {
  kind: 'collection'
  layout: 'progress' | 'list'
  entries: readonly CollectionEntry[]
}

export interface CommunityPage extends TitledPage {
  kind: 'community'
  items: Post[]
  topic?: Topic
  workId?: number
  message: string
}

export type PersonalPage = TitledPage & {
  kind: 'personal'
  guest: boolean
  message: string
} & (
  { section: 'anime' | 'collection'; items: SavedWork[] }
  | { section: 'history'; events: WatchEvent[] }
  | { section: 'inbox'; notices: Notice[]; unread: number }
)

export interface CataloguePage extends TitledPage {
  kind: 'catalogue'
  catalog: CatalogKind
  items: WorkItem[]
  message: string
}

export type RecommendationPage = TitledPage & {
  kind: 'recommendations'
  status: 'loading' | 'ready' | 'error'
} & (
  { section: 'picks'; mode: RecommendationMode; items: RecommendationFeed['forYou']; guest: boolean }
  | { section: 'popular'; items: RecommendationFeed['popular'] }
  | { section: 'newest'; items: RecommendationFeed['newest'] }
  | { section: 'discussions'; items: RecommendationFeed['discussions'] }
)

export type BookPageData = RecommendationPage | CommunityPage | PersonalPage | CataloguePage | FeaturedPage | VideoPage | GamePage | NotesPage | CollectionPage

export interface BookSpread {
  left: BookPageData
  right: BookPageData
}

export interface BookDefinition {
  spine: ShelfBook
  coverTitle: string
  theme: { light: string; leather: string; shade: string }
  spreads: readonly BookSpread[]
  artSources?: readonly string[]
  catalog?: CatalogKind
}
