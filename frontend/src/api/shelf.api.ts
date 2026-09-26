import { http, unwrap } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type { ShelfItem } from '@/types/domain'

export const shelfApi = {
  async list(userId: number) {
    const { data } = await http.get<ApiResponse<ShelfItem[]>>('/bookshelf/list', { params: { userId } })
    return (unwrap(data) || []).map(item => ({
      ...item,
      coverUrl: item.coverUrl || (item as ShelfItem & { cover_url?: string }).cover_url,
    }))
  },

  async save(userId: number, bookId: number, readStatus: number) {
    const { data } = await http.post<ApiResponse<unknown>>('/bookshelf/add', { userId, bookId, readStatus })
    return unwrap(data)
  },

  async remove(userId: number, bookId: number) {
    const { data } = await http.post<ApiResponse<unknown>>('/bookshelf/remove', { userId, bookId })
    return unwrap(data)
  },
}
