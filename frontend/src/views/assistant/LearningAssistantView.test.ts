import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LearningAssistantView from './LearningAssistantView.vue'
import { assistantApi, type AssistantAnswer } from '@/api/assistant.api'

vi.mock('@/api/assistant.api', () => ({ assistantApi: { status: vi.fn(), ask: vi.fn() } }))
const response: AssistantAnswer = {
  status: 'ANSWERED', answer: '访问令牌默认有效期900秒。', citations: [{ sourceId: 'auth_1', title: '登录说明', quote: '访问令牌默认有效期900秒。', excerpt: '访问令牌默认有效期900秒。刷新凭据7天。', score: .5 }],
  modelCalled: true, model: 'deepseek-flash', requestId: 'fixture', inputTokens: 10, outputTokens: 20, elapsedMs: 100,
}
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(assistantApi.status).mockResolvedValue({ configured: true, provider: 'DeepSeek', model: 'deepseek-flash', documents: 6, chunks: 15 })
  vi.mocked(assistantApi.ask).mockResolvedValue(response)
})
describe('LearningAssistantView', () => {
  it('shows an answer together with expandable original sources', async () => {
    const wrapper = mount(LearningAssistantView)
    await flushPromises()
    await wrapper.get('textarea').setValue('JWT 有效期？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(assistantApi.ask).toHaveBeenCalledWith('JWT 有效期？')
    expect(wrapper.get('.answer-text').text()).toContain('900秒')
    expect(wrapper.get('details summary').text()).toContain('登录说明')
    expect(wrapper.get('details blockquote').text()).toBe(response.citations[0]!.quote)
    wrapper.unmount()
  })
  it('renders model markup as text, never executable HTML', async () => {
    vi.mocked(assistantApi.ask).mockResolvedValue({ ...response, answer: '<img src=x onerror=alert(1)>' })
    const wrapper = mount(LearningAssistantView)
    await flushPromises()
    await wrapper.get('textarea').setValue('JWT 有效期')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(wrapper.find('.answer-text img').exists()).toBe(false)
    expect(wrapper.get('.answer-text').text()).toContain('<img')
    wrapper.unmount()
  })
  it('disables asking when credentials are not configured', async () => {
    vi.mocked(assistantApi.status).mockResolvedValue({ configured: false, provider: 'DeepSeek', model: 'deepseek-flash', documents: 6, chunks: 15 })
    const wrapper = mount(LearningAssistantView); await flushPromises()
    await wrapper.get('textarea').setValue('JWT')
    expect(wrapper.get('button[type=submit]').attributes('disabled')).toBeDefined()
    await wrapper.get('form').trigger('submit')
    expect(assistantApi.ask).not.toHaveBeenCalled()
    wrapper.unmount()
  })
  it('shows provider failure and does not display a fabricated answer', async () => {
    vi.mocked(assistantApi.ask).mockRejectedValue(new Error('模型服务暂时不可用'))
    const wrapper = mount(LearningAssistantView); await flushPromises()
    await wrapper.get('textarea').setValue('JWT')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(wrapper.get('[role=alert]').text()).toContain('模型服务暂时不可用')
    expect(wrapper.find('.answer-card').exists()).toBe(false)
    wrapper.unmount()
  })
  it('prevents duplicate submissions while a response is pending', async () => {
    let resolve!: (value: AssistantAnswer) => void
    vi.mocked(assistantApi.ask).mockImplementation(() => new Promise(r => { resolve = r }))
    const wrapper = mount(LearningAssistantView); await flushPromises()
    await wrapper.get('textarea').setValue('JWT')
    await wrapper.get('form').trigger('submit'); await wrapper.get('form').trigger('submit')
    expect(assistantApi.ask).toHaveBeenCalledTimes(1)
    expect(wrapper.get('button[type=submit]').attributes('disabled')).toBeDefined()
    resolve(response); await flushPromises(); wrapper.unmount()
  })
})
