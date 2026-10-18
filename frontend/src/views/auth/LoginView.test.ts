import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import LoginView from './LoginView.vue'

const mountLogin = () => mount(LoginView, {
  global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } },
})

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  register: vi.fn(),
  notify: vi.fn(),
  replace: vi.fn(),
}))

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: () => ({ submitting: false, login: mocks.login }),
}))

vi.mock('@/stores/ui.store', () => ({
  useUiStore: () => ({ notify: mocks.notify }),
}))

vi.mock('@/api/auth.api', () => ({
  authApi: { register: mocks.register },
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ replace: mocks.replace }),
  RouterLink: { template: '<a><slot /></a>' },
}))

describe('LoginView validation', () => {
  it('does not add validation messages when an input loses focus', async () => {
    const wrapper = mountLogin()

    await wrapper.get('#username').trigger('blur')
    await wrapper.get('#password').trigger('blur')

    expect(wrapper.findAll('[role="alert"]')).toHaveLength(0)
    expect(wrapper.find('#confirm-password').exists()).toBe(false)
    await wrapper.get('button.secondary').trigger('click')
    expect(wrapper.find('#confirm-password').exists()).toBe(true)
  })

  it('blocks submit with invalid username and password', async () => {
    const wrapper = mountLogin()

    await wrapper.get('form').trigger('submit')

    expect(wrapper.findAll('.field-error')).toHaveLength(2)
    expect(mocks.login).not.toHaveBeenCalled()
    expect(mocks.register).not.toHaveBeenCalled()

    await wrapper.get('button.secondary').trigger('click')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.findAll('.field-error')).toHaveLength(2)
    expect(mocks.register).not.toHaveBeenCalled()
  })
})
