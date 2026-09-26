import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import { ApiError } from '@/types/api'
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000, withCredentials: true,
})
// Access/refresh credentials are HttpOnly cookies; only the CSRF token is readable by this client.
http.interceptors.request.use(async config => {
  if (!['get', 'head', 'options'].includes(config.method || 'get')) {
    const { data } = await http.get<ApiResponse<{ token: string }>>('/csrf')
    config.headers.set('X-XSRF-TOKEN', unwrap(data).token)
  }
  return config
})
let refreshing: Promise<unknown> | null = null
http.interceptors.response.use(response => response, async (error: AxiosError<ApiResponse<unknown>>) => {
  const config = error.config as (InternalAxiosRequestConfig & { retried?: boolean }) | undefined
  const isCredentialRequest = config?.url === '/session/refresh' || (config?.url === '/session' && config?.method !== 'get')
  if (error.response?.status === 401 && config && !isCredentialRequest && !config.retried) {
    config.retried = true
    let renewed = false
    try {
      refreshing ??= http.post('/session/refresh').finally(() => { refreshing = null })
      await refreshing
      renewed = true
    } catch { /* The refresh failed; let the UI require login. */ }
    if (renewed) return http.request(config)
  }
  if (error.response?.status === 401 && !isCredentialRequest) {
    window.dispatchEvent(new CustomEvent('zhiyue:auth-expired'))
  }
  const message = error.response?.data?.msg || (error.response ? '请求失败，请重试' : '无法连接服务器，请确认后端已启动')
  return Promise.reject(new ApiError(message, error.response?.status))
})
export function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code === 200 || response.code === 201) return response.data
  throw new ApiError(response.msg || '请求未能完成', undefined, response.code)
}
