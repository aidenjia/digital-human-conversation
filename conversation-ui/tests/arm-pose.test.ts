import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { Object3D, Vector3 } from 'three'
import { relaxedArmPose } from '../src/avatar/armPose.ts'
import { applyRelaxedHands } from '../src/avatar/armPose.ts'
import { createVrmRig } from './fixtures/vrm-rig.ts'

test('the supplied VRM 0.x sample lowers both elbows instead of raising them', () => {
  const buffer = readFileSync(new URL('../public/models/avatar-sample-2.vrm', import.meta.url))
  const gltf = JSON.parse(buffer.subarray(20, 20 + buffer.readUInt32LE(12)).toString())
  const nodes = gltf.nodes.map((node: { translation?: number[]; rotation?: number[]; scale?: number[] }) => {
    const object = new Object3D()
    if (node.translation) object.position.fromArray(node.translation)
    if (node.rotation) object.quaternion.fromArray(node.rotation)
    if (node.scale) object.scale.fromArray(node.scale)
    return object
  })
  gltf.nodes.forEach((node: { children?: number[] }, i: number) => node.children?.forEach(child => nodes[i].add(nodes[child])))
  const bones = Object.fromEntries(gltf.extensions.VRM.humanoid.humanBones.map((bone: { bone: string; node: number }) => [bone.bone, nodes[bone.node]]))
  for (const side of ['left', 'right']) {
    const upper = bones[side + 'UpperArm'] as Object3D
    const lower = bones[side + 'LowerArm'] as Object3D
    const rest = lower.getWorldPosition(new Vector3()).sub(upper.getWorldPosition(new Vector3()))
    const corrected = rest.clone().applyAxisAngle(new Vector3(0, 0, 1), relaxedArmPose(rest).z)
    assert.ok(corrected.y < -0.15, side + ' elbow must rest below the shoulder')
    assert.ok(Math.abs(corrected.x) < 0.05, side + ' arm should remain close to the torso')
  }
})

test('opposite rest-axis rigs also lower the arm, with mirrored wave directions', () => {
  for (const x of [-1, 1]) {
    const pose = relaxedArmPose({ x, y: 0 })
    const elbow = new Vector3(x, 0, 0).applyAxisAngle(new Vector3(0, 0, 1), pose.z)
    assert.ok(elbow.y < -0.9)
    const raised = new Vector3(x, 0, 0).applyAxisAngle(new Vector3(0, 0, 1), pose.z + pose.side * 2.1)
    assert.ok(raised.y > 0, 'wave should raise the arm')
  }
})

test('default resting fingers curl gently toward the palm without changing wrists or arm placement', () => {
  const { humanoid } = createVrmRig()
  for (const side of ['left', 'right']) for (const finger of ['Index', 'Middle', 'Ring', 'Little']) {
    const joint = humanoid.getNormalizedBoneNode((side + finger + 'Intermediate') as 'head')!
    assert.ok(joint.quaternion.angleTo(new Object3D().quaternion) > 0.1, side + finger + ' should relax')
    assert.ok(joint.quaternion.angleTo(new Object3D().quaternion) < 0.6, side + finger + ' should not form a fist')
  }
  const nodes = ['leftHand', 'rightHand', 'leftUpperArm', 'rightUpperArm', 'leftLowerArm', 'rightLowerArm']
    .map(name => humanoid.getNormalizedBoneNode(name as 'head')!)
  const poses = nodes.map(node => node.quaternion.clone())
  applyRelaxedHands(humanoid)
  nodes.forEach((node, index) => assert.ok(node.quaternion.angleTo(poses[index]) < 1e-6))
})

test('both hands relax inward toward their actual palm instead of bending backward', () => {
  const { humanoid, scene } = createVrmRig()
  const bone = (name: string) => humanoid.getNormalizedBoneNode(name as 'head')!
  for (const side of ['left', 'right']) {
    for (const finger of ['Index', 'Middle', 'Ring', 'Little']) for (const part of ['Proximal', 'Intermediate', 'Distal'])
      bone(side + finger + part).quaternion.identity()
  }
  scene.updateMatrixWorld(true)
  const measurements = ['left', 'right'].map(side => {
    const wrist = bone(side + 'Hand').getWorldPosition(new Vector3())
    const palm = bone(side + 'IndexProximal').getWorldPosition(new Vector3()).sub(wrist)
      .cross(bone(side + 'LittleProximal').getWorldPosition(new Vector3()).sub(wrist)).normalize().multiplyScalar(side === 'left' ? -1 : 1)
    return { side, palm, before: bone(side + 'MiddleDistal').getWorldPosition(new Vector3()) }
  })
  applyRelaxedHands(humanoid)
  for (const { side, palm, before } of measurements) {
    const after = bone(side + 'MiddleDistal').getWorldPosition(new Vector3())
    assert.ok(after.sub(before).dot(palm) > 0.005, side + ' fingers should close slightly toward palm')
  }
  const rotations = measurements.map(({ side }) => bone(side + 'MiddleIntermediate').quaternion.clone())
  applyRelaxedHands(humanoid)
  measurements.forEach(({ side }, i) => assert.ok(bone(side + 'MiddleIntermediate').quaternion.angleTo(rotations[i]) < 1e-6))
})
