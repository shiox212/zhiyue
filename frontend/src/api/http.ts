import axios, { AxiosError } from 'axios'
import type { ApiResponse } from '@/types/api'
import { ApiError } from '@/types/api'
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000, withCredentials: true,
})
http.interceptors.response.use(response => response, (error: AxiosError<ApiResponse<unknown>>) => {
  if (error.response?.status === 401 && error.config?.url !== '/session') {
    window.dispatchEvent(new CustomEvent('zhiyue:auth-expired'))
  }
  const message = error.response?.data?.msg || (error.response ? '请求失败，请重试' : '无法连接服务器，请确认后端已启动')
  return Promise.reject(new ApiError(message, error.response?.status))
})
export function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code === 200 || response.code === 201) return response.data
  throw new ApiError(response.msg || '请求未能完成', undefined, response.code)
}
