import { http, unwrap } from './http'
import type { ApiResponse } from '@/types/api'
import type { User } from '@/types/domain'
export const authApi = {
  async login(payload: { username: string; password: string }) {
    return unwrap((await http.post<ApiResponse<User>>('/session', payload)).data)
  },
  async register(payload: { username: string; password: string; nickname?: string }) {
    return unwrap((await http.post<ApiResponse<unknown>>('/user/register', payload)).data)
  },
  async me() { return unwrap((await http.get<ApiResponse<User>>('/session')).data) },
  async logout() { return unwrap((await http.delete<ApiResponse<unknown>>('/session')).data) },
}
