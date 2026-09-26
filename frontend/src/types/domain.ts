export interface User {
  id: number
  username: string
  nickname?: string | null
  role: number | string
  roles?: string[]
  permissions?: string[]
  status?: number
  createTime?: string
}

export interface AuthSession {
  accessToken: string
  tokenType?: string
  expiresIn?: number
  user: User
}

export interface Book {
  id: number
  title: string
  author: string
  category: string
  coverUrl?: string | null
  description?: string | null
  avgScore?: number | null
  ratingCount?: number | null
  reviewCount?: number | null
  rankNum?: number | null
  reason?: string | null
  createTime?: string
}

export interface ShelfItem extends Book {
  bookId?: number
  readStatus?: number
  shelfId?: number
}

export interface Review {
  id: number
  userId: number
  bookId: number
  username?: string
  nickname?: string
  content: string
  status?: number
  likeCount?: number
  createTime?: string
}

export interface UserStats {
  shelfCount: number
  ratingCount: number
  reviewCount: number
}

export interface DashboardStats {
  totalBooks: number
  totalUsers: number
  totalRatings: number
  categoryData?: Array<Record<string, unknown>>
  topBooksData?: Array<Record<string, unknown>>
  userGrowthData?: Array<Record<string, unknown>>
}

export interface AiSource {
  title: string
  excerpt?: string
  location?: string
  score?: number
}

export interface AiAnswer {
  answer: string
  sources?: AiSource[]
  sessionId?: string
}
