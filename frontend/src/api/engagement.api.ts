import { http, unwrap } from '@/api/http'
import type { ApiResponse } from '@/types/api'

export const engagementApi = {
  async getRating(userId: number, bookId: number) {
    const { data } = await http.get<ApiResponse<number | null>>('/rating/get', { params: { userId, bookId } })
    return unwrap(data) || 0
  },

  async rate(userId: number, bookId: number, score: number) {
    const { data } = await http.post<ApiResponse<unknown>>('/rating/add', { userId, bookId, score })
    return unwrap(data)
  },

  async review(userId: number, bookId: number, content: string) {
    const { data } = await http.post<ApiResponse<unknown>>('/review/add', { userId, bookId, content })
    return unwrap(data)
  },
}
