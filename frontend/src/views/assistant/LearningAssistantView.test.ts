import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LearningAssistantView from './LearningAssistantView.vue'
import { assistantApi, type AssistantAnswer } from '@/api/assistant.api'
import { useAuthStore } from '@/stores/auth.store'
import type { User } from '@/types/domain'

vi.mock('@/api/assistant.api', () => ({ assistantApi: { status: vi.fn(), ask: vi.fn() } }))
vi.mock('@/stores/auth.store', async () => {
  const { reactive } = await import('vue')
  const auth = reactive({ user: { id: 1, username: 'fixture', role: 0 } as User | null, get isAuthenticated() { return Boolean(this.user) } })
  return { useAuthStore: () => auth }
})
vi.mock('vue-router', async (importOriginal) => ({ ...await importOriginal<typeof import('vue-router')>(), useRoute: () => ({ fullPath: '/books?q=fiction' }) }))
const response: AssistantAnswer = {
  status: 'ANSWERED', answer: '访问令牌默认有效期900秒。', citations: [{ sourceId: 'auth_1', title: '登录说明', quote: '访问令牌默认有效期900秒。', excerpt: '访问令牌默认有效期900秒。刷新凭据7天。', score: .5 }],
  modelCalled: true, model: 'deepseek-flash', requestId: 'fixture', inputTokens: 10, outputTokens: 20, elapsedMs: 100,
}
beforeEach(() => {
  vi.resetAllMocks()
  useAuthStore().user = { id: 1, username: 'fixture', role: 0 }
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
    expect(assistantApi.ask).toHaveBeenCalledWith('JWT 有效期？', 'LIBRARY')
    expect(wrapper.get('.answer-text').text()).toContain('900秒')
    expect(wrapper.get('details summary').text()).toContain('登录说明')
    expect(wrapper.get('details blockquote').text()).toBe(response.citations[0]!.quote)
    wrapper.unmount()
  })
  it('links verified book citations to the corresponding detail page', async () => {
    vi.mocked(assistantApi.ask).mockResolvedValue({ ...response, citations: [{ ...response.citations[0]!, title: '三体', bookId: 42 }] })
    const wrapper = mount(LearningAssistantView, { global: { stubs: { RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' } } } })
    await flushPromises(); await wrapper.get('textarea').setValue('三体作者')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(wrapper.get('.book-link').attributes('href')).toBe('/books/42')
    wrapper.unmount()
  })
  it('explicitly labels general knowledge and sends the selected mode', async () => {
    vi.mocked(assistantApi.ask).mockResolvedValue({ ...response, status: 'GENERAL_KNOWLEDGE', answer: '模型通用知识', citations: [] })
    const wrapper = mount(LearningAssistantView); await flushPromises()
    await wrapper.get('select').setValue('GENERAL'); await wrapper.get('textarea').setValue('魔幻现实主义是什么')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(assistantApi.ask).toHaveBeenCalledWith('魔幻现实主义是什么', 'GENERAL')
    expect(wrapper.get('.answer-label').text()).toContain('非站内资料 · 非实时联网')
    expect(wrapper.find('details').exists()).toBe(false); wrapper.unmount()
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
  it('retains previous turns and their modes, references, and failure explanations', async () => {
    const wrapper = mount(LearningAssistantView); await flushPromises()
    await wrapper.get('textarea').setValue('JWT')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    vi.mocked(assistantApi.ask).mockRejectedValueOnce(new Error('上游暂时不可用'))
    await wrapper.get('select').setValue('GENERAL'); await wrapper.get('textarea').setValue('阅读建议')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(wrapper.findAll('.conversation-turn')).toHaveLength(2)
    expect(wrapper.get('details blockquote').text()).toBe(response.citations[0]!.quote)
    expect(wrapper.findAll('.turn-mode')[1]!.text()).toContain('模型通用知识')
    expect(wrapper.get('[role=alert]').text()).toContain('上游暂时不可用')
    wrapper.unmount()
  })
  it('clears history on logout and discards late answers from the previous account', async () => {
    let resolve!: (value: AssistantAnswer) => void
    vi.mocked(assistantApi.ask).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const wrapper = mount(LearningAssistantView, { global: { stubs: { RouterLink: true } } }); await flushPromises()
    await wrapper.get('textarea').setValue('JWT')
    await wrapper.get('form').trigger('submit')
    useAuthStore().user = null; await flushPromises()
    expect(wrapper.text()).toContain('登录后可向图书知识助手提问')
    useAuthStore().user = { id: 2, username: 'second', role: 0 }; await flushPromises()
    resolve(response); await flushPromises()
    expect(wrapper.find('.conversation-turn').exists()).toBe(false)
    expect(wrapper.get('textarea').element.value).toBe('')
    expect(wrapper.get('button[type=submit]').text()).toBe('向图书知识助手提问')
    wrapper.unmount()
  })
  it('ignores a late status response after the reader logs out', async () => {
    let resolve!: (value: Awaited<ReturnType<typeof assistantApi.status>>) => void
    vi.mocked(assistantApi.status).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const wrapper = mount(LearningAssistantView, { global: { stubs: { RouterLink: true } } })
    useAuthStore().user = null; await flushPromises()
    resolve({ configured: true, provider: 'DeepSeek', model: 'fixture', documents: 6, chunks: 15 }); await flushPromises()
    expect(wrapper.find('.ready-badge').exists()).toBe(false)
    expect(wrapper.find('form').exists()).toBe(false)
    wrapper.unmount()
  })
})
