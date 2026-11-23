import { siteMediaUrl } from '../api/siteMedia'
import type { Artwork, FeaturedItem, FeaturedPage } from '../reader/bookTypes'
import harborCoverLocal from '../assets/reader/mist-harbor.webp'
import observatoryCoverLocal from '../assets/reader/star-court.webp'
import bambooCoverLocal from '../assets/reader/bamboo-night.webp'
import harborBannerLocal from '../assets/reader/banner-harbor.webp'
import starsBannerLocal from '../assets/reader/banner-stars.webp'
import bambooBannerLocal from '../assets/reader/banner-bamboo.webp'
import placesLocal from '../assets/reader/video-places.webp'
import talesLocal from '../assets/reader/video-tales.webp'
import modernGenresLocal from '../assets/reader/genres-modern.webp'
import classicGenresLocal from '../assets/reader/genres-classic.webp'

const harborCover = siteMediaUrl('reader/mist-harbor.webp', harborCoverLocal)
const observatoryCover = siteMediaUrl('reader/star-court.webp', observatoryCoverLocal)
const bambooCover = siteMediaUrl('reader/bamboo-night.webp', bambooCoverLocal)
const harborBanner = siteMediaUrl('reader/banner-harbor.webp', harborBannerLocal)
const starsBanner = siteMediaUrl('reader/banner-stars.webp', starsBannerLocal)
const bambooBanner = siteMediaUrl('reader/banner-bamboo.webp', bambooBannerLocal)
const places = siteMediaUrl('reader/video-places.webp', placesLocal)
const tales = siteMediaUrl('reader/video-tales.webp', talesLocal)
const modernGenres = siteMediaUrl('reader/genres-modern.webp', modernGenresLocal)
const classicGenres = siteMediaUrl('reader/genres-classic.webp', classicGenresLocal)

export const readerGenres = [
  { id: 'all', label: '全部' },
  { id: 'passion', label: '热血' },
  { id: 'action', label: '战斗' },
  { id: 'romance', label: '恋爱' },
  { id: 'campus', label: '校园' },
  { id: 'daily', label: '日常' },
  { id: 'comedy', label: '搞笑' },
  { id: 'healing', label: '治愈' },
  { id: 'tearjerker', label: '催泪' },
  { id: 'mystery', label: '悬疑' },
  { id: 'detective', label: '推理' },
  { id: 'horror', label: '恐怖' },
  { id: 'fantasy', label: '奇幻' },
  { id: 'adventure', label: '冒险' },
  { id: 'isekai', label: '异世界' },
  { id: 'time-travel', label: '穿越' },
  { id: 'magic', label: '魔法' },
  { id: 'scifi', label: '科幻' },
  { id: 'mecha', label: '机战' },
  { id: 'sports', label: '运动' },
  { id: 'music', label: '音乐' },
  { id: 'idol', label: '偶像' },
  { id: 'history', label: '历史' },
  { id: 'period', label: '古风' },
  { id: 'wuxia', label: '武侠' },
  { id: 'xianxia', label: '仙侠' },
  { id: 'food', label: '美食' },
  { id: 'inspiration', label: '励志' },
  { id: 'workplace', label: '职场' },
  { id: 'ensemble', label: '群像' },
] as const

export type ReaderGenre = (typeof readerGenres)[number]['id']
type VideoGenre = Exclude<ReaderGenre, 'all'>

export interface VideoItem {
  title: string
  duration: string
  artwork: Artwork
  genres: readonly VideoGenre[]
}

export interface VideoPage {
  kind: 'videos'
  number: number
  videos: readonly VideoItem[]
}

interface AnimeSpread {
  left: FeaturedPage
  right: VideoPage
}

const harbor: FeaturedItem = {
  title: '雾港来信',
  description: '潮水退去之前，把来自昨日的信送达。',
  artwork: { src: harborBanner, portrait: harborCover },
}
const stars: FeaturedItem = {
  title: '星庭遗录',
  description: '一颗会移动的星，牵出旧观星台的谜题。',
  artwork: { src: starsBanner, portrait: observatoryCover },
}
const bamboo: FeaturedItem = {
  title: '青岚夜行',
  description: '循着灯火和白狐，走进竹林深处。',
  artwork: { src: bambooBanner, portrait: bambooCover },
}

export const animeFeatures = { harbor, stars, bamboo }

function video(title: string, duration: string, src: string, genres: readonly VideoGenre[], tile?: number): VideoItem {
  return { title, duration, artwork: { src, tile }, genres }
}

// Fictional programs and artwork bundled with the frontend.
export const animeSpreads: readonly AnimeSpread[] = [
  {
    left: { kind: 'featured', number: 1, slides: [harbor, stars, bamboo] },
    right: {
      kind: 'videos',
      number: 2,
      videos: [
        video('雾港来信 · 预告', '02:16', harborBanner, ['mystery', 'fantasy']),
        video('午夜灯塔', '04:28', places, ['mystery', 'adventure'], 0),
        video('邮局里的旧风声', '05:30', places, ['detective', 'mystery'], 1),
        video('一封寄给你的信', '03:12', tales, ['romance', 'fantasy'], 0),
        video('在雾港等你', '06:24', harborCover, ['romance', 'adventure']),
        video('沿绳桥行走', '07:08', tales, ['adventure', 'fantasy'], 1),
      ],
    },
  },
  {
    left: { kind: 'featured', number: 3, slides: [stars, bamboo, harbor] },
    right: {
      kind: 'videos',
      number: 4,
      videos: [
        video('星庭遗录 · 预告', '01:58', starsBanner, ['mystery', 'fantasy']),
        video('极光下的旧塔', '06:12', places, ['adventure', 'fantasy'], 2),
        video('浑仪的暗号', '04:42', tales, ['detective', 'mystery'], 2),
        video('星图上的约定', '08:16', observatoryCover, ['romance', 'fantasy']),
        video('城下的水道', '05:38', places, ['detective', 'adventure'], 3),
        video('修院点灯人', '03:04', tales, ['mystery'], 4),
      ],
    },
  },
  {
    left: { kind: 'featured', number: 5, slides: [bamboo, harbor, stars] },
    right: {
      kind: 'videos',
      number: 6,
      videos: [
        video('青岚夜行 · 预告', '02:09', bambooBanner, ['fantasy', 'adventure']),
        video('暮色里的约定', '08:47', places, ['romance', 'adventure'], 4),
        video('萤火竹桥', '04:32', places, ['fantasy', 'romance'], 5),
        video('白狐与纸灯', '06:10', tales, ['mystery', 'fantasy'], 3),
        video('青岚山门迷踪', '03:56', bambooCover, ['detective', 'adventure']),
        video('红叶下的晚餐', '05:18', tales, ['romance', 'adventure'], 5),
      ],
    },
  },
]

const genreVideos: readonly VideoItem[] = [
  video('接力赛的最后一棒', '04:10', modernGenres, ['sports', 'campus', 'passion', 'inspiration', 'ensemble'], 0),
  video('偶像部的第一场演出', '05:48', modernGenres, ['music', 'idol', 'campus', 'ensemble'], 1),
  video('猫咪面包店', '04:26', modernGenres, ['food', 'daily', 'comedy', 'healing'], 2),
  video('晨光机甲 · 出击', '03:56', modernGenres, ['mecha', 'scifi', 'action', 'passion'], 3),
  video('时钟塔的另一侧', '06:30', modernGenres, ['isekai', 'time-travel', 'magic', 'fantasy', 'adventure'], 4),
  video('向星环起航', '04:12', modernGenres, ['scifi', 'adventure'], 5),
  video('松风桥试剑', '05:20', classicGenres, ['wuxia', 'period', 'action'], 0),
  video('云山问剑', '04:44', classicGenres, ['xianxia', 'period', 'magic', 'action'], 1),
  video('长街灯会', '06:18', classicGenres, ['history', 'period', 'music', 'ensemble', 'food'], 2),
  video('夜半校舍', '05:06', classicGenres, ['horror', 'mystery', 'campus'], 3),
  video('动画工作室的一天', '08:12', classicGenres, ['workplace', 'daily', 'ensemble', 'inspiration'], 4),
  video('末班车到站之前', '05:52', classicGenres, ['tearjerker', 'romance', 'healing'], 5),
]

const genreLabels = new Map(readerGenres.map(({ id, label }) => [id, label] as const))

function normalizeSearchText(value: string) {
  return value.normalize('NFKC').toLowerCase()
}

export function filterReaderSpreads(
  spreads: readonly AnimeSpread[],
  genre: ReaderGenre,
  query = '',
): readonly AnimeSpread[] {
  const terms = normalizeSearchText(query).match(/\S+/g) ?? []
  if (genre === 'all' && terms.length === 0) return spreads

  function isMatch(video: VideoItem) {
    if (genre !== 'all' && !video.genres.includes(genre)) return false
    if (!terms.length) return true

    const keywords = video.genres.map(id => genreLabels.get(id))
    const searchable = normalizeSearchText([video.title, ...keywords].join(' '))
    return terms.every(term => searchable.includes(term))
  }

  const suggestions = [
    ...genreVideos,
    ...spreads.flatMap(spread => spread.right.videos),
  ].filter(isMatch)

  return spreads.map(spread => {
    const matches = spread.right.videos.filter(isMatch)
    let videos = matches.length ? matches : suggestions
    if (terms.length) videos = [...new Set([...matches, ...suggestions])]

    return {
      left: spread.left,
      right: { ...spread.right, videos: videos.slice(0, 6) },
    }
  })
}

export const readerArtSources = [...new Set([
  ...animeSpreads.flatMap(({ left, right }) => [
    ...left.slides.map(item => item.artwork.src),
    ...right.videos.map(item => item.artwork.src),
  ]),
  ...genreVideos.map(item => item.artwork.src),
])]
