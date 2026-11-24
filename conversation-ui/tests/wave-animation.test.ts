import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { Quaternion, Vector3 } from 'three'
import { VRMHumanoid } from '@pixiv/three-vrm'
import { createMotionClip, createWaveClip, parseWaveMotion, WaveAnimation, WAVE_BONES } from '../src/avatar/WaveAnimation.ts'
import { AvatarRuntime } from '../src/avatar/AvatarRuntime.ts'
import { createVrmRig } from './fixtures/vrm-rig.ts'

const json = readFileSync(new URL('../public/animations/wave.motion.json', import.meta.url), 'utf8')
const motion = parseWaveMotion(JSON.parse(json))

function setup() {
  const { scene, humanoid } = createVrmRig()
  const player = new WaveAnimation([], humanoid)
  player.registerMotion(motion, humanoid, 'right-arm')
  const runtime = new AvatarRuntime()
  runtime.setWaveDuration(motion.duration)
  runtime.act('wave')
  runtime.motionStarted = 0
  const frame = (now: number) => {
    runtime.finishMotion(now)
    player.restoreBase()
    player.apply(runtime, now)
    scene.updateMatrixWorld(true)
  }
  return { scene, humanoid, player, runtime, frame }
}

test('the real FBX export is a compact 90-frame upper-body gesture without mesh or root tracks', () => {
  assert.equal(motion.times.length, 90)
  assert.ok(Math.abs(motion.duration - 2.9666667) < 0.001)
  assert.deepEqual(Object.keys(motion.rotations).sort(), [...WAVE_BONES].sort())
  assert.ok(Buffer.byteLength(json) < 100_000)
  assert.ok(JSON.parse(json).source.sha256.match(/^[0-9a-f]{64}$/))
})

test('the actual VRM raises its right hand above its shoulder while its left hand stays down', () => {
  const { humanoid, frame } = setup()
  const right = humanoid.getNormalizedBoneNode('rightHand')!
  const left = humanoid.getNormalizedBoneNode('leftHand')!
  const shoulder = humanoid.getNormalizedBoneNode('rightUpperArm')!
  let highest = -Infinity
  for (let i = 0; i <= 90; i++) {
    frame(i / 30)
    const height = right.getWorldPosition(new Vector3()).y - shoulder.getWorldPosition(new Vector3()).y
    highest = Math.max(highest, height)
    assert.ok(left.getWorldPosition(new Vector3()).y < shoulder.getWorldPosition(new Vector3()).y - 0.2)
  }
  assert.ok(highest > 0.12, 'the animation must visibly raise the anatomical right hand')
})

test('the dressed wave preserves torso and left-arm idle while retaining the real right-arm tracks', () => {
  const { humanoid } = createVrmRig()
  const clip = createMotionClip(motion, humanoid, 'right-arm')
  const names = ['rightShoulder', 'rightUpperArm', 'rightLowerArm', 'rightHand'] as const
  assert.deepEqual(clip.tracks.map(track => track.name).sort(), names.map(name => humanoid.getNormalizedBoneNode(name)!.uuid + '.quaternion').sort())
  // Other models and explicit standard retargeting retain the original clip.
  assert.equal(createWaveClip(motion, humanoid).tracks.length, WAVE_BONES.length)
  const { humanoid: rig, frame } = setup()
  const resting = ['spine', 'chest', 'upperChest', 'leftShoulder', 'leftUpperArm', 'leftLowerArm', 'leftHand']
    .map(name => rig.getNormalizedBoneNode(name as 'head')!)
  const before = resting.map(node => node.quaternion.clone())
  for (let i = 0; i <= 200; i++) {
    frame(i / 60)
    resting.forEach((node, index) => assert.ok(node.quaternion.angleTo(before[index]) < 1e-6, `${node.name} must keep its idle pose`))
  }
})

test('completion and interruption blend back to the exact idle arm pose without a one-frame snap', () => {
  const { humanoid, runtime, frame } = setup()
  const arm = humanoid.getNormalizedBoneNode('rightUpperArm')!
  const idle = arm.quaternion.clone()
  frame(0)
  frame(0.8)
  const raised = arm.quaternion.clone()
  assert.ok(raised.angleTo(idle) > 0.3)
  runtime.interrupt()
  frame(0.8)
  assert.ok(arm.quaternion.angleTo(raised) < 1e-6)
  frame(1.1)
  assert.ok(arm.quaternion.angleTo(idle) < 1e-6)
  frame(1.2)
  assert.ok(arm.quaternion.angleTo(idle) < 1e-6, 'clip output cannot feed back into the idle base pose')
})

for (const interrupted of [false, true]) test(`wave ${interrupted ? 'interruption' : 'completion'} leaves wrists beside the torso without reaching forward or spreading wide`, () => {
  const { humanoid, runtime, frame } = setup()
  for (let i = 0; i <= 240; i++) {
    if (interrupted && i === 60) runtime.interrupt()
    frame(i / 60)
  }
  const position = (name: string) => humanoid.getNormalizedBoneNode(name as 'head')!.getWorldPosition(new Vector3())
  const lateral = position('leftUpperArm').sub(position('rightUpperArm')).normalize()
  const forward = lateral.clone().cross(new Vector3(0, 1, 0)).normalize()
  for (const side of ['left', 'right']) {
    const shoulder = position(side + 'UpperArm'), elbow = position(side + 'LowerArm'), wrist = position(side + 'Hand')
    const length = shoulder.distanceTo(elbow) + elbow.distanceTo(wrist)
    const offset = wrist.sub(shoulder)
    assert.ok(Math.abs(offset.dot(forward)) < length * 0.1, `${side} wrist must rest beside the torso, not reach in front`)
    assert.ok(Math.abs(offset.dot(lateral)) < length * 0.34, `${side} arm must not spread wide to avoid the dress`)
    assert.ok(offset.y < -length * 0.9, `${side} arm must hang down`)
  }
})

test('repeating wave mid-gesture starts from the displayed pose and keeps the clip duration', () => {
  const { humanoid, runtime, frame } = setup()
  const arm = humanoid.getNormalizedBoneNode('rightUpperArm')!
  const idle = arm.quaternion.clone()
  frame(0)
  frame(0.8)
  const before = arm.quaternion.clone()
  runtime.act('wave')
  runtime.motionStarted = 0.8
  frame(0.8)
  assert.ok(arm.quaternion.angleTo(before) < 1e-6)
  frame(3.4)
  assert.equal(runtime.motion, 'wave', 'a generated wave is not truncated by the old 2.5 second limit')
  frame(3.8)
  frame(4.1)
  assert.equal(runtime.motion, 'idle')
  assert.ok(arm.quaternion.angleTo(idle) < 1e-6)
})

test('wave leaves hips, legs, head and finger channels under their existing owners', () => {
  const { humanoid, frame } = setup()
  const names = ['hips', 'leftUpperLeg', 'rightUpperLeg', 'leftLowerLeg', 'rightLowerLeg', 'head', 'leftIndexProximal'] as const
  const before = names.map(name => {
    const node = humanoid.getNormalizedBoneNode(name)!
    node.rotation.x = 0.07
    return { node, q: node.quaternion.clone(), position: node.position.clone() }
  })
  frame(0)
  frame(1)
  for (const { node, q, position } of before) {
    assert.ok(node.quaternion.angleTo(q) < 1e-6)
    assert.ok(node.position.distanceTo(position) < 1e-9)
  }
})

test('VRM 1.x arm direction uses the opposite rotation signs without swapping left and right', () => {
  const vrm0 = createVrmRig().humanoid
  const canonical = createVrmRig().humanoid
  canonical.getNormalizedBoneNode('leftLowerArm')!.position.x *= -1
  canonical.getNormalizedBoneNode('rightLowerArm')!.position.x *= -1
  const find = (clip: ReturnType<typeof createWaveClip>, id: string) => clip.tracks.find(track => track.name === id + '.quaternion')!
  const oldTrack = find(createWaveClip(motion, vrm0), vrm0.getNormalizedBoneNode('rightUpperArm')!.uuid)
  const newTrack = find(createWaveClip(motion, canonical), canonical.getNormalizedBoneNode('rightUpperArm')!.uuid)
  const oldQ = new Quaternion().fromArray(oldTrack.values, 30 * 4).normalize()
  const newQ = new Quaternion().fromArray(newTrack.values, 30 * 4).normalize()
  assert.ok(oldQ.angleTo(new Quaternion(-newQ.x, newQ.y, -newQ.z, newQ.w)) < 1e-6)
})

test('a missing upperChest track is composed into chest rather than silently dropping torso motion', () => {
  const { scene, humanoid: original } = createVrmRig()
  scene.rotation.y = 0
  scene.updateMatrixWorld(true)
  const bones = { ...original.rawHumanBones }
  delete bones.upperChest
  const humanoid = new VRMHumanoid(bones)
  const clip = createWaveClip(motion, humanoid)
  const track = clip.tracks.find(track => track.name === humanoid.getNormalizedBoneNode('chest')!.uuid + '.quaternion')!
  const expected = new Quaternion().fromArray(motion.rotations.chest, 120)
    .multiply(new Quaternion().fromArray(motion.rotations.upperChest, 120)).normalize()
  expected.set(-expected.x, expected.y, -expected.z, expected.w)
  assert.ok(new Quaternion().fromArray(track.values, 120).normalize().angleTo(expected) < 1e-6)
})

test('invalid keyframes and malformed rotations are rejected before playback', () => {
  for (const modify of [
    (data: any) => { data.duration = Infinity },
    (data: any) => { data.times[1] = 0 },
    (data: any) => { data.rotations.rightUpperArm[1] = NaN },
    (data: any) => { data.rotations.rightUpperArm = [] },
  ]) {
    const data = JSON.parse(json)
    modify(data)
    assert.throws(() => parseWaveMotion(data))
  }
})
