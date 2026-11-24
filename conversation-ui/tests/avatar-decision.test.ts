import assert from 'node:assert/strict'
import test from 'node:test'
import { AvatarRuntime } from '../src/avatar/AvatarRuntime.ts'
import { parseAvatarEvent } from '../src/avatar/avatarEvents.ts'

function start(avatar: AvatarRuntime, id: string, now = 0) {
  avatar.registerMotion('wave', 2.966667)
  avatar.receive({ type: 'avatar.response', responseId: id, phase: 'created', decisionMode: 'model' }, now)
}
function decide(avatar: AvatarRuntime, id: string, now = 0, gesture: 'none' | 'wave' | 'nod' = 'wave') {
  return avatar.receive({ type: 'avatar.decision', responseId: id, cueId: 'opening', segmentId: 'opening', intent: gesture === 'wave' ? 'greet' : gesture === 'nod' ? 'agree' : 'none', emotion: 'happy', expiresInMs: 1500 }, now)
}

test('model decision waits for speech and starts the wave timeline only once', () => {
  const avatar = new AvatarRuntime()
  start(avatar, 'r1'); decide(avatar, 'r1')
  avatar.update(0.1, 0)
  assert.equal(avatar.motion, 'idle'); assert.equal(avatar.emotion, 'neutral')
  avatar.update(0.2, 0.1)
  assert.equal(avatar.motion, 'wave'); assert.equal(avatar.emotion, 'happy')
  assert.equal(avatar.motionStarted, 0.2)
  assert.equal(decide(avatar, 'r1', 0.3), false)
  assert.equal(avatar.motionSequence, 1)
})

test('gesture acknowledgement is sent only when automatic animation starts', () => {
  const avatar = new AvatarRuntime()
  const starts: unknown[] = []
  avatar.setGestureStartedHandler(event => starts.push(event))
  start(avatar, 'welcome'); decide(avatar, 'welcome'); avatar.update(2, 0)
  assert.equal(starts.length, 0)
  start(avatar, 'greeting', 3); decide(avatar, 'greeting', 3)
  avatar.update(3.1, 0.1); avatar.update(3.2, 0.1)
  assert.deepEqual(starts, [{ responseId: 'greeting', cueId: 'opening', intent: 'greet', motionId: 'wave' }])
})

test('expired, interrupted and superseded decisions never animate another response', () => {
  const avatar = new AvatarRuntime()
  start(avatar, 'r1'); decide(avatar, 'r1')
  avatar.update(2, 0.1); assert.equal(avatar.motion, 'idle')
  avatar.interrupt(); assert.equal(decide(avatar, 'r1', 2), false)
  avatar.stopListening(); start(avatar, 'r2', 3)
  assert.equal(decide(avatar, 'r1', 3), false)
  decide(avatar, 'r2', 3); avatar.update(3.1, 0.1)
  assert.equal(avatar.motion, 'wave')
  avatar.receive({ type: 'avatar.response', responseId: 'r2', phase: 'cancelled' }, 3.2)
  assert.equal(avatar.motion, 'idle')
})

test('explicit gestures win and survive their original spoken response cancellation', () => {
  const avatar = new AvatarRuntime()
  start(avatar, 'r1'); decide(avatar, 'r1'); avatar.act('nod')
  avatar.update(0.2, 0.1); assert.equal(avatar.motion, 'nod')
  assert.equal(decide(avatar, 'r1', 0.3), false)
  avatar.receive({ type: 'avatar.response', responseId: 'r1', phase: 'cancelled' }, 0.4)
  assert.equal(avatar.motion, 'nod')
})

test('same gesture has a cooldown while none can still set an expression', () => {
  const avatar = new AvatarRuntime()
  start(avatar, 'r1'); decide(avatar, 'r1'); avatar.update(0.1, 0.1)
  avatar.finishMotion(4)
  start(avatar, 'r2', 10); decide(avatar, 'r2', 10); avatar.update(10.1, 0.1)
  assert.equal(avatar.motion, 'idle')
  start(avatar, 'r3', 31); decide(avatar, 'r3', 31, 'none'); avatar.update(31.1, 0.1)
  assert.equal(avatar.motion, 'idle'); assert.equal(avatar.emotion, 'happy')
  start(avatar, 'r4', 32); decide(avatar, 'r4', 32); avatar.update(32.1, 0.1)
  assert.equal(avatar.motion, 'wave')
})

test('an explicit animation cannot be overwritten by a model suggestion', () => {
  const avatar = new AvatarRuntime()
  avatar.act('dance'); start(avatar, 'r1'); decide(avatar, 'r1'); avatar.update(0.1, 0.1)
  assert.equal(avatar.motion, 'dance')
})

test('model mode disables local transcript emotion inference', () => {
  const avatar = new AvatarRuntime()
  start(avatar, 'r1'); avatar.observeTranscript("I'm happy for you", 'r1'); avatar.update(0.1, 0.1)
  assert.equal(avatar.emotion, 'neutral')
})

test('decision parser restricts actions, expressions and expiry', () => {
  const event = { type: 'avatar.decision', responseId: 'r', cueId: 'opening', segmentId: 'opening', intent: 'greet', emotion: 'happy', expiresInMs: 99999 }
  assert.deepEqual(parseAvatarEvent(event), { ...event, expiresInMs: 1500 })
  assert.equal(parseAvatarEvent({ ...event, intent: 'fly' }), null)
  assert.equal(parseAvatarEvent({ ...event, emotion: 'unknown' }), null)
  assert.equal(parseAvatarEvent({ ...event, expiresInMs: NaN }), null)
})
