import assert from 'node:assert/strict'
import test from 'node:test'
import { appendCaptionDelta } from '../src/services/captionLines.ts'

test('caption updates are pure when React replays an updater', () => {
  const previous: Array<{ id: string; role: 'assistant'; text: string }> = []
  const line = { id: 'a1', role: 'assistant' as const, text: '你好' }
  assert.deepEqual(appendCaptionDelta(previous, line), [line])
  assert.deepEqual(appendCaptionDelta(previous, line), [line])
  assert.deepEqual(previous, [])
})

test('deltas append within a response and remain separate across turns', () => {
  let lines = appendCaptionDelta([], { id: 'user1', role: 'user', text: '你好' })
  lines = appendCaptionDelta(lines, { id: 'reply1', role: 'assistant', text: '你好' })
  lines = appendCaptionDelta(lines, { id: 'reply1', role: 'assistant', text: '呀' })
  lines = appendCaptionDelta(lines, { id: 'user2', role: 'user', text: '讲个故事' })
  assert.deepEqual(lines.map(line => line.text), ['你好', '你好呀', '讲个故事'])
})
