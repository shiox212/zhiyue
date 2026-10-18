import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { assistantApi, type AssistantAnswer } from '@/api/assistant.api'
import { useAuthStore } from '@/stores/auth.store'
import { useAssistantStore } from '@/stores/assistant.store'
vi.mock('@/api/assistant.api', () => ({ assistantApi: { status: vi.fn(), ask: vi.fn() } }))
const route = reactive({ path: '/books', fullPath: '/books?q=fiction' })
vi.mock('vue-router', async original => ({ ...await original<typeof import('vue-router')>(), useRoute: () => route }))
let pinia: ReturnType<typeof createPinia>
const mountLayout = () => mount(DefaultLayout, { attachTo: document.body, global: { plugins: [pinia], stubs: { AppHeader: true, RouterView: true, RouterLink: true } } })
beforeEach(() => { vi.resetAllMocks(); pinia = createPinia(); setActivePinia(pinia); route.path = '/books'; route.fullPath = '/books?q=fiction'; useAuthStore().user = { id: 1, username: 'fixture', role: 0 }; vi.mocked(assistantApi.status).mockResolvedValue({ configured: true, provider: 'DeepSeek', model: 'fixture', documents: 6, chunks: 15 }); window.matchMedia = vi.fn().mockReturnValue({ matches: false }); Object.defineProperty(window, 'innerWidth', { configurable: true, value: 1366 }); Object.defineProperty(window, 'innerHeight', { configurable: true, value: 768 }) })
describe('assistant entry and shared window', () => {
  it('is discoverable, lazy loads, focuses close and restores focus on Escape', async () => {
    const w = mountLayout(); const entry = w.get('.assistant-fab'); expect(entry.text()).toContain('问问 AI'); expect(assistantApi.status).not.toHaveBeenCalled()
    await entry.trigger('click'); await flushPromises(); expect(document.activeElement?.getAttribute('aria-label')).toBe('收起 AI 阅读助手'); expect(w.get('#floating-assistant').attributes('aria-modal')).toBeUndefined(); expect(document.body.style.overflow).not.toBe('hidden')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })); await flushPromises(); expect(w.find('#floating-assistant').exists()).toBe(false); expect(document.activeElement).toBe(entry.element); w.unmount()
  })
  it('retains in-flight answer and draft across collapse and ordinary navigation, and clears unread on open', async () => {
    let resolve!: (a: AssistantAnswer) => void; vi.mocked(assistantApi.ask).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const w = mountLayout(); await w.get('.assistant-fab').trigger('click'); await flushPromises(); await w.get('textarea').setValue('阅读建议'); await w.get('form').trigger('submit'); await w.get('textarea').setValue('下一题草稿'); await w.get('[aria-label="收起 AI 阅读助手"]').trigger('click')
    route.path = '/books/7'; route.fullPath = '/books/7'; resolve({ status: 'GENERAL_KNOWLEDGE', answer: '先读短篇。', citations: [], modelCalled: true, model: 'fixture', requestId: '', inputTokens: 0, outputTokens: 0, elapsedMs: 0 }); await flushPromises(); expect(w.get('.assistant-fab').text()).toContain('有新回答')
    await w.get('.assistant-fab').trigger('click'); await flushPromises(); expect(w.get('.answer-text').text()).toContain('先读短篇'); expect(w.get('textarea').element.value).toBe('下一题草稿'); expect(w.get('.assistant-fab').text()).not.toContain('有新回答'); expect(assistantApi.status).toHaveBeenCalledTimes(1); expect(assistantApi.ask).toHaveBeenCalledTimes(1); w.unmount()
  })
  it('shows connection failure and reconnects explicitly', async () => {
    vi.mocked(assistantApi.status).mockRejectedValueOnce(new Error('连接失败')); const w = mountLayout(); await w.get('.assistant-fab').trigger('click'); await flushPromises(); expect(w.get('[role=alert]').text()).toContain('连接失败'); await w.findAll('button').find(b => b.text() === '重新连接')!.trigger('click'); await flushPromises(); expect(assistantApi.status).toHaveBeenCalledTimes(2); w.unmount()
  })
  it('offers guest login returning to current route and sends no assistant requests', async () => {
    useAuthStore().user = null; const w = mountLayout(); await w.get('.assistant-fab').trigger('click'); await flushPromises(); expect(w.text()).toContain('登录后可向 AI 阅读助手提问'); const login = w.findAllComponents({ name: 'RouterLink' }).find(c => typeof c.props('to') === 'object')!; expect(login.props('to')).toEqual({ path: '/login', query: { redirect: '/books?q=fiction' } }); expect(assistantApi.status).not.toHaveBeenCalled(); expect(assistantApi.ask).not.toHaveBeenCalled(); w.unmount()
  })
  it('hides duplicate floating UI on full page while retaining session', async () => {
    const w = mountLayout(); await w.get('.assistant-fab').trigger('click'); await flushPromises(); useAssistantStore().draft = '保留'; route.path = '/assistant'; route.fullPath = '/assistant'; await flushPromises(); expect(w.find('.assistant-fab').exists()).toBe(false); expect(w.find('#floating-assistant').exists()).toBe(false); expect(useAssistantStore().draft).toBe('保留'); w.unmount()
  })
  it('mobile uses modal semantics, locks background and releases on close', async () => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 360 }); const w = mountLayout(); await w.get('.assistant-fab').trigger('click'); await flushPromises(); expect(w.get('#floating-assistant').attributes('role')).toBe('dialog'); expect(w.get('#floating-assistant').attributes('aria-modal')).toBe('true'); expect(document.body.style.overflow).toBe('hidden')
    await w.get('[aria-label="收起 AI 阅读助手"]').trigger('click'); await flushPromises(); expect(document.body.style.overflow).not.toBe('hidden'); w.unmount()
  })
})
