import assert from 'node:assert/strict'
import test from 'node:test'
import { AvatarRuntime, mouthTarget, smooth } from '../src/avatar/AvatarRuntime.ts'

test('barge-in switches to listening, cancels the gesture, and allows speech to resume', () => {
  const avatar = new AvatarRuntime()
  avatar.setAudioReader(() => 0.15)
  avatar.act('dance')
  avatar.interrupt()
  assert.equal(avatar.listening, true)
  assert.equal(avatar.motion, 'idle')
  avatar.stopListening()
  assert.equal(avatar.listening, false)
  assert.ok(mouthTarget(avatar.readLevel()) > 0)
})

test('ending a session detaches audio and restores pose, expression and camera', () => {
  const avatar = new AvatarRuntime()
  avatar.setAudioReader(() => 0.8)
  avatar.act('heartbeat')
  avatar.act('zoom_in')
  avatar.act('change_scene')
  avatar.interrupt()
  avatar.reset()
  assert.equal(avatar.readLevel(), 0)
  assert.equal(avatar.motion, 'idle')
  assert.equal(avatar.emotion, 'neutral')
  assert.equal(avatar.listening, false)
  assert.equal(avatar.zoom, 1)
  assert.equal(avatar.scene, 0)
})

test('gestures finish and unsupported commands do not start a fake gesture', () => {
  const avatar = new AvatarRuntime()
  avatar.act('dance')
  avatar.finishMotion(avatar.motionStarted + 3)
  assert.equal(avatar.motion, 'dance')
  avatar.finishMotion(avatar.motionStarted + 7)
  assert.equal(avatar.motion, 'idle')
  assert.equal(avatar.act('sing'), false)
  assert.equal(avatar.motion, 'idle')
})

test('mouth smoothing is consistent at 30 and 60 FPS and rejects the noise floor', () => {
  const integrate = (fps: number) => {
    let weight = 0
    for (let i = 0; i < fps; i++) weight = smooth(weight, 1, 6, 1 / fps)
    return weight
  }
  assert.ok(Math.abs(integrate(30) - integrate(60)) < 1e-10)
  assert.equal(mouthTarget(0.005), 0)
  assert.equal(mouthTarget(1), 1)
})
