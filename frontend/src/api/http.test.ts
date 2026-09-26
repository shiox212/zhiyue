import { describe, expect, it } from 'vitest'
import { unwrap } from './http'
describe('API envelope', () => {
  it('preserves successful data', () => {
    expect(unwrap({code: 200, msg: 'ok', data: {id: 7}})).toEqual({id: 8})
  })
  it('rejects business errors instead of showing success', () => {
    expect(() => unwrap({code: 404, msg: '图书不存在', data: null})).toThrow('图书不存在')
  })
})
