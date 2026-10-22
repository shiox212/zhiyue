import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import BookDetailView from './BookDetailView.vue'

const api = vi.hoisted(() => ({ getById: vi.fn(), reviews: vi.fn(), similar: vi.fn(), getRating: vi.fn(), rate: vi.fn(), review: vi.fn(), save: vi.fn(), notify: vi.fn() }))
vi.mock('@/api/books.api', () => ({ booksApi: { getById: api.getById, reviews: api.reviews, similar: api.similar } }))
vi.mock('@/api/engagement.api', () => ({ engagementApi: { getRating: api.getRating, rate: api.rate, review: api.review } }))
vi.mock('@/api/shelf.api', () => ({ shelfApi: { save: api.save } }))
vi.mock('@/stores/auth.store', () => ({ useAuthStore: () => ({ user: { id: 10 }, isAuthenticated: true }) }))
vi.mock('@/stores/ui.store', () => ({ useUiStore: () => ({ notify: api.notify }) }))
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { id: '1' }, fullPath: '/books/1' }), useRouter: () => ({ replace: vi.fn() }) }))
const book = { id: 1, title: 'Fixture', author: 'Author', category: '文学经典', description: 'Fixture description', avgScore: 4, ratingCount: 1, reviewCount: 1 }
const mountView = () => mount(BookDetailView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' }, BookCard: true, StatusPanel: true } } })
beforeEach(() => {
  vi.resetAllMocks()
  api.getById.mockResolvedValue({ ...book })
  api.reviews.mockResolvedValue([{ id: 1, content: 'First review', nickname: 'Reader' }])
  api.similar.mockResolvedValue([]); api.getRating.mockResolvedValue(4)
  api.rate.mockResolvedValue(null); api.review.mockResolvedValue(null); api.save.mockResolvedValue(null)
})
describe('Detail engagement statistics', () => {
  it('refreshes displayed aggregate after a saved rating without reloading the page', async () => {
    api.getById.mockResolvedValueOnce({ ...book }).mockResolvedValue({ ...book, avgScore: 5 })
    const wrapper = mountView(); await flushPromises()
    await wrapper.find('input[type="radio"][value="5"]').setValue()
    await wrapper.findAll('button').find(button => button.text() === '保存评分')!.trigger('click'); await flushPromises()
    expect(api.rate).toHaveBeenCalledWith(10, 1, 5)
    expect(wrapper.find('.book-facts').text()).toContain('5.0')
    wrapper.unmount()
  })
  it('refreshes displayed review count after publishing', async () => {
    api.getById.mockResolvedValueOnce({ ...book }).mockResolvedValue({ ...book, reviewCount: 2 })
    const wrapper = mountView(); await flushPromises()
    api.reviews.mockResolvedValue([{ id: 1, content: 'First review' }, { id: 2, content: 'Second review' }])
    await wrapper.find('textarea').setValue('Second review')
    await wrapper.find('form.review-form').trigger('submit'); await flushPromises()
    expect(api.review).toHaveBeenCalledWith(10, 1, 'Second review')
    expect(wrapper.find('.book-facts').text()).toContain('2篇书评')
    wrapper.unmount()
  })
})
