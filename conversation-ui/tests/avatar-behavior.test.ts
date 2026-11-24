import assert from 'node:assert/strict'
import test from 'node:test'
import { AvatarRuntime } from '../src/avatar/AvatarRuntime.ts'
import { expressionFromTranscript, parseAvatarEvent } from '../src/avatar/avatarEvents.ts'
import { GazeController } from '../src/avatar/GazeController.ts'

test('emotion waits for actual playback and expires after its duration', () => {
  const avatar = new AvatarRuntime()
  avatar.receive({ type: 'avatar.response', responseId: 'r1', phase: 'created' }, 10)
  avatar.receive({ type: 'avatar.expression', responseId: 'r1', name: 'happy', weight: 0.3, durationMs: 1000 }, 10)
  avatar.update(10.1, 0)
  assert.equal(avatar.emotion, 'neutral')
  assert.equal(avatar.state, 'thinking')
  avatar.update(10.5, 0.1)
  assert.equal(avatar.emotion, 'happy')
  assert.equal(avatar.state, 'speaking')
  avatar.update(11.6, 0)
  assert.equal(avatar.emotion, 'neutral')
})

test('late events from an interrupted response cannot revive expressions', () => {
  const avatar = new AvatarRuntime()
  avatar.receive({ type: 'avatar.response', responseId: 'old', phase: 'created' }, 10)
  avatar.receive({ type: 'avatar.expression', responseId: 'old', name: 'happy', weight: 0.4, durationMs: 3000 }, 10)
  avatar.interrupt()
  avatar.stopListening()
  assert.equal(avatar.receive({ type: 'avatar.response', responseId: 'old', phase: 'created' }), false)
  assert.equal(avatar.receive({ type: 'avatar.expression', responseId: 'old', name: 'angry', weight: 0.4, durationMs: 3000 }), false)
  avatar.receive({ type: 'avatar.response', responseId: 'new', phase: 'created' }, 12)
  avatar.update(12.1, 0.2)
  assert.equal(avatar.emotion, 'neutral')
  assert.equal(avatar.receive({ type: 'avatar.response', responseId: 'old', phase: 'done' }), false)
})

test('missing playback drops stale emotions rather than applying them on another turn', () => {
  const avatar = new AvatarRuntime()
  avatar.receive({ type: 'avatar.response', responseId: 'r1', phase: 'created' }, 1)
  avatar.receive({ type: 'avatar.expression', responseId: 'r1', name: 'sad', weight: 0.3, durationMs: 1000 }, 1)
  avatar.update(7, 0.1)
  assert.equal(avatar.emotion, 'neutral')
})

test('tool-triggered response cancellation preserves its dispatched gesture', () => {
  const avatar = new AvatarRuntime()
  avatar.receive({ type: 'avatar.response', responseId: 'tool-turn', phase: 'created' }, 1)
  avatar.act('wave')
  avatar.receive({ type: 'avatar.response', responseId: 'tool-turn', phase: 'cancelled' }, 2)
  assert.equal(avatar.motion, 'wave')
  avatar.interrupt()
  assert.equal(avatar.motion, 'idle')
})

test('tool-associated happy expression survives the tool speech cancellation', () => {
  const avatar = new AvatarRuntime()
  avatar.receive({ type: 'avatar.response', responseId: 'tool', phase: 'created' })
  avatar.act('heartbeat')
  avatar.receive({ type: 'avatar.response', responseId: 'tool', phase: 'cancelled' })
  avatar.receive({ type: 'avatar.response', responseId: 'confirmation', phase: 'created' })
  assert.equal(avatar.emotion, 'happy')
  avatar.interrupt()
  assert.equal(avatar.emotion, 'neutral')
})

test('explicit expression commands survive tool cancellation and stop on interruption', () => {
  for (const emotion of ['happy', 'sad', 'angry', 'surprised'] as const) {
    const avatar = new AvatarRuntime()
    avatar.receive({ type: 'avatar.response', responseId: 'tool', phase: 'created' })
    assert.equal(avatar.act(`expression_${emotion}`), true)
    avatar.receive({ type: 'avatar.response', responseId: 'tool', phase: 'cancelled' })
    avatar.receive({ type: 'avatar.response', responseId: 'confirmation', phase: 'created' })
    assert.equal(avatar.emotion, emotion)
    avatar.interrupt()
    assert.equal(avatar.emotion, 'neutral')
  }
})

test('explicit expression starts with confirmation speech even after a long tool delay', () => {
  const avatar = new AvatarRuntime()
  const now = performance.now() / 1000
  avatar.receive({ type: 'avatar.response', responseId: 'tool', phase: 'created' }, now)
  avatar.act('expression_sad')
  avatar.receive({ type: 'avatar.response', responseId: 'tool', phase: 'cancelled' }, now + 0.1)
  avatar.update(now + 4, 0)
  avatar.receive({ type: 'avatar.response', responseId: 'confirmation', phase: 'created' }, now + 4)
  avatar.update(now + 4.2, 0.1)
  assert.equal(avatar.emotion, 'sad')
  assert.ok(avatar.emotionUntil > now + 6)
})

test('fragmented assistant phrases trigger at most one cue per response', () => {
  const now = performance.now() / 1000
  const avatar = new AvatarRuntime()
  avatar.receive({ type: 'avatar.response', responseId: 'r1', phase: 'created' }, now)
  avatar.observeTranscript('真为', 'r1')
  avatar.observeTranscript('你高兴！', 'r1')
  avatar.update(now + 0.1, 0.1)
  assert.equal(avatar.emotion, 'happy')
  const until = avatar.emotionUntil
  avatar.observeTranscript('这太意外了', 'r1')
  avatar.update(now + 0.2, 0.1)
  assert.equal(avatar.emotion, 'happy')
  assert.equal(avatar.emotionUntil, until)
  assert.equal(expressionFromTranscript('你今天开心吗？'), null)
  assert.equal(expressionFromTranscript('好的，我们接着聊'), null)
  assert.equal(expressionFromTranscript('我不替你开心'), null)
  assert.equal(expressionFromTranscript('请不要说“真为你高兴”'), null)
})

test('state holds speaking over brief pauses and falls back from stalled thinking', () => {
  const avatar = new AvatarRuntime()
  avatar.receive({ type: 'avatar.response', responseId: 'r1', phase: 'created' }, 10)
  avatar.update(10.1, 0.1)
  avatar.update(10.3, 0)
  assert.equal(avatar.state, 'speaking')
  avatar.update(10.6, 0)
  assert.equal(avatar.state, 'thinking')
  avatar.update(19, 0)
  assert.equal(avatar.state, 'idle')
})

test('expression event parser enforces response identity and finite bounded weights', () => {
  assert.equal(parseAvatarEvent({ type: 'avatar.expression', name: 'happy' }), null)
  assert.equal(parseAvatarEvent({ type: 'avatar.expression', responseId: 'r1', name: 'unknown' }), null)
  assert.equal(parseAvatarEvent({ type: 'avatar.expression', responseId: 'r1', name: 'happy', weight: NaN }), null)
  assert.deepEqual(parseAvatarEvent({ type: 'avatar.expression', responseId: 'r1', name: 'happy', weight: 99, durationMs: 999999 }), {
    type: 'avatar.expression', responseId: 'r1', name: 'happy', weight: 0.7, durationMs: 5000,
  })
})

test('gaze keeps a target during dwell and changes behavior when conversation state changes', () => {
  const gaze = new GazeController(() => 0.5)
  const listening = gaze.update('listening', 1)
  assert.ok(Math.abs(listening.x) < 0.03)
  assert.strictEqual(gaze.update('listening', 1.1), listening)
  const thinking = gaze.update('thinking', 1.2)
  assert.ok(Math.abs(thinking.x) >= 0.12)
  assert.ok(Math.abs(thinking.x) <= 0.24)
  assert.ok(thinking.y > 0)
})
