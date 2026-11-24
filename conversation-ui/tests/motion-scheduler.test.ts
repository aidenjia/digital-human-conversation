import assert from 'node:assert/strict'
import test from 'node:test'
import { AvatarRuntime } from '../src/avatar/AvatarRuntime.ts'
import { parseAvatarEvent, type DecisionCue } from '../src/avatar/avatarEvents.ts'
import { MotionScheduler } from '../src/avatar/MotionScheduler.ts'
import { MOTION_CATALOG } from '../src/avatar/motionCatalog.ts'

const cue = (cueId: string, intent = 'greet', segmentId = 'opening') => ({
  type: 'avatar.decision', responseId: 'r', cueId, segmentId, intent,
  emotion: 'happy', expiresInMs: 1500,
})
function runtime() {
  const avatar = new AvatarRuntime()
  avatar.registerMotion('wave', 3)
  avatar.receive({ type: 'avatar.response', responseId: 'r', phase: 'created', decisionMode: 'model' }, 0)
  return avatar
}

test('intent protocol accepts independent cues and rejects legacy action names', () => {
  assert.deepEqual(parseAvatarEvent(cue('c1')), cue('c1'))
  assert.equal(parseAvatarEvent({ ...cue('c1'), cueId: '' }), null)
  assert.equal(parseAvatarEvent({ ...cue('c1'), intent: 'wave' }), null)
  assert.equal(parseAvatarEvent({ ...cue('c1'), segmentId: '' }), null)
})

test('denial waits for speech, plays the prepared head shake and stops on interruption', () => {
  const avatar = runtime(), starts: unknown[] = []
  avatar.registerMotion('shake_head', 1.35)
  avatar.setGestureStartedHandler(event => starts.push(event))
  const event = parseAvatarEvent(cue('denial', 'deny'))
  assert.ok(event)
  avatar.receive(event, 0)
  avatar.update(0.1, 0)
  assert.equal(avatar.motion, 'idle')
  avatar.update(0.2, 0.1)
  assert.equal(avatar.motion, 'shake_head')
  assert.deepEqual(starts, [{ responseId: 'r', cueId: 'denial', intent: 'deny', motionId: 'shake_head' }])
  avatar.interrupt()
  assert.equal(avatar.motion, 'idle')
})

test('explanation intent waits for playback, starts the prepared asset once and acknowledges its identity', () => {
  const avatar = runtime(), starts: unknown[] = []
  avatar.registerMotion('explain', 3)
  avatar.setGestureStartedHandler(event => starts.push(event))
  const event = parseAvatarEvent(cue('explanation', 'explain'))
  assert.ok(event, 'explain must be a supported semantic intent')
  assert.equal(avatar.receive(event, 0), true)
  avatar.update(0.1, 0)
  assert.equal(avatar.motion, 'idle')
  assert.equal(starts.length, 0)
  avatar.update(0.2, 0.1)
  assert.equal(avatar.motion, 'explain')
  assert.equal(avatar.receive(event, 0.3), false)
  avatar.update(0.3, 0.1)
  assert.deepEqual(starts, [{ responseId: 'r', cueId: 'explanation', intent: 'explain', motionId: 'explain' }])
  avatar.interrupt()
  assert.equal(avatar.motion, 'idle')
  assert.equal(avatar.receive(event, 0.4), false)
})

test('automatic explanation shares global cooldown and retains its own cooldown across other intents', () => {
  const avatar = runtime()
  avatar.registerMotion('explain', 3)
  const run = (id: string, intent: string, now: number) => {
    avatar.finishMotion(now)
    avatar.receive({ type: 'avatar.response', responseId: id, phase: 'created', decisionMode: 'model' }, now)
    const event = parseAvatarEvent({ ...cue(id, intent), responseId: id })
    assert.ok(event)
    avatar.receive(event, now)
    avatar.update(now + 0.1, 0.1)
    return avatar.motion
  }
  assert.equal(run('first', 'explain', 0), 'explain')
  assert.equal(run('global', 'agree', 4), 'idle')
  assert.equal(run('other', 'agree', 10), 'nod')
  assert.equal(run('same', 'explain', 20), 'idle')
  assert.equal(run('ready', 'explain', 31), 'explain')
})

test('unprepared or expired explanation is skipped without downloading or reviving after readiness', () => {
  const avatar = runtime()
  let downloads = 0
  avatar.setMotionLoader(async () => { downloads++; return true })
  const event = parseAvatarEvent(cue('unloaded', 'explain'))
  assert.ok(event)
  avatar.receive(event, 0)
  avatar.update(0.1, 0.1)
  assert.equal(avatar.motion, 'idle')
  assert.equal(downloads, 0)
  avatar.registerMotion('explain', 3)
  avatar.update(0.2, 0.1)
  assert.equal(avatar.motion, 'idle')
  avatar.receive({ type: 'avatar.response', responseId: 'expired', phase: 'created' }, 2)
  avatar.receive({ ...event, responseId: 'expired' }, 2)
  avatar.update(4, 0.1)
  assert.equal(avatar.motion, 'idle')
})

test('distinct cues in one response are accepted once and acknowledge concrete playback separately', () => {
  const avatar = runtime()
  const starts: unknown[] = []
  avatar.setGestureStartedHandler(event => starts.push(event))
  assert.equal(avatar.receive(parseAvatarEvent(cue('c1'))!, 0), true)
  assert.equal(avatar.receive(parseAvatarEvent(cue('c1'))!, 0), false)
  avatar.update(0.1, 0.1)
  assert.equal(avatar.motion, 'wave')
  assert.equal(avatar.receive(parseAvatarEvent(cue('c2', 'agree'))!, 8), true)
  avatar.finishMotion(8)
  avatar.update(8.2, 0.1)
  assert.equal(avatar.motion, 'nod')
  assert.deepEqual(starts, [
    { responseId: 'r', cueId: 'c1', intent: 'greet', motionId: 'wave' },
    { responseId: 'r', cueId: 'c2', intent: 'agree', motionId: 'nod' },
  ])
})

test('busy body queues a cue until expiry and never overlaps or replays an expired cue', () => {
  const avatar = runtime()
  avatar.motion = 'dance'
  assert.equal(avatar.receive(parseAvatarEvent(cue('c1'))!, 0), true)
  avatar.update(0.1, 0.1)
  assert.equal(avatar.motion, 'dance')
  avatar.motion = 'idle'
  avatar.update(2, 0.1)
  assert.equal(avatar.motion, 'idle')
  assert.equal(avatar.receive(parseAvatarEvent(cue('c1'))!, 2), false)
})

test('a busy body releases a queued cue before its deadline', () => {
  const avatar = runtime()
  avatar.motion = 'dance'
  avatar.receive(parseAvatarEvent(cue('queued'))!, 0)
  avatar.update(0.1, 0.1)
  assert.equal(avatar.motion, 'dance')
  avatar.motion = 'idle'
  avatar.update(0.5, 0.1)
  assert.equal(avatar.motion, 'wave')
})

test('catalog-only variants rotate deterministically and share their intent cooldown', () => {
  const wave = MOTION_CATALOG.find(asset => asset.id === 'wave')!
  const catalog = [wave, { ...wave, id: 'wave_soft' }, { ...wave, id: 'wave_trial', admission: 'preview' as const }]
  const scheduler = new MotionScheduler(catalog)
  const ready = new Set(catalog.map(asset => asset.id))
  const run = (id: string, now: number) => {
    scheduler.beginResponse('r')
    scheduler.enqueue(parseAvatarEvent(cue(id)) as DecisionCue, now)
    return scheduler.tick(now, true, true, ready).started?.motionId
  }
  assert.equal(run('first', 0), 'wave')
  assert.equal(run('too-soon', 10), undefined)
  assert.equal(run('second', 31), 'wave_soft')
  assert.equal(run('third', 62), 'wave')
})

test('late cue for a completed segment is rejected without affecting the next segment', () => {
  const avatar = runtime()
  avatar.markSpeechSegment('r', 's2')
  assert.equal(avatar.receive(parseAvatarEvent(cue('old'))!, 0), false)
  avatar.receive(parseAvatarEvent(cue('current', 'agree', 's2'))!, 0)
  avatar.update(0.1, 0.1)
  assert.equal(avatar.motion, 'nod')
})

test('barge-in cancels manual preview intent even when its download finishes later', async () => {
  const avatar = runtime()
  let finish: (loaded: boolean) => void = () => {}
  avatar.setMotionLoader(() => new Promise(resolve => { finish = resolve }))
  const pending = avatar.previewMotion('explain')
  avatar.interrupt()
  avatar.registerMotion('explain', 4)
  finish(true)
  assert.equal(await pending, false)
  assert.equal(avatar.motion, 'idle')
})

test('unloaded assets never start automatically but emotions still apply', () => {
  const avatar = new AvatarRuntime()
  avatar.receive({ type: 'avatar.response', responseId: 'r', phase: 'created' }, 0)
  avatar.receive(parseAvatarEvent(cue('c1'))!, 0)
  avatar.update(0.1, 0.1)
  assert.equal(avatar.motion, 'idle')
  assert.equal(avatar.emotion, 'happy')
})

test('unknown future segment waits for its playback boundary and interruption clears its queue', () => {
  const avatar = runtime()
  avatar.receive(parseAvatarEvent(cue('c1', 'greet', 's2'))!, 0)
  avatar.update(0.1, 0.1)
  assert.equal(avatar.motion, 'idle')
  avatar.markSpeechSegment('r', 's2')
  avatar.update(0.2, 0.1)
  assert.equal(avatar.motion, 'wave')
  avatar.interrupt()
  avatar.stopListening()
  avatar.update(1, 0.1)
  assert.equal(avatar.motion, 'idle')
})
