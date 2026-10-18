import { afterEach, describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { http, unwrap } from './http'
describe('API envelope', () => {
  it('preserves successful data', () => {
    expect(unwrap({code: 200, msg: 'ok', data: {id: 7}})).toEqual({id: 7})
  })
  it('rejects business errors instead of showing success', () => {
    expect(() => unwrap({code: 404, msg: '图书不存在', data: null})).toThrow('图书不存在')
  })
})

describe('cookie authentication transport', () => {
  const adapter = http.defaults.adapter
  afterEach(() => { http.defaults.adapter = adapter; vi.restoreAllMocks() })
  const ok = (config: InternalAxiosRequestConfig, data: unknown = null) => ({ config, status: 200, statusText: 'OK', headers: new AxiosHeaders(), data: { code: 200, msg: 'ok', data } })
  const reject = (config: InternalAxiosRequestConfig, status: number) => Promise.reject(new AxiosError('failure', undefined, config, undefined, { ...ok(config), status, data: { code: status, msg: `error-${status}`, data: null } }))
  it('fetches and sends a CSRF token before a mutation', async () => {
    const calls: string[] = []
    http.defaults.adapter = async config => {
      calls.push(config.url!)
      if (config.url === '/csrf') return ok(config, { token: 'csrf-example' })
      expect(config.headers.get('X-XSRF-TOKEN')).toBe('csrf-example')
      expect(config.withCredentials).toBe(true)
      return ok(config)
    }
    await http.post('/bookshelf/add', { bookId: 1 })
    expect(calls).toEqual(['/csrf', '/bookshelf/add'])
  })
  it('shares one refresh for concurrent expired requests', async () => {
    let refreshed = false, count = 0
    http.defaults.adapter = async config => {
      if (config.url === '/csrf') return ok(config, { token: 'csrf-example' })
      if (config.url === '/session/refresh') { count++; await new Promise(resolve => setTimeout(resolve, 10)); refreshed = true; return ok(config) }
      return refreshed ? ok(config) : reject(config, 401)
    }
    await Promise.all([http.get('/session'), http.get('/bookshelf/list')])
    expect(count).toBe(1)
  })
  it('preserves a forbidden response after a successful refresh', async () => {
    let refreshed = false
    const event = vi.spyOn(window, 'dispatchEvent')
    http.defaults.adapter = async config => {
      if (config.url === '/csrf') return ok(config, { token: 'csrf-example' })
      if (config.url === '/session/refresh') { refreshed = true; return ok(config) }
      return reject(config, refreshed ? 403 : 401)
    }
    await expect(http.get('/admin/user/list')).rejects.toMatchObject({ status: 403, message: 'error-403' })
    expect(event).not.toHaveBeenCalled()
  })
  it('does not refresh a failed login and stops after a failed refresh', async () => {
    const calls: string[] = []
    const event = vi.spyOn(window, 'dispatchEvent')
    http.defaults.adapter = async config => {
      calls.push(config.url!)
      return config.url === '/csrf' ? ok(config, { token: 'csrf-example' }) : reject(config, 401)
    }
    await expect(http.post('/session', {})).rejects.toMatchObject({ status: 401 })
    expect(calls).not.toContain('/session/refresh')
    await expect(http.get('/session')).rejects.toMatchObject({ status: 401 })
    expect(calls.filter(url => url === '/session/refresh')).toHaveLength(1)
    expect(event).toHaveBeenCalledTimes(1)
  })
})
