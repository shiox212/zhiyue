import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LearningAssistantView from './LearningAssistantView.vue'
import AssistantPanel from '@/components/assistant/AssistantPanel.vue'
import { assistantApi, type AssistantAnswer } from '@/api/assistant.api'
import { useAuthStore } from '@/stores/auth.store'
import { useAssistantStore } from '@/stores/assistant.store'
vi.mock('@/api/assistant.api', () => ({ assistantApi: { status: vi.fn(), ask: vi.fn() } }))
vi.mock('vue-router', async original => ({ ...await original<typeof import('vue-router')>(), useRoute: () => ({ path: '/assistant', fullPath: '/assistant' }) }))
const response: AssistantAnswer = { status: 'ANSWERED', answer: '访问令牌默认有效期900秒。', citations: [{ sourceId: 'auth_1', title: '登录说明', quote: '访问令牌默认有效期900秒。', excerpt: '访问令牌默认有效期900秒。刷新凭据7天。', score: .5 }], modelCalled: true, model: 'fixture', requestId: 'DO_NOT_COPY', inputTokens: 10, outputTokens: 20, elapsedMs: 100 }
const stubs = { RouterLink: { props: ['to'], template: '<a :href="typeof to === \'string\' ? to : to.path"><slot /></a>' } }
let pinia: ReturnType<typeof createPinia>
const view = () => mount(LearningAssistantView, { global: { plugins: [pinia], stubs } })
async function ask(wrapper: ReturnType<typeof view>, question = 'JWT 有效期？') { await flushPromises(); await wrapper.get('textarea').setValue(question); await wrapper.get('form').trigger('submit'); await flushPromises() }
beforeEach(() => {
  vi.resetAllMocks(); pinia = createPinia(); setActivePinia(pinia)
  useAuthStore().user = { id: 1, username: 'fixture', role: 0 }
  vi.mocked(assistantApi.status).mockResolvedValue({ configured: true, provider: 'DeepSeek', model: 'fixture', documents: 6, chunks: 15 })
  vi.mocked(assistantApi.ask).mockResolvedValue(response)
  window.matchMedia = vi.fn().mockReturnValue({ matches: false })
})
describe('unified reading assistant', () => {
  it('uses AUTO without a selector, clears submitted draft, preserves sources and escapes HTML', async () => {
    vi.mocked(assistantApi.ask).mockResolvedValue({ ...response, answer: '<img src=x onerror=alert(1)>' })
    const w = view(); await ask(w)
    expect(w.find('select').exists()).toBe(false); expect(assistantApi.ask).toHaveBeenCalledWith('JWT 有效期？', 'AUTO')
    expect(w.get('textarea').element.value).toBe(''); expect(w.get('.source-card blockquote').text()).toBe(response.citations[0]!.quote)
    expect(w.find('.answer-text img').exists()).toBe(false); expect(w.get('.answer-text').text()).toContain('<img'); w.unmount()
  })
  it('fills examples without sending, and protects Chinese composition and Shift+Enter', async () => {
    const w = view(); await flushPromises(); await w.get('.assistant-examples button').trigger('click')
    expect(assistantApi.ask).not.toHaveBeenCalled(); const input = w.get('textarea')
    await input.trigger('compositionstart'); await input.trigger('keydown', { key: 'Enter' }); expect(assistantApi.ask).not.toHaveBeenCalled()
    await input.trigger('compositionend'); await input.trigger('keydown', { key: 'Enter', isComposing: true }); await input.trigger('keydown', { key: 'Enter', keyCode: 229 }); await input.trigger('keydown', { key: 'Enter', shiftKey: true }); expect(assistantApi.ask).not.toHaveBeenCalled()
    await input.trigger('keydown', { key: 'Enter' }); await flushPromises(); expect(assistantApi.ask).toHaveBeenCalledTimes(1); w.unmount()
  })
  it('labels general and supplementary knowledge separately without fabricating sources', async () => {
    vi.mocked(assistantApi.ask).mockResolvedValue({ ...response, status: 'GENERAL_KNOWLEDGE', citations: [], relatedBooks: [], answer: '本次未检索到相关站内资料，以下为通用知识。' })
    const w = view(); await ask(w, '阅读建议'); expect(w.get('.answer-label').text()).toContain('非实时联网'); expect(w.find('.source-list').exists()).toBe(false)
    vi.mocked(assistantApi.ask).mockResolvedValue({ ...response, status: 'INSUFFICIENT', answer: '资料不足', citations: [], generalSupplement: '通用阅读建议', relatedBooks: [{ bookId: 7, title: '夹具图书' }, { bookId: -1, title: '无效ID' }] })
    await ask(w, '作品分析'); expect(w.get('.assistant-supplement').text()).toContain('通用阅读建议'); expect(w.get('.assistant-book a').attributes('href')).toBe('/books/7'); expect(w.findAll('.assistant-book a')).toHaveLength(1); w.unmount()
  })
  it('supports legal citation detail navigation and never enables invalid IDs', async () => {
    vi.mocked(assistantApi.ask).mockResolvedValue({ ...response, citations: [{ ...response.citations[0]!, bookId: 42 }, { ...response.citations[0]!, sourceId: 'invalid', bookId: -2 }] })
    const w = view(); await ask(w); expect(w.get('.book-link').attributes('href')).toBe('/books/42'); expect(w.findAll('.book-link')).toHaveLength(1); w.unmount()
  })
  it('disables unconfigured requests and reconnects status only on user action', async () => {
    vi.mocked(assistantApi.status).mockResolvedValueOnce({ configured: false, provider: 'DeepSeek', model: '', documents: 6, chunks: 15 })
    const w = view(); await ask(w); expect(assistantApi.ask).not.toHaveBeenCalled(); expect(w.get('button[type=submit]').attributes('disabled')).toBeDefined()
    await w.findAll('button').find(b => b.text() === '重新连接')!.trigger('click'); await flushPromises(); expect(assistantApi.status).toHaveBeenCalledTimes(2); w.unmount()
  })
  it('keeps failed question for explicit edit and resend without automatic retry', async () => {
    vi.mocked(assistantApi.ask).mockRejectedValueOnce(new Error('模型服务超时'))
    const w = view(); await ask(w, '待重试的问题'); expect(w.get('[role=alert]').text()).toContain('超时'); expect(w.find('.answer-card').exists()).toBe(false); expect(assistantApi.ask).toHaveBeenCalledTimes(1)
    await w.findAll('button').find(b => b.text() === '编辑后重发')!.trigger('click'); expect(w.get('textarea').element.value).toBe('待重试的问题'); expect(assistantApi.ask).toHaveBeenCalledTimes(1)
    await w.get('form').trigger('submit'); await flushPromises(); expect(w.findAll('.conversation-turn')).toHaveLength(2); expect(assistantApi.ask).toHaveBeenCalledTimes(2); w.unmount()
  })
  it('prevents duplicates and shares in-flight state across view unmounts without new status calls', async () => {
    let resolve!: (a: AssistantAnswer) => void; vi.mocked(assistantApi.ask).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const w = view(); await ask(w); await w.get('form').trigger('submit'); expect(assistantApi.ask).toHaveBeenCalledTimes(1); w.unmount()
    const other = mount(AssistantPanel, { global: { plugins: [pinia], stubs } }); await flushPromises(); resolve(response); await flushPromises()
    expect(other.get('.answer-text').text()).toContain('900秒'); expect(assistantApi.status).toHaveBeenCalledTimes(1); other.unmount()
  })
  it('invalidates late answers and status on logout or account change', async () => {
    let resolve!: (a: AssistantAnswer) => void; vi.mocked(assistantApi.ask).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const w = view(); await ask(w); useAuthStore().clearSession(); await flushPromises(); expect(w.find('form').exists()).toBe(false)
    useAuthStore().user = { id: 2, username: 'second', role: 0 }; await flushPromises(); resolve(response); await flushPromises(); expect(w.find('.conversation-turn').exists()).toBe(false); expect(w.get('textarea').element.value).toBe(''); w.unmount()
  })
  it('ignores old status after logout', async () => {
    let resolve!: (v: Awaited<ReturnType<typeof assistantApi.status>>) => void; vi.mocked(assistantApi.status).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const w = view(); useAuthStore().clearSession(); resolve({ configured: true, provider: 'DeepSeek', model: 'fixture', documents: 6, chunks: 15 }); await flushPromises(); expect(useAssistantStore().status).toBeNull(); w.unmount()
  })
  it('preserves conversation on same-account profile updates but isolates a direct account switch', async () => {
    let resolve!: (a: AssistantAnswer) => void
    vi.mocked(assistantApi.ask).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const w = view(); await ask(w, '旧账号问题'); await w.get('textarea').setValue('旧账号草稿')
    useAuthStore().updateUser({ id: 1, username: 'fixture', nickname: '新昵称', role: 0 }); await flushPromises()
    expect(useAssistantStore().history).toHaveLength(1); expect(useAssistantStore().pending).toBe(true)
    expect(useAssistantStore().draft).toBe('旧账号草稿'); expect(assistantApi.status).toHaveBeenCalledTimes(1)
    useAuthStore().updateUser({ id: 2, username: 'second', role: 0 }); await flushPromises()
    expect(useAssistantStore().history).toHaveLength(0); expect(useAssistantStore().draft).toBe('')
    resolve(response); await flushPromises(); expect(w.find('.conversation-turn').exists()).toBe(false)
    expect(assistantApi.status).toHaveBeenCalledTimes(2); w.unmount()
  })
  it('discards old account status even when it arrives after the new account status', async () => {
    let resolve!: (v: Awaited<ReturnType<typeof assistantApi.status>>) => void
    vi.mocked(assistantApi.status).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const w = view(); await flushPromises(); useAuthStore().updateUser({ id: 2, username: 'second', role: 0 }); await flushPromises()
    expect(useAssistantStore().status?.configured).toBe(true)
    resolve({ configured: false, provider: 'old', model: 'old', documents: 0, chunks: 0 }); await flushPromises()
    expect(useAssistantStore().status?.configured).toBe(true); expect(useAssistantStore().status?.provider).toBe('DeepSeek'); w.unmount()
  })
  it('does not follow a response while an expanded citation is being read at the bottom', async () => {
    const w = view(); await ask(w)
    const el = w.get('.assistant-messages').element as HTMLElement
    Object.defineProperties(el, { scrollHeight: { configurable: true, value: 1000 }, clientHeight: { configurable: true, value: 300 } })
    el.scrollTop = 700; await w.get('.assistant-messages').trigger('scroll')
    const source = w.get('details.source-card'); (source.element as HTMLDetailsElement).open = true; await source.trigger('toggle')
    Object.defineProperty(el, 'scrollHeight', { configurable: true, value: 1300 })
    useAssistantStore().revision++; await flushPromises()
    expect(el.scrollTop).toBe(700); expect(w.find('.assistant-new').exists()).toBe(true); w.unmount()
  })
  it('keeps privacy and clear confirmation exclusive and moves focus to the confirmation', async () => {
    const w = mount(LearningAssistantView, { attachTo: document.body, global: { plugins: [pinia], stubs } }); await ask(w)
    const privacy = w.get('.assistant-footnote details'); (privacy.element as HTMLDetailsElement).open = true; await privacy.trigger('toggle')
    await w.findAll('button').find(b => b.text() === '清空对话')!.trigger('click'); await flushPromises()
    expect((privacy.element as HTMLDetailsElement).open).toBe(false)
    expect(document.activeElement?.textContent).toBe('确认清空')
    await w.findAll('button').find(b => b.text() === '保留对话')!.trigger('click'); await flushPromises()
    expect(document.activeElement?.textContent).toBe('清空对话')
    // A native details toggle event may still be queued when Escape is pressed.
    ;(privacy.element as HTMLDetailsElement).open = true
    await privacy.get('summary').trigger('keydown', { key: 'Escape' })
    expect((privacy.element as HTMLDetailsElement).open).toBe(false); w.unmount()
  })
  it('clear confirmation invalidates in-flight response but never promises cancellation', async () => {
    let resolve!: (a: AssistantAnswer) => void; vi.mocked(assistantApi.ask).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const w = view(); await ask(w); await w.findAll('button').find(b => b.text() === '清空对话')!.trigger('click'); expect(w.text()).toContain('仍可能计费'); expect(useAssistantStore().history).toHaveLength(1)
    await w.findAll('button').find(b => b.text() === '确认清空')!.trigger('click'); resolve(response); await flushPromises(); expect(w.find('.conversation-turn').exists()).toBe(false); w.unmount()
  })
  it('does not steal scroll while reading an old answer and offers return to bottom', async () => {
    const w = view(); await ask(w); const el = w.get('.assistant-messages').element as HTMLElement
    Object.defineProperties(el, { scrollHeight: { configurable: true, value: 2000 }, clientHeight: { configurable: true, value: 300 } }); el.scrollTop = 50; await w.get('.assistant-messages').trigger('scroll')
    useAssistantStore().revision++; await flushPromises(); expect(el.scrollTop).toBe(50); expect(w.get('.assistant-new').text()).toContain('新内容')
    await w.get('.assistant-new').trigger('click'); expect(el.scrollTop).toBe(2000); w.unmount()
  })
  it('copy reports actual success/failure and excludes internal metadata', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined); Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } })
    const w = view(); await ask(w); const button = w.findAll('button').find(b => b.text() === '复制回答')!; await button.trigger('click'); await flushPromises(); expect(writeText).toHaveBeenCalledWith(response.answer); expect(w.text()).toContain('回答已复制')
    writeText.mockRejectedValueOnce(new Error('denied')); await button.trigger('click'); await flushPromises(); expect(w.text()).toContain('复制失败'); w.unmount()
  })
})
