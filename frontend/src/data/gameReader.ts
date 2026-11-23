import { siteMediaUrl } from '../api/siteMedia'
import coversLocal from '../assets/reader/mock-game-covers.webp'
import extraLocal from '../assets/reader/game-genres.webp'
import modernLocal from '../assets/reader/genres-modern.webp'
import classicLocal from '../assets/reader/genres-classic.webp'
import type { BookSpread, GameEntry, GamePage } from '../reader/bookTypes'

const covers = siteMediaUrl('reader/mock-game-covers.webp', coversLocal)
const extra = siteMediaUrl('reader/game-genres.webp', extraLocal)
const modern = siteMediaUrl('reader/genres-modern.webp', modernLocal)
const classic = siteMediaUrl('reader/genres-classic.webp', classicLocal)

const labels = [
  '动作', '角色扮演', '冒险', '开放世界', '射击', '策略', '模拟经营', '解谜',
  'FPS', 'TPS', 'MOBA', 'MMORPG', '回合制', '即时战略', '战棋', '卡牌',
  '格斗', '竞速', '体育', '音乐节奏', '平台跳跃', '生存', '沙盒', '建造',
  '种田', '肉鸽', '类魂', '地牢', '恐怖', '视觉小说', '恋爱', '剧情',
  '养成', '探索', '休闲', '派对', '单人', '多人联机', '多人合作', '竞技',
]

export const gameGenres = [
  { id: 'all', label: '全部' },
  ...labels.map(label => ({ id: label, label })),
]

export const games: readonly GameEntry[] = [
  {
    title: '余火守夜人', description: '提灯穿过黑曜石废城，守住王国最后一处火种。',
    genre: '动作角色扮演', platforms: ['PC', 'PS5'],
    tags: ['动作', '角色扮演', '类魂', '单人'], artwork: { src: covers, tile: 0 },
  },
  {
    title: '北境驿道', description: '冒着风雪翻越吊桥，赶在封山前把信送到。',
    genre: '公路冒险', platforms: ['PC', 'Switch'],
    tags: ['冒险', '生存', '探索', '单人'], artwork: { src: covers, tile: 1 },
  },
  {
    title: '空潮遗迹', description: '用古代机关改变地下水位，寻找失落的晶石。',
    genre: '地牢解谜', platforms: ['PC', 'Xbox Series X|S'],
    tags: ['解谜', '冒险', '地牢', '探索'], artwork: { src: covers, tile: 2 },
  },
  {
    title: '苔光温室', description: '收集山野种子，在玻璃屋里照料可以发光的花。',
    genre: '休闲经营', platforms: ['PC', 'Switch'],
    tags: ['种田', '模拟经营', '休闲', '建造'], artwork: { src: covers, tile: 3 },
  },
  {
    title: '云海航团', description: '与好友驾驶飞艇往来于浮岛之间，搭建自己的空港。',
    genre: '开放世界探索', platforms: ['PC', 'PS5'],
    tags: ['开放世界', '沙盒', '建造', '多人合作', '探索'], artwork: { src: covers, tile: 4 },
  },
  {
    title: '秋炉食堂', description: '在晚餐高峰和好友分工，为归来的冒险者准备炖菜。',
    genre: '合作烹饪', platforms: ['PC', 'Switch'],
    tags: ['模拟经营', '休闲', '多人合作', '派对'], artwork: { src: covers, tile: 5 },
  },
  {
    title: '赤砂拉力赛', description: '沿着沙海驿道漂移，在最后一个发卡弯抢下领先位置。',
    genre: '奇幻拉力竞速', platforms: ['PC', 'PS5', 'Xbox Series X|S'],
    tags: ['竞速', '体育', '竞技', '多人联机'], artwork: { src: extra, tile: 0 },
  },
  {
    title: '暮河战线', description: '组建五人小队，沿三条河道推进，击破对方的水晶塔。',
    genre: '多人在线战术竞技', platforms: ['PC', 'Android', 'iOS'],
    tags: ['MOBA', '策略', '竞技', '多人联机'], artwork: { src: extra, tile: 1 },
  },
  {
    title: '黄铜棋局', description: '构筑战牌，在棋盘地城中指挥骑士逐格突围。',
    genre: '回合制卡牌战棋', platforms: ['PC', 'Switch'],
    tags: ['策略', '战棋', '卡牌', '回合制', '肉鸽'], artwork: { src: extra, tile: 2 },
  },
  {
    title: '灯影对决', description: '在屋脊的灯火间拆招，挑战各地擂台的守擂人。',
    genre: '横版格斗', platforms: ['PC', 'PS5'],
    tags: ['格斗', '动作', '竞技', '多人联机'], artwork: { src: extra, tile: 3 },
  },
  {
    title: '零号星港', description: '从第一人称视角护送队友穿过封锁线，在警报响起前撤离。',
    genre: '第一人称射击', platforms: ['PC', 'Xbox Series X|S'],
    tags: ['射击', 'FPS', '生存', '多人合作'], artwork: { src: extra, tile: 4 },
  },
  {
    title: '星弦步旅', description: '跟随弓弦的拍点，在月光照亮的浮石间跳跃。',
    genre: '节奏平台冒险', platforms: ['PC', 'Switch', 'iOS'],
    tags: ['音乐节奏', '平台跳跃', '冒险', '休闲'], artwork: { src: extra, tile: 5 },
  },
  {
    title: '逆风接力', description: '训练校队，掌握交接棒时机，与好友比拼最后一百米。',
    genre: '田径运动', platforms: ['PC', 'Switch'],
    tags: ['体育', '竞技', '养成', '派对'], artwork: { src: modern, tile: 0 },
  },
  {
    title: '机甲防线', description: '切换肩后视角拦截敌机，部署防御塔掩护工程队。',
    genre: '机甲防守射击', platforms: ['PC', 'PS5'],
    tags: ['射击', 'TPS', '塔防', '即时战略', '多人合作'], artwork: { src: modern, tile: 3 },
  },
  {
    title: '九霄灵契', description: '拜入山门，与同服伙伴结队挑战云海深处的秘境。',
    genre: '大型多人在线角色扮演', platforms: ['PC', 'Android'],
    tags: ['MMORPG', '角色扮演', '开放世界', '多人联机', '养成'], artwork: { src: classic, tile: 1 },
  },
  {
    title: '零点回廊', description: '夜巡校舍，借走廊回声破解门锁，躲开巡游的影子。',
    genre: '悬疑逃生', platforms: ['PC', 'Switch'],
    tags: ['恐怖', '解谜', '生存', '剧情'], artwork: { src: classic, tile: 3 },
  },
  {
    title: '月台来信', description: '在末班车到站前选择回信，解开两个人错过的往事。',
    genre: '恋爱视觉小说', platforms: ['PC', 'Switch', 'iOS'],
    tags: ['恋爱', '视觉小说', '剧情', '单人'], artwork: { src: classic, tile: 5 },
  },
  {
    title: '星环拓荒记', description: '采集小行星上的材料，建设一个能够自给自足的空间站。',
    genre: '太空沙盒生存', platforms: ['PC', 'Xbox Series X|S'],
    tags: ['沙盒', '生存', '建造', '开放世界'], artwork: { src: modern, tile: 5 },
  },
  {
    title: '逆时齿轮', description: '在每次重置的钟楼里拾取新齿轮，组合技能闯过机关。',
    genre: '肉鸽平台动作', platforms: ['PC', 'Switch'],
    tags: ['肉鸽', '动作', '平台跳跃', '地牢'], artwork: { src: modern, tile: 4 },
  },
  {
    title: '长街百业', description: '规划灯会摊位、招募艺人，为旧城搭起一年一度的大集。',
    genre: '古城规划经营', platforms: ['PC', 'Android', 'iOS'],
    tags: ['模拟经营', '建造', '策略', '休闲'], artwork: { src: classic, tile: 2 },
  },
]

export const gameArtSources = [...new Set(games.map(game => game.artwork.src))]
const aliases: Record<string, string> = {
  '角色扮演': 'RPG', '动作': 'ACT', '肉鸽': 'Roguelike Roguelite', '即时战略': 'RTS',
  '音乐节奏': '音游', '视觉小说': 'AVG 文字冒险', '战棋': 'SRPG', '模拟经营': 'SIM',
  'FPS': '第一人称射击', 'TPS': '第三人称射击', 'MMORPG': '大型多人在线 网游',
  'MOBA': '多人在线战术竞技', '多人合作': '联机合作 co-op', '平台跳跃': '平台动作',
}

function makeSpreads(entries: readonly GameEntry[]): BookSpread[] {
  const total = entries.length
  const spreads: BookSpread[] = []
  for (let start = 0; start < Math.max(1, total); start += 4) {
    const group = entries.slice(start, start + 4)
    const split = Math.ceil(group.length / 2)
    const number = spreads.length * 2 + 1
    const left: GamePage = { kind: 'game', number, title: '游戏图鉴', total, entries: group.slice(0, split) }
    const right: GamePage = { kind: 'game', number: number + 1, title: '馆藏检索', total, entries: group.slice(split) }
    spreads.push({ left, right })
  }
  return spreads
}

export const gameSpreads = makeSpreads(games)

function normalize(text: string) {
  return text.normalize('NFKC').toLowerCase()
}

export function filterGames(tag: string, query: string): readonly BookSpread[] {
  const terms = normalize(query).match(/\S+/g) ?? []
  if (tag === 'all' && !terms.length) return gameSpreads

  return makeSpreads(games.filter(game => {
    if (tag !== 'all' && !game.tags.includes(tag)) return false
    const fields = [game.title, game.genre, ...game.platforms, ...game.tags, ...game.tags.map(id => aliases[id] ?? '')]
    const searchable = normalize(fields.join(' '))
    return terms.every(term => searchable.includes(term))
  }))
}
