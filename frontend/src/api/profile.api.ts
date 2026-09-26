import { http, unwrap } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type { User, UserStats } from '@/types/domain'

export const profileApi = {
  async stats(userId: number) {
    const { data } = await http.get<ApiResponse<UserStats>>('/user/stats', { params: { userId } })
    return unwrap(data)
  },

  async update(userId: number, nickname: string) {
    const { data } = await http.post<ApiResponse<User>>('/user/profile/update', { userId, nickname })
    return unwrap(data)
  },

  async changePassword(userId: number, oldPassword: string, newPassword: string) {
    const { data } = await http.post<ApiResponse<unknown>>('/user/password/change', {
      userId,
      oldPassword,
      newPassword,
    })
    return unwrap(data)
  },
}
