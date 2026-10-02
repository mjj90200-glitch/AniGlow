export interface ReviewReply {
  id: number
  userId?: number
  user: string
  avatar?: string
  content: string
  date: string
  likes: number
  avatarGradient: string
  isFresh?: boolean
  createdAt?: number
}

export interface Review {
  id: number
  userId?: number
  user: string
  avatar?: string
  rating: number
  content: string
  date: string
  likes: number
  avatarGradient: string
  isLocal?: boolean
  isFresh?: boolean
  createdAt?: number
  replies: ReviewReply[]
}

export type ReviewDeleteTarget =
  | { type: 'review'; id: number }
  | { type: 'reply'; reviewId: number; replyId: number }
