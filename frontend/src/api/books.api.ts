import { http, unwrap } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type { Book, Review } from '@/types/domain'

export const booksApi = {
  async list(category?: string) {
    const { data } = await http.get<ApiResponse<Book[]>>('/books', {
      params: category ? { category } : undefined,
    })
    return unwrap(data) || []
  },

  async search(keyword: string, category?: string) {
    const { data } = await http.get<ApiResponse<Book[]>>('/books', {
      params: { q: keyword, ...(category ? { category } : {}) },
    })
    return unwrap(data) || []
  },

  async categories() {
    const { data } = await http.get<ApiResponse<string[]>>('/books/categories')
    return unwrap(data) || []
  },

  async getById(id: number) {
    const { data } = await http.get<ApiResponse<Book>>('/books/' + id)
    return unwrap(data)
  },

  async similar(bookId: number) {
    const { data } = await http.get<ApiResponse<Book[]>>('/book/similar', { params: { bookId } })
    return unwrap(data) || []
  },

  async reviews(bookId: number) {
    const { data } = await http.get<ApiResponse<Review[]>>('/review/list', { params: { bookId } })
    return (unwrap(data) || []).map((review) => ({
      ...review,
      createTime: review.createTime || (review as Review & { create_time?: string }).create_time,
      likeCount: review.likeCount ?? (review as Review & { like_count?: number }).like_count ?? 0,
    }))
  },
}
