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
  sourceId: string
  title: string
  quote: string
  excerpt: string
  score: number
}
export interface AssistantAnswer {
  status: 'ANSWERED' | 'NO_EVIDENCE' | 'INSUFFICIENT' | 'UNVERIFIED'
  answer: string
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
    return unwrap((await http.get<ApiResponse<AssistantStatus>>('/assistant/status')).data)
  },
  async ask(question: string) {
    return unwrap((await http.post<ApiResponse<AssistantAnswer>>('/assistant/ask', { question }, { timeout: 55000 })).data)
  },
}
