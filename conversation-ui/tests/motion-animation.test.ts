import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { Euler, Quaternion, Vector3 } from 'three'
import { MotionAnimation, parseMotion } from '../src/avatar/WaveAnimation.ts'
import { AvatarRuntime } from '../src/avatar/AvatarRuntime.ts'
import { createVrmRig } from './fixtures/vrm-rig.ts'

const wave = JSON.parse(readFileSync(new URL('../public/animations/wave.motion.json', import.meta.url), 'utf8'))
// Synthetic head track tests player ownership only; it is not a shipped motion asset.
const headClip = { version: 1, name: 'shake_head', duration: 1,
  times: [0, 0.5, 1], rotations: { head: [0, 0, 0, 1, ...new Quaternion().setFromAxisAngle({ x: 0, y: 1, z: 0 }, 0.3).toArray(), 0, 0, 0, 1] } }

test('generic motion rejects unknown bones, empty tracks and unsafe names', () => {
  assert.equal(parseMotion(headClip).name, 'shake_head')
  for (const data of [
    { ...headClip, name: '../escape' },
    { ...headClip, rotations: {} },
    { ...headClip, rotations: { hips: headClip.rotations.head } },
    { ...headClip, rotations: { head: [0, 0, 0, 0] } },
  ]) assert.throws(() => parseMotion(data))
})

test('incremental registration preserves an active pose and can add a new bone channel', () => {
  const { humanoid } = createVrmRig()
  const player = new MotionAnimation(parseMotion(wave), humanoid)
  const runtime = new AvatarRuntime()
  runtime.act('wave'); runtime.motionStarted = 0
  player.restoreBase(); player.apply(runtime, 0.8)
  const arm = humanoid.getNormalizedBoneNode('rightUpperArm')!
  const raised = arm.quaternion.clone()
  player.registerMotion(parseMotion(headClip), humanoid)
  assert.ok(raised.angleTo(arm.quaternion) < 1e-6)
  runtime.registerMotion('shake_head', 1)
  runtime.act('shake_head'); runtime.motionStarted = 0.8
  player.restoreBase(); player.apply(runtime, 0.8)
  assert.ok(raised.angleTo(arm.quaternion) < 1e-6)
  player.restoreBase(); player.apply(runtime, 1.3)
  assert.ok(humanoid.getNormalizedBoneNode('head')!.quaternion.angleTo(new Quaternion()) > 0.1)
})

test('registering a new channel retains the procedural pose already displayed by that bone', () => {
  const { humanoid } = createVrmRig()
  const player = new MotionAnimation(parseMotion(wave), humanoid)
  const runtime = new AvatarRuntime()
  runtime.act('wave'); runtime.motionStarted = 0
  player.restoreBase(); player.apply(runtime, 0.1)
  const head = humanoid.getNormalizedBoneNode('head')!
  head.rotation.y = 0.4
  const displayed = head.quaternion.clone()
  player.registerMotion(parseMotion(headClip), humanoid)
  player.restoreBase(); player.apply(runtime, 0.1)
  assert.ok(head.quaternion.angleTo(displayed) < 1e-6)
})

test('switching clips preserves displayed pose and releases outgoing tracks', () => {
  const { humanoid } = createVrmRig()
  const player = new MotionAnimation([parseMotion(wave), parseMotion(headClip)], humanoid)
  const runtime = new AvatarRuntime()
  runtime.registerMotion('shake_head', 1)
  const arm = humanoid.getNormalizedBoneNode('rightUpperArm')!
  const head = humanoid.getNormalizedBoneNode('head')!
  const armRest = arm.quaternion.clone()
  const headRest = head.quaternion.clone()
  const frame = (now: number) => { player.restoreBase(); player.apply(runtime, now) }
  runtime.act('wave'); runtime.motionStarted = 0
  frame(0); frame(0.8)
  const raised = arm.quaternion.clone()
  assert.ok(raised.angleTo(armRest) > 0.3)
  runtime.act('shake_head'); runtime.motionStarted = 0.8
  frame(0.8)
  assert.ok(arm.quaternion.angleTo(raised) < 1e-6)
  frame(1.3)
  assert.ok(arm.quaternion.angleTo(armRest) < 1e-6)
  assert.ok(head.quaternion.angleTo(headRest) > 0.1)
  const turned = head.quaternion.clone()
  runtime.interrupt(); frame(1.3)
  assert.ok(head.quaternion.angleTo(turned) < 1e-6)
  frame(1.6)
  assert.ok(head.quaternion.angleTo(headRest) < 1e-6)
})

test('unprepared actions cannot execute and loaded durations control completion', () => {
  const runtime = new AvatarRuntime()
  assert.equal(runtime.act('clap'), false)
  runtime.registerMotion('clap', 3.7)
  assert.equal(runtime.act('clap'), true)
  assert.equal(runtime.getMotionDuration(), 3.7)
  runtime.motionStarted = 0
  runtime.finishMotion(3)
  assert.equal(runtime.motion, 'clap')
  runtime.finishMotion(3.8)
  assert.equal(runtime.motion, 'idle')
  runtime.clearImportedMotions()
  assert.equal(runtime.act('clap'), false)
})

test('fresh shake-head asset turns both ways without owning the arms', () => {
  const motion = parseMotion(JSON.parse(readFileSync(new URL('../public/animations/shake_head.motion.json', import.meta.url), 'utf8')))
  assert.deepEqual(Object.keys(motion.rotations).sort(), ['head', 'neck'])
  const { humanoid } = createVrmRig()
  const player = new MotionAnimation(motion, humanoid)
  const runtime = new AvatarRuntime()
  runtime.registerMotion('shake_head', motion.duration)
  runtime.act('shake_head'); runtime.motionStarted = 0
  const arm = humanoid.getNormalizedBoneNode('rightUpperArm')!
  const armRest = arm.quaternion.clone()
  const head = humanoid.getNormalizedBoneNode('head')!
  const neck = humanoid.getNormalizedBoneNode('neck')!
  const yaws: number[] = []
  for (let i = 0; i <= 90; i++) {
    runtime.finishMotion(i / 30)
    player.restoreBase(); player.apply(runtime, i / 30)
    yaws.push(new Euler().setFromQuaternion(neck.quaternion.clone().multiply(head.quaternion)).y)
    assert.ok(arm.quaternion.angleTo(armRest) < 1e-6)
  }
  assert.ok(Math.min(...yaws) < -0.2)
  assert.ok(Math.max(...yaws) > 0.2)
  runtime.stopMotion(); player.restoreBase(); player.apply(runtime, 3.5)
  assert.ok(head.quaternion.angleTo(new Quaternion()) < 1e-6)
})

test('fresh explain asset raises both hands and returns to the idle arm pose', () => {
  const motion = parseMotion(JSON.parse(readFileSync(new URL('../public/animations/explain.motion.json', import.meta.url), 'utf8')))
  const { scene, humanoid } = createVrmRig()
  const player = new MotionAnimation(motion, humanoid)
  const runtime = new AvatarRuntime()
  runtime.registerMotion('explain', motion.duration)
  runtime.act('explain'); runtime.motionStarted = 0
  scene.updateMatrixWorld(true)
  const hands = ['leftHand', 'rightHand'].map(name => humanoid.getNormalizedBoneNode(name as 'leftHand' | 'rightHand')!)
  const heights = hands.map(hand => hand.getWorldPosition(new Vector3()).y)
  const raised = [false, false]
  const arm = humanoid.getNormalizedBoneNode('rightUpperArm')!
  const rest = arm.quaternion.clone()
  for (let i = 0; i <= 120; i++) {
    runtime.finishMotion(i / 30)
    player.restoreBase(); player.apply(runtime, i / 30)
    scene.updateMatrixWorld(true)
    hands.forEach((hand, j) => { if (hand.getWorldPosition(new Vector3()).y > heights[j] + 0.05) raised[j] = true })
  }
  assert.deepEqual(raised, [true, true])
  runtime.stopMotion(); player.restoreBase(); player.apply(runtime, 4.5)
  assert.ok(arm.quaternion.angleTo(rest) < 1e-6)
})
