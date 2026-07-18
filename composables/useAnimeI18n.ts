/**
 * 动漫数据中文国际化映射
 * MAL (MyAnimeList) 英文 → 中文
 */

// ═══════════════════════════════════════════════════════════════
// Genre 类型 → 中文 + 糖果色标签类型
// ═══════════════════════════════════════════════════════════════

export const GENRE_MAP: Record<string, { label: string; type: string }> = {
  'Action':          { label: '热血', type: 'action' },
  'Adventure':       { label: '冒险', type: 'adventure' },
  'Comedy':          { label: '搞笑', type: 'daily' },
  'Drama':           { label: '剧情', type: 'healing' },
  'Fantasy':         { label: '奇幻', type: 'fantasy' },
  'Horror':          { label: '恐怖', type: 'horror' },
  'Mystery':         { label: '悬疑', type: 'scifi' },
  'Romance':         { label: '爱情', type: 'romance' },
  'Sci-Fi':          { label: '科幻', type: 'scifi' },
  'Slice of Life':   { label: '日常', type: 'daily' },
  'Sports':          { label: '运动', type: 'action' },
  'Supernatural':    { label: '奇幻', type: 'fantasy' },
  'Thriller':        { label: '悬疑', type: 'scifi' },
  'Psychological':   { label: '心理', type: 'horror' },
  'Ecchi':           { label: '福利', type: 'romance' },
  'Mahou Shoujo':    { label: '魔法', type: 'fantasy' },
  'Mecha':           { label: '机甲', type: 'scifi' },
  'Music':           { label: '音乐', type: 'youth' },
  'School':          { label: '校园', type: 'youth' },
  'Shoujo':          { label: '少女', type: 'romance' },
  'Shounen':         { label: '少年', type: 'action' },
  'Seinen':          { label: '青年', type: 'action' },
  'Josei':           { label: '女性', type: 'romance' },
  'Award Winning':   { label: '获奖', type: 'new' },
  'Gourmet':         { label: '美食', type: 'daily' },
  'Isekai':          { label: '异世界', type: 'fantasy' },
  'Iyashikei':       { label: '治愈', type: 'healing' },
  'Reincarnation':   { label: '转生', type: 'fantasy' },
  'Historical':      { label: '历史', type: 'adventure' },
  'Vampire':         { label: '吸血鬼', type: 'horror' },
  'Boys Love':       { label: '耽美', type: 'romance' },
  'Girls Love':      { label: '百合', type: 'romance' },
  'Harem':           { label: '后宫', type: 'romance' },
  'Reverse Harem':   { label: '逆后宫', type: 'romance' },
  'Martial Arts':    { label: '武术', type: 'action' },
  'Samurai':         { label: '武士', type: 'action' },
  'Detective':       { label: '推理', type: 'scifi' },
  'Parody':          { label: '恶搞', type: 'daily' },
  'Game':            { label: '游戏', type: 'scifi' },
  'Space':           { label: '太空', type: 'scifi' },
  'Military':        { label: '军事', type: 'action' },
  'Police':          { label: '刑侦', type: 'action' },
  'Avant Garde':     { label: '实验', type: 'scifi' },
  'Suspense':        { label: '悬疑', type: 'scifi' },
  'Adult Cast':      { label: '成人向', type: 'action' },
  'Anthropomorphic': { label: '拟人', type: 'fantasy' },
  'CGDCT':           { label: '轻百合', type: 'healing' },
  'Childcare':       { label: '育儿', type: 'healing' },
  'Combat Sports':   { label: '格斗', type: 'action' },
  'Crossdressing':   { label: '女装', type: 'daily' },
  'Delinquents':     { label: '不良', type: 'action' },
  'Educational':     { label: '教育', type: 'daily' },
  'Gag Humor':       { label: '吐槽', type: 'daily' },
  'Gore':            { label: '血腥', type: 'horror' },
  'High Stakes Game':{ label: '博弈', type: 'scifi' },
  'Idols (Female)':  { label: '偶像', type: 'youth' },
  'Idols (Male)':    { label: '偶像', type: 'youth' },
  'Isekai Quartet':  { label: '异世界', type: 'fantasy' },
  'Love Polygon':    { label: '多角恋', type: 'romance' },
  'Magical Sex Shift':{ label: '性转', type: 'fantasy' },
  'Medical':         { label: '医疗', type: 'daily' },
  'Mythology':       { label: '神话', type: 'fantasy' },
  'Organized Crime': { label: '黑帮', type: 'action' },
  'Otaku Culture':   { label: '御宅', type: 'daily' },
  'Performing Arts': { label: '演艺', type: 'youth' },
  'Pets':            { label: '宠物', type: 'healing' },
  'Romantic Subtext':{ label: '轻恋爱', type: 'romance' },
  'Showbiz':         { label: '演艺', type: 'youth' },
  'Strategy Game':   { label: '策略', type: 'scifi' },
  'Super Power':     { label: '超能力', type: 'fantasy' },
  'Survival':        { label: '生存', type: 'adventure' },
  'Team Sports':     { label: '运动', type: 'action' },
  'Time Travel':     { label: '穿越', type: 'fantasy' },
  'Villainess':      { label: '恶役', type: 'fantasy' },
  'Visual Arts':     { label: '美术', type: 'youth' },
  'Workplace':       { label: '职场', type: 'daily' },
  'Urban Fantasy':   { label: '都市奇幻', type: 'fantasy' },
  'Love Status Quo': { label: '恋爱拉扯', type: 'romance' },
  'Kids':            { label: '少儿', type: 'healing' },
  'Video Game':      { label: '电子游戏', type: 'scifi' },
  'Racing':          { label: '竞速', type: 'action' },
}

// ═══════════════════════════════════════════════════════════════
// 类型 英文 → 中文
// ═══════════════════════════════════════════════════════════════

export const TYPE_MAP: Record<string, string> = {
  'TV': 'TV动画',
  'Movie': '剧场版',
  'OVA': 'OVA',
  'ONA': 'ONA',
  'Special': '特别篇',
  'Music': 'MV',
  'CM': '广告',
  'PV': '预告片',
}

// ═══════════════════════════════════════════════════════════════
// 状态 英文 → 中文
// ═══════════════════════════════════════════════════════════════

export const STATUS_MAP: Record<string, string> = {
  'Finished Airing': '已完结',
  'Currently Airing': '连载中',
  'Not yet aired': '未开播',
}

// ═══════════════════════════════════════════════════════════════
// 糖果色标签 class 映射
// ═══════════════════════════════════════════════════════════════

export const TAG_CLASS_MAP: Record<string, string> = {
  fantasy:   'tag-fantasy',
  romance:   'tag-romance',
  action:    'tag-action',
  healing:   'tag-healing',
  scifi:     'tag-scifi',
  youth:     'tag-youth',
  adventure: 'tag-adventure',
  daily:     'tag-daily',
  horror:    'tag-horror',
  new:       'tag-new',
}

// ═══════════════════════════════════════════════════════════════
// 工具函数
// ═══════════════════════════════════════════════════════════════

/** 将英文 genre 数组转为中文标签（最多 3 个） */
export function mapGenres(genres: string[]) {
  return (genres ?? [])
    .map(g => GENRE_MAP[g] ?? { label: g, type: 'fantasy' })
    .slice(0, 3)
}

/** 获取最佳的显示标题：中文 > 日文 > 罗马音 */
export function displayTitle(a: { titleCn?: string; titleJapanese?: string; title?: string }): string {
  return a.titleCn || a.titleJapanese || a.title || ''
}

/** 获取最佳简介：中文 > 英文 */
export function displaySynopsis(a: { synopsisCn?: string; synopsis?: string }): string {
  return a.synopsisCn || a.synopsis || ''
}

/** 获取站内用户评分：没有用户评分时显示 0，不回退到抓取评分 */
export function communityScore(a: { communityMeanRating?: number; communityRatingCount?: number }): number {
  if (!a.communityRatingCount) return 0
  return Math.round((a.communityMeanRating ?? 0) * 10) / 10
}

/** 翻译类型 */
export function tType(type?: string): string {
  return type ? TYPE_MAP[type] || type : ''
}

/** 翻译状态 */
export function tStatus(status?: string): string {
  return status ? STATUS_MAP[status] || status : ''
}

/** 获取标签 class */
export function tagClass(type: string): string {
  return TAG_CLASS_MAP[type] || 'tag-fantasy'
}

/** 格式化数字 */
export function formatNumber(n?: number): string {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
