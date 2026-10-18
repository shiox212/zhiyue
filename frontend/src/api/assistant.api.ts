import type { AxiosRequestConfig } from 'axios'
import { http, unwrap } from './http'
import type { ApiResponse } from '@/types/api'

export interface AssistantStatus {
  configured: boolean
  provider: string
  model: string
  documents: number
  chunks: number
}
export interface Citation {
  bookId?: number | null
  sourceId: string
  title: string
  quote: string
  excerpt: string
  score: number
}
export interface AssistantAnswer {
  status: 'ANSWERED' | 'GENERAL_KNOWLEDGE' | 'NO_EVIDENCE' | 'INSUFFICIENT' | 'UNVERIFIED'
  answer: string
  relatedBooks?: { bookId: number; title: string }[]
  generalSupplement?: string | null
  citations: Citation[]
  modelCalled: boolean
  model: string
  requestId: string
  inputTokens: number
  outputTokens: number
  elapsedMs: number
}
export const assistantApi = {
  async status() {
    return unwrap((await http.get<ApiResponse<AssistantStatus>>('/assistant/status', { retried: true } as AxiosRequestConfig & { retried: boolean })).data)
  },
  async ask(question: string, mode: 'AUTO' | 'LIBRARY' | 'GENERAL' = 'AUTO') {
    return unwrap((await http.post<ApiResponse<AssistantAnswer>>('/assistant/ask', { question, mode }, { timeout: 55000, retried: true } as AxiosRequestConfig & { retried: boolean })).data)
  },
}
