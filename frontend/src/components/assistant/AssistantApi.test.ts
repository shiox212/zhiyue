import { beforeEach, expect, it, vi } from 'vitest'
import { assistantApi } from '@/api/assistant.api'
import { http } from '@/api/http'
vi.mock('@/api/http', () => ({ http: { get: vi.fn(), post: vi.fn() }, unwrap: (response: { data: unknown }) => response.data }))
beforeEach(() => vi.resetAllMocks())
it('sends only the current question and AUTO and disables implicit 401 replay for asks and status', async () => {
  vi.mocked(http.post).mockResolvedValue({ data: { data: {} } }); vi.mocked(http.get).mockResolvedValue({ data: { data: {} } })
  await assistantApi.ask('阅读建议'); await assistantApi.status()
  expect(http.post).toHaveBeenCalledExactlyOnceWith('/assistant/ask', { question: '阅读建议', mode: 'AUTO' }, { timeout: 55000, retried: true })
  expect(http.get).toHaveBeenCalledExactlyOnceWith('/assistant/status', { retried: true })
})
