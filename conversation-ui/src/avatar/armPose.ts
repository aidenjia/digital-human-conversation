import { Quaternion, Vector3 } from 'three'
import type { VRMHumanoid, VRMHumanBoneName } from '@pixiv/three-vrm'

// Keep the supplied dress's hands beside the body with minimal forward reach.
// A small lateral clearance replaces the old pronounced forward inclination.
export const DRESSED_IDLE_FORWARD_PITCH = 0.04
export const DRESSED_IDLE_LATERAL_SLOPE = 0.31

/** Normalized VRM 0.x and 1.x rigs can point their rest arms in opposite X directions. */
export function relaxedArmPose(direction: { x: number; y: number }, forwardPitch = 0, lateralSlope = 0.18) {
  const side = direction.x >= 0 ? 1 : -1
  const restAngle = Math.atan2(direction.y, direction.x)
  const targetAngle = Math.atan2(-1, side * lateralSlope)
  const delta = targetAngle - restAngle
  return { x: forwardPitch, z: Math.atan2(Math.sin(delta), Math.cos(delta)), side }
}

/** Default model's static hand pose. Animation tracks still own wrists, not fingers. */
export function applyRelaxedHands(humanoid: VRMHumanoid) {
  const bone = (name: string) => humanoid.getNormalizedBoneNode(name as VRMHumanBoneName)
  const root = humanoid.normalizedHumanBonesRoot
  const fingers = [
    { name: 'Index', curl: [0.12, 0.20, 0.10] },
    { name: 'Middle', curl: [0.14, 0.22, 0.12] },
    { name: 'Ring', curl: [0.15, 0.24, 0.13] },
    { name: 'Little', curl: [0.16, 0.26, 0.14] },
  ]
  const parts = ['Proximal', 'Intermediate', 'Distal']
  for (const side of ['left', 'right']) {
    const hand = bone(side + 'Hand'), index = bone(side + 'IndexProximal'), little = bone(side + 'LittleProximal')
    if (!hand || !index || !little) continue
    // Reapplying the idle setup is idempotent, never cumulatively curls fingers.
    for (const finger of fingers) for (const part of parts) bone(side + finger.name + part)?.quaternion.identity()
    root.updateWorldMatrix(true, true)
    const wrist = hand.getWorldPosition(new Vector3())
    const palm = index.getWorldPosition(new Vector3()).sub(wrist)
      .cross(little.getWorldPosition(new Vector3()).sub(wrist)).normalize().multiplyScalar(side === 'left' ? -1 : 1)
    for (const finger of fingers) {
      for (let i = 0; i < parts.length; i++) {
        const node = bone(side + finger.name + parts[i])
        const next = bone(side + finger.name + parts[Math.min(i + 1, 2)])
        const previous = bone(side + finger.name + parts[Math.max(i - 1, 0)])
        if (!node || !next || !previous) continue
        root.updateWorldMatrix(true, true)
        const direction = i < 2 ? next.getWorldPosition(new Vector3()).sub(node.getWorldPosition(new Vector3()))
          : node.getWorldPosition(new Vector3()).sub(previous.getWorldPosition(new Vector3()))
        const inverseWorld = node.getWorldQuaternion(new Quaternion()).invert()
        const axis = direction.normalize().cross(palm).normalize().applyQuaternion(inverseWorld)
        if (axis.lengthSq() > 0.5) node.quaternion.multiply(new Quaternion().setFromAxisAngle(axis, finger.curl[i]))
      }
    }
  }
  root.updateWorldMatrix(true, true)
}
