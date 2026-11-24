import assert from 'node:assert/strict'
import test from 'node:test'
import { TextSubmissions, shouldSendOnEnter } from '../src/services/realtimeText.ts'

test('Enter sends but newline and IME confirmation do not', () => {
  assert.equal(shouldSendOnEnter({ key: 'Enter', shiftKey: false, isComposing: false, keyCode: 13 }), true)
  assert.equal(shouldSendOnEnter({ key: 'Enter', shiftKey: true, isComposing: false, keyCode: 13 }), false)
  assert.equal(shouldSendOnEnter({ key: 'Enter', shiftKey: false, isComposing: true, keyCode: 13 }), false)
  assert.equal(shouldSendOnEnter({ key: 'Enter', shiftKey: false, isComposing: false, keyCode: 229 }), false)
})

test('text submission requires readiness and ignores blank input', async () => {
  const submissions = new TextSubmissions()
  await assert.rejects(submissions.submit('你好', () => false), /连接/)
  await assert.rejects(submissions.submit('  ', () => true), /不能为空/)
  await assert.rejects(submissions.submit('x'.repeat(4001), () => true), /4000/)
})

test('acceptance resolves one request and a duplicate send is blocked while awaiting receipt', async () => {
  const submissions = new TextSubmissions()
  let payload: Record<string, unknown> = {}
  const result = submissions.submit('  你好  ', event => { payload = event; return true })
  assert.equal(payload.text, '你好')
  await assert.rejects(submissions.submit('again', () => true), /处理中/)
  submissions.receive({ type: 'text.accepted', id: payload.id })
  await result
})

test('rejection and disconnect reject pending submissions, allowing caller to restore the draft', async () => {
  const submissions = new TextSubmissions()
  let id: unknown
  const result = submissions.submit('你好', event => { id = event.id; return true })
  submissions.receive({ type: 'text.rejected', id, message: '尚未就绪' })
  await assert.rejects(result, /尚未就绪/)
  const pending = submissions.submit('你好', () => true)
  submissions.close()
  await assert.rejects(pending, /断开/)
})
