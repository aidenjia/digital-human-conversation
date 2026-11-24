import { readFileSync } from 'node:fs'
import { Object3D } from 'three'
import { VRMHumanoid, type VRMHumanBones } from '@pixiv/three-vrm'
import { DRESSED_IDLE_FORWARD_PITCH, DRESSED_IDLE_LATERAL_SLOPE, relaxedArmPose, applyRelaxedHands } from '../../src/avatar/armPose.ts'

/** Use the actual shipped skeleton without loading textures or requiring WebGL. */
export function createVrmRig() {
  const buffer = readFileSync(new URL('../../public/models/avatar-sample-2.vrm', import.meta.url))
  const gltf = JSON.parse(buffer.subarray(20, 20 + buffer.readUInt32LE(12)).toString())
  const nodes = gltf.nodes.map((node: any) => {
    const object = new Object3D()
    object.name = node.name
    if (node.translation) object.position.fromArray(node.translation)
    if (node.rotation) object.quaternion.fromArray(node.rotation)
    if (node.scale) object.scale.fromArray(node.scale)
    return object
  }) as Object3D[]
  gltf.nodes.forEach((node: any, index: number) => node.children?.forEach((child: number) => nodes[index].add(nodes[child])))
  const scene = new Object3D()
  gltf.scenes[0].nodes.forEach((index: number) => scene.add(nodes[index]))
  scene.updateMatrixWorld(true)
  const bones = Object.fromEntries(gltf.extensions.VRM.humanoid.humanBones.map((bone: any) => [bone.bone, { node: nodes[bone.node] }])) as VRMHumanBones
  const humanoid = new VRMHumanoid(bones)
  scene.add(humanoid.normalizedHumanBonesRoot)
  scene.rotation.y = Math.PI // Same VRM 0.x facing correction as the production loader.
  for (const side of ['left', 'right'] as const) {
    const pose = relaxedArmPose(humanoid.getNormalizedBoneNode(side === 'left' ? 'leftLowerArm' : 'rightLowerArm')!.position,
      DRESSED_IDLE_FORWARD_PITCH, DRESSED_IDLE_LATERAL_SLOPE)
    humanoid.getNormalizedBoneNode(side === 'left' ? 'leftUpperArm' : 'rightUpperArm')!.rotation.set(pose.x, 0, pose.z)
  }
  applyRelaxedHands(humanoid)
  return { scene, humanoid, nodes, gltf, buffer }
}
