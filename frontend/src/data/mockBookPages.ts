import type { BookSpread, FeaturedPage } from '../reader/bookTypes'
import { animeFeatures } from './animeReader'
import { games } from './gameReader'

export const communitySpreads: readonly BookSpread[] = [
  {
    left: { kind: 'notes', number: 1, title: '馆内公告', layout: 'notice', entries: [
      { title: '周五放映会征集', text: '本期想看雨夜题材。说说你心中的那部作品，也给还没看过的馆友留一点惊喜。', label: '置顶 · 活动', detail: '值班馆员 · 24 条回复' },
      { title: '讨论前请标记剧透', text: '提到关键情节时，请在标题里标出集数或章节。', label: '置顶 · 说明', detail: '值班馆员' },
    ] },
    right: { kind: 'notes', number: 2, title: '热门讨论', layout: 'threads', entries: [
      { title: '《雾港来信》的灯塔响了几次？', text: '重看第三集才发现，邮差到岸前的钟声少了一下。', label: '番剧讨论', detail: '潮汐信差 · 18 条回复' },
      { title: '分享一下我的浮岛航线', text: '傍晚从白帆港起飞，西边有一段特别适合拍照。', label: '游戏心得', detail: '纸鸢旅人 · 11 条回复' },
      { title: '求适合周末看完的治愈番', text: '偏爱短篇和自然风景，慢节奏也可以。', label: '求推荐', detail: '花房守夜人 · 9 条回复' },
    ] },
  },
  {
    left: { kind: 'notes', number: 3, title: '同人创作', layout: 'threads', entries: [
      { title: '画了一张星庭的旧车票', text: '借用了观星台的黄铜配色，背面还留了一小行暗号。', label: '插画', detail: '铜墨 · 12 条回复' },
      { title: '雾港地图重绘', text: '照着各集背景，把灯塔、邮局和旧码头补到了同一张图上。', label: '同人资料', detail: '临海抄图员 · 8 条回复' },
    ] },
    right: { kind: 'notes', number: 4, title: '新留言', layout: 'threads', entries: [
      { title: '《青岚夜行》片尾乐器考据', text: '竹林那段独奏听起来像箫，也可能混了排笛。', label: '考据讨论', detail: '听风记 · 6 条回复' },
      { title: '苔光温室的种子放在哪？', text: '第一年的秋日市集漏看了，看能不能在冬天补上。', label: '游戏问答', detail: '小栗枝 · 4 条回复' },
      { title: '想加入周五放映会', text: '第一次参加，打算从第三集开始补。', label: '馆友交流', detail: '月白 · 3 条回复' },
    ] },
  },
]

const { harbor, stars, bamboo } = animeFeatures

const recommendationFeatures: readonly FeaturedPage[] = [
  { kind: 'featured', number: 1, title: '馆员封面荐读', slides: [
    { ...stars, description: '周末解谜，从一张被修改过的星图开始。' },
    { ...games[2], description: '本周游戏精选，跟着水流读懂地下古城。' },
    { ...harbor, description: '重看前三集，再去社区找馆友绘制的雾港地图。' },
  ] },
  { kind: 'featured', number: 3, title: '雨夜封面', slides: [
    { ...harbor, description: '关掉屋灯，听雾港的潮水和钟声。' },
    { ...games[3], description: '在玻璃屋听雨，照料一株会发光的花。' },
    { ...bamboo, description: '雨沿着屋檐落下，竹林里还有一段路。' },
  ] },
]

export const recommendationArtSources = [...new Set(recommendationFeatures.flatMap(({ slides }) =>
  slides.flatMap(({ artwork }) => artwork.portrait ? [artwork.src, artwork.portrait] : [artwork.src]),
))]

export const recommendationSpreads: readonly BookSpread[] = [
  {
    left: recommendationFeatures[0],
    right: { kind: 'notes', number: 2, title: '本周书单', layout: 'recommendations', entries: [
      { title: '星庭遗录', text: '喜欢细节推理，可以留意画面边缘反复出现的旧星图。', label: '番剧 · 悬疑', detail: '周末连看' },
      { title: '空潮遗迹', text: '从水位变化读懂房间结构，适合享受慢慢破解机关的玩家。', label: '游戏 · 解谜', detail: '适合单人' },
      { title: '雾港地图重绘', text: '看完前三集再翻这张图，能串起镜头里几处一闪而过的地标。', label: '社区 · 同人资料', detail: '含前三集剧透' },
    ] },
  },
  {
    left: recommendationFeatures[1],
    right: { kind: 'notes', number: 4, title: '雨夜特刊', layout: 'recommendations', entries: [
      { title: '雾港来信', text: '从灯塔的钟声开始，听一封迟到多年的信。', label: '番剧 · 奇幻', detail: '适合雨夜' },
      { title: '苔光温室', text: '玻璃屋外雷声不断，屋里可以慢慢挑选要种的花。', label: '游戏 · 经营', detail: '轻松游玩' },
      { title: '青岚夜行', text: '竹雨、石阶和一盏灯，适合喜欢古风氛围的读者。', label: '番剧 · 冒险', detail: '短篇' },
    ] },
  },
]

export const personalSpreads: readonly BookSpread[] = [
  {
    left: { kind: 'collection', number: 1, title: '继续观看', layout: 'progress', entries: [
      { title: '雾港来信', kind: '番剧', note: '上次停在邮差抵达灯塔。', progress: { current: 3, total: 12, label: '第 3 / 12 集' } },
      { title: '星庭遗录', kind: '番剧', note: '观星台的星图换了位置。', progress: { current: 2, total: 8, label: '第 2 / 8 集' } },
      { title: '青岚夜行', kind: '番剧', note: '离开山门之后。', progress: { current: 5, total: 10, label: '第 5 / 10 集' } },
    ] },
    right: { kind: 'collection', number: 2, title: '稍后再看', layout: 'list', entries: [
      { title: '午夜灯塔', kind: '视频', note: '雾港来信 · 背景短片' },
      { title: '云海航团', kind: '游戏', note: '浮岛探险 · PC、PS5' },
      { title: '青岚夜行片尾考据', kind: '帖子', note: '配乐和乐器讨论' },
    ] },
  },
  {
    left: { kind: 'collection', number: 3, title: '游戏手记', layout: 'progress', entries: [
      { title: '余火守夜人', kind: '游戏', note: '下次从黑曜石城门继续。', progress: { current: 4, total: 9, label: '已探索 4 / 9 处区域' } },
      { title: '苔光温室', kind: '游戏', note: '记得照料夜光睡莲。', progress: { current: 7, total: 18, label: '已收集 7 / 18 种植物' } },
    ] },
    right: { kind: 'collection', number: 4, title: '收藏剪报', layout: 'list', entries: [
      { title: '雾港地图重绘', kind: '帖子', note: '方便重看时查地标。' },
      { title: '周五放映会征集', kind: '活动', note: '想参加下次的雨夜主题。' },
      { title: '雨夜特刊', kind: '书单', note: '周末有空慢慢看。' },
    ] },
  },
]
