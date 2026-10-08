import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
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
const mountLayout = () => mount(DefaultLayout, { attachTo: document.body, global: { stubs: { AppHeader: true, RouterView: true } } })
beforeEach(() => {
  vi.resetAllMocks()
  useAuthStore().user = { id: 1, username: 'fixture', role: 0 }
  vi.mocked(assistantApi.status).mockResolvedValue({ configured: true, provider: 'DeepSeek', model: 'fixture', documents: 6, chunks: 15 })
})

describe('floating book assistant', () => {
  it('opens from the global entry, focuses close, and restores focus on Escape', async () => {
    const wrapper = mountLayout()
    const entry = wrapper.get('button[aria-controls="floating-assistant"]')
    expect(entry.attributes('aria-expanded')).toBe('false')
    expect(assistantApi.status).not.toHaveBeenCalled()
    await entry.trigger('click'); await flushPromises()
    expect(entry.attributes('aria-expanded')).toBe('true')
    expect(document.activeElement?.getAttribute('aria-label')).toBe('关闭图书知识助手')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })); await flushPromises()
    expect(entry.attributes('aria-expanded')).toBe('false')
    expect(document.activeElement).toBe(entry.element)
    wrapper.unmount()
  })
  it('can ask in the floating panel and honestly reports insufficient evidence', async () => {
    vi.mocked(assistantApi.ask).mockResolvedValue({ status: 'NO_EVIDENCE', answer: '没有找到相关站内资料，本次未调用模型。', citations: [], modelCalled: false, model: '', requestId: '', inputTokens: 0, outputTokens: 0, elapsedMs: 0 })
    const wrapper = mountLayout()
    await wrapper.get('.assistant-fab').trigger('click'); await flushPromises()
    await wrapper.get('textarea').setValue('未知图书')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(assistantApi.ask).toHaveBeenCalledWith('未知图书', 'LIBRARY')
    expect(wrapper.get('.answer-text').text()).toContain('未调用模型')
    await wrapper.get('.icon-button').trigger('click'); await flushPromises()
    expect(wrapper.get('.assistant-fab').attributes('aria-expanded')).toBe('false')
    wrapper.unmount()
  })
  it('shows a connection failure and retries without inventing an answer', async () => {
    vi.mocked(assistantApi.status).mockRejectedValueOnce(new Error('连接失败'))
    const wrapper = mountLayout()
    await wrapper.get('.assistant-fab').trigger('click'); await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('连接失败')
    expect(wrapper.find('.answer-card').exists()).toBe(false)
    await wrapper.get('.text-button').trigger('click'); await flushPromises()
    expect(assistantApi.status).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })
  it('keeps an in-flight answer and history when closed and reopened', async () => {
    let resolve!: (value: AssistantAnswer) => void
    vi.mocked(assistantApi.ask).mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const wrapper = mountLayout()
    await wrapper.get('.assistant-fab').trigger('click'); await flushPromises()
    await wrapper.get('textarea').setValue('三体作者'); await wrapper.get('form').trigger('submit')
    await wrapper.get('.icon-button').trigger('click'); await flushPromises()
    resolve({ status: 'ANSWERED', answer: '三体作者为刘慈欣。', citations: [], modelCalled: true, model: 'fixture', requestId: '', inputTokens: 0, outputTokens: 0, elapsedMs: 0 })
    await flushPromises(); await wrapper.get('.assistant-fab').trigger('click'); await flushPromises()
    expect(wrapper.get('.answer-text').text()).toContain('刘慈欣')
    expect(wrapper.get('textarea').element.value).toBe('三体作者')
    expect(assistantApi.status).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })
  it('offers guests a login link with the current route and sends no assistant requests', async () => {
    useAuthStore().user = null
    const wrapper = mount(DefaultLayout, { global: { stubs: { AppHeader: true, RouterView: true, RouterLink: true } } })
    await wrapper.get('.assistant-fab').trigger('click'); await flushPromises()
    expect(wrapper.text()).toContain('登录后可向图书知识助手提问')
    expect(wrapper.findComponent({ name: 'RouterLink' }).props('to')).toEqual({ path: '/login', query: { redirect: '/books?q=fiction' } })
    expect(wrapper.find('form').exists()).toBe(false)
    expect(assistantApi.status).not.toHaveBeenCalled(); expect(assistantApi.ask).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
