import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { Quaternion, Ray, Triangle, Vector3 } from 'three'
import { AvatarRuntime } from '../src/avatar/AvatarRuntime.ts'
import { MotionAnimation, parseMotion } from '../src/avatar/WaveAnimation.ts'
import { createVrmRig } from './fixtures/vrm-rig.ts'
import { createDressProbe } from './fixtures/vrm-garment.ts'
import { createMotionClip } from '../src/avatar/WaveAnimation.ts'

test('shake-head preview finishes briskly within 1.6 seconds', () => {
  const data = parseMotion(JSON.parse(readFileSync(new URL('../public/animations/shake_head.motion.json', import.meta.url), 'utf8')))
  assert.ok(data.duration >= 1.1 && data.duration <= 1.6)
})

for (const armScale of [0.85, 1, 1.15]) test(`explanation fits arm scale ${armScale} with wrists above elbows and palms up`, () => {
  const motion = parseMotion(JSON.parse(readFileSync(new URL('../public/animations/explain.motion.json', import.meta.url), 'utf8')))
  const { scene, humanoid } = createVrmRig()
  for (const side of ['left', 'right']) {
    humanoid.getNormalizedBoneNode((side + 'LowerArm') as 'head')!.position.multiplyScalar(armScale)
    humanoid.getNormalizedBoneNode((side + 'Hand') as 'head')!.position.multiplyScalar(armScale)
  }
  scene.rotation.y += 0.4
  scene.scale.setScalar(0.8)
  scene.updateMatrixWorld(true)
  const player = new MotionAnimation(motion, humanoid)
  const runtime = new AvatarRuntime()
  runtime.registerMotion('explain', motion.duration)
  runtime.act('explain'); runtime.motionStarted = 0
  const pos = (name: string) => humanoid.getNormalizedBoneNode(name as 'head')!.getWorldPosition(new Vector3())
  for (const time of [0.9, 1.1, 1.3]) {
    player.restoreBase(); player.apply(runtime, time); scene.updateMatrixWorld(true)
    const left = pos('leftUpperArm'), right = pos('rightUpperArm')
    const center = left.clone().add(right).multiplyScalar(0.5)
    const sideAxis = left.clone().sub(right).normalize()
    const front = sideAxis.clone().cross(new Vector3(0, 1, 0)).normalize()
    const torso = center.y - pos('hips').y
    for (const side of ['left', 'right']) {
      const hand = pos(side + 'Hand'), elbow = pos(side + 'LowerArm')
      assert.ok(hand.y >= elbow.y - torso * 0.03, `${side} wrist must not hang below elbow at ${time}`)
      assert.ok(hand.y > pos('hips').y + torso * 0.35, `${side} hand must be above waist`)
      assert.ok(hand.clone().sub(center).dot(front) > torso * 0.12, `${side} hand must be in front of torso`)
      assert.ok(elbow.clone().sub(center).dot(front) > -torso * 0.04, `${side} elbow must not pull behind torso`)
      const normal = pos(side + 'IndexProximal').sub(hand).cross(pos(side + 'LittleProximal').sub(hand)).normalize()
        .multiplyScalar(side === 'left' ? -1 : 1)
      assert.ok(normal.y > 0.7, `${side} palm must face up`)
    }
  }
})

test('adapted explanation has no elbow or wrist flip during entry and recovery', () => {
  const motion = parseMotion(JSON.parse(readFileSync(new URL('../public/animations/explain.motion.json', import.meta.url), 'utf8')))
  const { humanoid } = createVrmRig()
  const player = new MotionAnimation(motion, humanoid)
  const runtime = new AvatarRuntime()
  runtime.registerMotion('explain', motion.duration)
  runtime.act('explain'); runtime.motionStarted = 0
  const nodes = ['leftUpperArm', 'leftLowerArm', 'leftHand', 'rightUpperArm', 'rightLowerArm', 'rightHand'].map(name => humanoid.getNormalizedBoneNode(name as 'head')!)
  let previous = nodes.map(node => node.quaternion.clone())
  for (let i = 0; i <= 270; i++) {
    const time = i / 60
    runtime.finishMotion(time); player.restoreBase(); player.apply(runtime, time)
    nodes.forEach((node, j) => assert.ok(previous[j].angleTo(node.quaternion) < 0.14, `${node.name} exceeds 8 degrees/frame at ${time}`))
    previous = nodes.map(node => node.quaternion.clone())
  }
})

test('explanation starts and ends in the exact current idle pose', () => {
  const data = parseMotion(JSON.parse(readFileSync(new URL('../public/animations/explain.motion.json', import.meta.url), 'utf8')))
  const { humanoid } = createVrmRig()
  const clip = createMotionClip(data, humanoid)
  for (const track of clip.tracks) {
    const node = humanoid.normalizedHumanBonesRoot.getObjectByProperty('uuid', track.name.split('.')[0])!
    const q = node.quaternion.clone()
    assert.ok(q.angleTo(q.clone().fromArray(track.values, 0).normalize()) < 1e-5, `${node.name} starts away from idle`)
    assert.ok(q.angleTo(q.clone().fromArray(track.values, track.values.length - 4).normalize()) < 1e-5, `${node.name} ends away from idle`)
  }
})

test('natural idle preserves neutral forearms instead of forcing outward skirt clearance', () => {
  const { humanoid } = createVrmRig()
  for (const side of ['left', 'right']) {
    const lower = humanoid.getNormalizedBoneNode((side + 'LowerArm') as 'head')!
    assert.ok(lower.quaternion.angleTo(new Quaternion()) < 1e-6, `${side} idle forearm must retain its original neutral rotation`)
  }
})

test('dressed idle keeps hand and finger points clear of the actual skirt without reaching forward', () => {
  const rig = createVrmRig(), dress = createDressProbe(rig)
  const triangles = dress.triangles(), hit = new Vector3()
  const surfaces = triangles.map(([a, b, c]) => new Triangle(a, b, c))
  for (const side of ['left', 'right']) {
    const upper = rig.humanoid.getNormalizedBoneNode((side + 'UpperArm') as 'head')!
    assert.ok(Math.abs(upper.rotation.x) <= 0.05, 'idle must not push the whole arm forward')
    for (const suffix of ['Hand', 'IndexProximal', 'IndexDistal', 'MiddleDistal', 'LittleDistal']) {
      const point = rig.humanoid.getNormalizedBoneNode((side + suffix) as 'head')!.getWorldPosition(new Vector3())
      const ray = new Ray(point.clone().add(new Vector3(0, 0, -0.008)), new Vector3(0, 0, 1))
      assert.ok(!triangles.some(([a, b, c]) => ray.intersectTriangle(a, b, c, false, hit)), `${side}${suffix} must remain outside the skirt`)
      assert.ok(surfaces.every(triangle => triangle.closestPointToPoint(point, hit).distanceTo(point) >= 0.008),
        `${side}${suffix} needs 8mm clearance from the actual skirt surface`)
    }
  }
})

test('explanation does not worsen the accepted idle skirt envelope and settles without a second recovery', () => {
  const motion = parseMotion(JSON.parse(readFileSync(new URL('../public/animations/explain.motion.json', import.meta.url), 'utf8')))
  const rig = createVrmRig()
  const dress = createDressProbe(rig)
  const player = new MotionAnimation(motion, rig.humanoid), runtime = new AvatarRuntime()
  runtime.registerMotion('explain', motion.duration); runtime.act('explain'); runtime.motionStarted = 0
  const points = ['left', 'right'].flatMap(side => ['Hand', 'IndexDistal', 'MiddleDistal', 'LittleDistal']
    .map(name => rig.humanoid.getNormalizedBoneNode((side + name) as 'head')!))
  const hip = rig.humanoid.getNormalizedBoneNode('hips')!
  const idle = points.map(node => node.getWorldPosition(new Vector3()))
  // The user's preferred idle overlaps the conservative horizontal bounds.
  // This is a baseline-relative regression gate, NOT proof of mesh clearance.
  const envelopeClearance = (point: Vector3, skirt: Vector3[]) => {
    const slice = skirt.filter(v => Math.abs(v.y - point.y) < 0.02)
    return slice.length ? Math.max(Math.abs(point.x) - Math.max(...slice.map(v => Math.abs(v.x))),
      point.z - Math.max(...slice.map(v => v.z))) : Infinity
  }
  const idleSkirt = dress()
  const baseline = idle.map(point => Math.min(0.005, envelopeClearance(point, idleSkirt)))
  const previous = idle.map(point => point.clone())
  const failures: string[] = []
  for (let i = 0; i <= 270; i++) {
    const time = i / 60
    runtime.finishMotion(time); player.restoreBase(); player.apply(runtime, time)
    const skirt = dress(), hipY = hip.getWorldPosition(new Vector3()).y
    points.forEach((node, j) => {
      const point = node.getWorldPosition(new Vector3())
      if (point.distanceTo(previous[j]) >= 0.022) failures.push(`${node.name} moves ${point.distanceTo(previous[j]).toFixed(3)}m/frame at ${time}`)
      if (point.y < hipY + 0.04) {
        const clearance = envelopeClearance(point, skirt)
        if (clearance < baseline[j] - 0.002) failures.push(`${node.name} worsens idle envelope by ${(baseline[j] - clearance).toFixed(3)}m at ${time}`)
      }
      if (time >= 2.7 && point.distanceTo(idle[j]) >= 0.005) failures.push(`${node.name} has a second late recovery at ${time}`)
      previous[j] = point
    })
  }
  assert.equal(failures.length, 0, failures.slice(0, 16).join('\n'))
})

for (const name of ['wave', 'explain']) test(`lowering ${name} does not cross actual skirt triangles with the hand and finger segments`, () => {
  const motion = parseMotion(JSON.parse(readFileSync(new URL(`../public/animations/${name}.motion.json`, import.meta.url), 'utf8')))
  const rig = createVrmRig(), dress = createDressProbe(rig)
  const player = new MotionAnimation([], rig.humanoid), runtime = new AvatarRuntime()
  player.registerMotion(motion, rig.humanoid, name === 'wave' ? 'right-arm' : 'explanation')
  runtime.registerMotion(name, motion.duration); runtime.act(name); runtime.motionStarted = 0
  const point = (name: string) => rig.humanoid.getNormalizedBoneNode(name as 'head')!.getWorldPosition(new Vector3())
  const ray = new Ray(), hit = new Vector3(), failures: string[] = []
  for (let i = 0; i <= 270; i++) {
    const time = i / 60
    runtime.finishMotion(time); player.restoreBase(); player.apply(runtime, time)
    const triangles = dress.triangles()
    for (const side of ['left', 'right']) for (const finger of ['Index', 'Middle', 'Little']) {
      const path = ['Hand', finger + 'Proximal', finger + 'Intermediate', finger + 'Distal'].map(name => point(side + name))
      for (let j = 1; j < path.length; j++) {
        const start = path[j - 1], end = path[j], length = start.distanceTo(end)
        ray.set(start, end.clone().sub(start).normalize())
        if (triangles.some(([a, b, c]) => ray.intersectTriangle(a, b, c, false, hit) && start.distanceTo(hit) < length)) {
          failures.push(`${side}${finger} segment ${j} intersects dress at ${time.toFixed(3)}`)
        }
      }
    }
  }
  assert.equal(failures.length, 0, failures.slice(0, 24).join('\n'))
})
