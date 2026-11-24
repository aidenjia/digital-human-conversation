import assert from 'node:assert/strict'
import test from 'node:test'
import { clearToken, getToken, isAuthenticated, loginWithFixedToken } from '../src/utils/auth.ts'

function storage(t: test.TestContext) {
  const prior = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
  const values = new Map<string, string>()
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
  } })
  t.after(() => {
    if (prior) Object.defineProperty(globalThis, 'localStorage', prior)
    else Reflect.deleteProperty(globalThis, 'localStorage')
  })
  return values
}

test('fixed token login stores the bearer without the old SMS expiry and logout removes it', t => {
  const values = storage(t)
  values.set('auth_token_expires_at', '1')
  loginWithFixedToken('  demo-fixed-token  ')
  assert.equal(getToken(), 'demo-fixed-token')
  assert.equal(isAuthenticated(), true)
  assert.equal(values.has('auth_token_expires_at'), false)
  clearToken()
  assert.equal(getToken(), null)
  assert.equal(isAuthenticated(), false)
})

test('pasting an Authorization bearer value does not duplicate its prefix', t => {
  storage(t)
  loginWithFixedToken('  Bearer demo-fixed-token  ')
  assert.equal(getToken(), 'demo-fixed-token')
})

test('invalid fixed tokens are rejected without overwriting the saved credential or exposing input', t => {
  storage(t)
  loginWithFixedToken('existing-token')
  for (const input of ['', '   ', 'Bearer ', 'Bearer', 'bad token', 'secret\nheader']) {
    assert.throws(() => loginWithFixedToken(input), { message: '请输入有效的固定 Token' })
    assert.equal(getToken(), 'existing-token')
  }
})
