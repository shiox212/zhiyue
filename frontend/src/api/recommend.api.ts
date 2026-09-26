import { http, unwrap } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type { Book } from '@/types/domain'

export const recommendApi = {
  async list(userId: number) {
    const { data } = await http.get<ApiResponse<Book[]>>('/recommend/get', { params: { userId } })
    return unwrap(data) || []
  },
}
