export interface AnimeGenreOption {
  label: string
  genre: string
  icon: string
}

export interface AnimeFilterOption {
  label: string
  value: string
}

export interface AnimeYearOption {
  label: string
  from: number | null
  to: number | null
}

export const ANIME_GENRE_OPTIONS: AnimeGenreOption[] = [
  { label: '全部', genre: '', icon: '✦' },
  { label: '热血', genre: 'Action', icon: '⚡' },
  { label: '冒险', genre: 'Adventure', icon: '✈' },
  { label: '搞笑', genre: 'Comedy', icon: '☺' },
  { label: '恋爱', genre: 'Romance', icon: '♡' },
  { label: '奇幻', genre: 'Fantasy', icon: '✧' },
  { label: '科幻', genre: 'Sci-Fi', icon: '⌁' },
  { label: '日常', genre: 'Slice of Life', icon: '☁' },
  { label: '校园', genre: 'School', icon: '♧' },
  { label: '治愈', genre: 'Iyashikei', icon: '蛍' },
  { label: '异世界', genre: 'Isekai', icon: '◇' },
  { label: '音乐', genre: 'Music', icon: '♪' },
  { label: '运动', genre: 'Sports', icon: '★' },
  { label: '悬疑', genre: 'Mystery', icon: '?' },
  { label: '心理', genre: 'Psychological', icon: '◈' },
  { label: '超自然', genre: 'Supernatural', icon: '✜' },
  { label: '恐怖', genre: 'Horror', icon: '☾' },
  { label: '惊悚', genre: 'Thriller', icon: '⌖' },
  { label: '剧情', genre: 'Drama', icon: '❖' },
  { label: '机甲', genre: 'Mecha', icon: '⚙' },
  { label: '魔法少女', genre: 'Mahou Shoujo', icon: '☆' },
]

export const ANIME_TYPE_LABELS: Record<string, string> = {
  TV: 'TV动画',
  Movie: '剧场版',
  ONA: 'ONA',
  OVA: 'OVA',
  Special: '特别篇',
  Music: '音乐',
}

export const ANIME_YEAR_RANGES: AnimeYearOption[] = [
  { label: '2014-2010', from: 2010, to: 2014 },
  { label: '2009-2005', from: 2005, to: 2009 },
  { label: '2004-2000', from: 2000, to: 2004 },
  { label: '90年代', from: 1990, to: 1999 },
  { label: '80年代', from: 1980, to: 1989 },
  { label: '更早', from: null, to: 1979 },
]
