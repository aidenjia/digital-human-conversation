import { Matrix4, Quaternion, QuaternionKeyframeTrack, Vector3, type AnimationClip, type Object3D } from 'three'
import type { VRMHumanoid, VRMHumanBoneName } from '@pixiv/three-vrm'

const clamp = (x: number) => Math.max(0, Math.min(1, x))
const ease = (x: number) => { const t = clamp(x); return t * t * (3 - 2 * t) }
const position = (node: Object3D) => node.getWorldPosition(new Vector3())

/** Bake a rig-proportioned two-bone correction once at load, not in the render loop.
 * Source elbow flexion drives the gesture envelope; source torso motion is retained.
 * Normalized VRM finger fan gives palm orientation (left/right handed winding).
 */
export function adaptExplanationClip(clip: AnimationClip, humanoid: VRMHumanoid) {
  const root = humanoid.normalizedHumanBonesRoot
  const bone = (name: string) => humanoid.getNormalizedBoneNode(name as VRMHumanBoneName)
  const chains = (['left', 'right'] as const).flatMap(side => {
    const upper = bone(side + 'UpperArm'), lower = bone(side + 'LowerArm'), hand = bone(side + 'Hand')
    const index = bone(side + 'IndexProximal'), little = bone(side + 'LittleProximal')
    return upper && lower && hand && index && little ? [{ side, upper, lower, hand, index, little }] : []
  })
  const hips = bone('hips'), left = bone('leftUpperArm'), right = bone('rightUpperArm')
  if (chains.length !== 2 || !hips || !left || !right) throw new Error('解释动作适配需要完整双臂及掌部骨骼')
  const nodes = new Map<string, Object3D>()
  const saved = new Map<Object3D, Quaternion>()
  root.traverse(node => { nodes.set(node.uuid, node); saved.set(node, node.quaternion.clone()) })
  root.updateWorldMatrix(true, true)
  const rests = chains.map(chain => position(chain.hand))
  const palmBases = chains.map(chain => {
    const wrist = position(chain.hand)
    const index = position(chain.index).sub(wrist), little = position(chain.little).sub(wrist)
    const inverseWorld = chain.hand.getWorldQuaternion(new Quaternion()).invert()
    const normal = index.clone().cross(little).normalize().multiplyScalar(chain.side === 'left' ? -1 : 1).applyQuaternion(inverseWorld)
    const fingers = index.add(little).normalize().applyQuaternion(inverseWorld)
    fingers.addScaledVector(normal, -fingers.dot(normal)).normalize()
    return new Quaternion().setFromRotationMatrix(new Matrix4().makeBasis(fingers, normal, fingers.clone().cross(normal))).invert()
  })
  const times = clip.tracks[0].times
  const corrected = new Map<string, number[]>()
  for (const chain of chains) for (const node of [chain.upper, chain.lower, chain.hand]) corrected.set(node.uuid, [])
  const worldQ = new Quaternion(), parentQ = new Quaternion()
  const sourceFrame = (frame: number) => {
    for (const [node, q] of saved) node.quaternion.copy(q)
    for (const track of clip.tracks) nodes.get(track.name.split('.')[0])!.quaternion.fromArray(track.values, frame * 4)
    root.updateWorldMatrix(true, true)
  }
  const turnToward = (node: Object3D, from: Vector3, to: Vector3) => {
    const correction = new Quaternion().setFromUnitVectors(from.normalize(), to.normalize())
    node.getWorldQuaternion(worldQ)
    node.parent!.getWorldQuaternion(parentQ)
    node.quaternion.copy(parentQ.invert().multiply(correction.multiply(worldQ)))
    root.updateWorldMatrix(true, true)
  }
  try {
    // Source flexion can rise from 12 to 59 degrees in 0.13 seconds. Filtering
    // its activation offline avoids amplifying that burst into a large IK jump.
    const weights = chains.map(() => [] as number[])
    for (let frame = 0; frame < times.length; frame++) {
      sourceFrame(frame)
      chains.forEach((chain, i) => {
        const shoulder = position(chain.upper), elbow = position(chain.lower), wrist = position(chain.hand)
        const flex = Math.acos(Math.max(-1, Math.min(1, elbow.sub(shoulder).normalize().dot(wrist.sub(position(chain.lower)).normalize()))))
        weights[i].push(clamp((flex - 0.25) / 0.65))
      })
    }
    const filtered = weights.map(values => Array.from(times, time => {
      let sum = 0, total = 0
      for (let i = 0; i < times.length; i++) {
        const distance = Math.abs(times[i] - time)
        if (distance > 0.36) continue
        const weight = Math.exp(-0.5 * (distance / 0.12) ** 2)
        sum += weight * values[i]; total += weight
      }
      return sum / total
    }))
    for (let frame = 0; frame < times.length; frame++) {
      sourceFrame(frame)
      const activation = Math.max(...filtered.map(values => values[frame]))
      // Settle source torso/shoulders with the gesture, not in a second late fade.
      for (const track of clip.tracks) {
        const node = nodes.get(track.name.split('.')[0])!
        node.quaternion.slerp(saved.get(node)!, 1 - activation)
      }
      root.updateWorldMatrix(true, true)
      const center = position(left).add(position(right)).multiplyScalar(0.5)
      const lateral = position(left).sub(position(right))
      const width = lateral.length(); lateral.normalize()
      const up = center.clone().sub(position(hips)).normalize()
      const forward = lateral.clone().cross(up).normalize()
      const torso = center.distanceTo(position(hips))
      chains.forEach((chain, chainIndex) => {
        const shoulder = position(chain.upper), elbow = position(chain.lower), wrist = position(chain.hand)
        const upperLength = shoulder.distanceTo(elbow), lowerLength = elbow.distanceTo(wrist)
        const weight = filtered[chainIndex][frame]
        const sign = chain.side === 'left' ? 1 : -1
        const target = center.clone().addScaledVector(lateral, sign * width * 0.95)
          .addScaledVector(up, -torso * 0.4).addScaledVector(forward, width * 1.05)
        target.lerpVectors(rests[chainIndex], target, weight)
        // Lift and lower along an outward arc. A straight wrist line cuts
        // through the flared dress as the fingers rotate back toward idle.
        target.addScaledVector(lateral, sign * torso * 0.2 * 4 * weight * (1 - weight))
        const along = target.clone().sub(shoulder)
        const distance = Math.max(1e-5, Math.min(along.length(), (upperLength + lowerLength) * 0.995))
        along.normalize(); target.copy(shoulder).addScaledVector(along, distance)
        // An outward/downward pole with a small forward bias keeps elbows beside
        // the torso rather than pulling them backwards while wrists rise.
        const pole = lateral.clone().multiplyScalar(sign * 0.7).addScaledVector(up, -1).addScaledVector(forward, 0.15)
        pole.addScaledVector(along, -pole.dot(along)).normalize()
        const a = (upperLength ** 2 - lowerLength ** 2 + distance ** 2) / (2 * distance)
        const height = Math.sqrt(Math.max(0, upperLength ** 2 - a ** 2))
        const desiredElbow = shoulder.clone().addScaledVector(along, a).addScaledVector(pole, height)
        // Solve from a stable rest basis rather than inheriting source arm twist.
        chain.upper.quaternion.copy(saved.get(chain.upper)!)
        chain.lower.quaternion.copy(saved.get(chain.lower)!)
        chain.hand.quaternion.copy(saved.get(chain.hand)!)
        root.updateWorldMatrix(true, true)
        turnToward(chain.upper, position(chain.lower).sub(position(chain.upper)), desiredElbow.clone().sub(shoulder))
        turnToward(chain.lower, position(chain.hand).sub(position(chain.lower)), target.clone().sub(position(chain.lower)))
        // Use the actual palm/finger basis, not abrupt source wrist twist. Both
        // palm normal and finger heading are constrained to remove roll ambiguity.
        const fingers = lateral.clone().multiplyScalar(sign * 0.75).addScaledVector(forward, 0.6).normalize()
        fingers.addScaledVector(up, -fingers.dot(up)).normalize()
        const alignedWorld = new Quaternion().setFromRotationMatrix(new Matrix4().makeBasis(fingers, up, fingers.clone().cross(up)))
          .multiply(palmBases[chainIndex])
        const palmWorld = chain.hand.getWorldQuaternion(new Quaternion())
        palmWorld.slerp(alignedWorld, weight)
        chain.hand.parent!.getWorldQuaternion(parentQ)
        chain.hand.quaternion.copy(parentQ.invert().multiply(palmWorld))
        root.updateWorldMatrix(true, true)
        for (const node of [chain.upper, chain.lower, chain.hand]) {
          node.quaternion.slerp(saved.get(node)!, 1 - ease(weight / 0.15))
          corrected.get(node.uuid)!.push(...node.quaternion.toArray())
        }
      })
    }
    clip.tracks = clip.tracks.map(track => {
      const values = corrected.get(track.name.split('.')[0])
      if (!values) {
        const node = nodes.get(track.name.split('.')[0])!
        const output = Array.from(times).flatMap((time, i) => saved.get(node)!.clone()
          .slerp(new Quaternion().fromArray(track.values, i * 4), Math.max(...filtered.map(v => v[i]))
            * ease(time / 0.3) * ease((clip.duration - time) / 0.45)).toArray())
        return new QuaternionKeyframeTrack(track.name, times, output)
      }
      // Smooth solved local rotations too: near arm extension small positional
      // changes can otherwise amplify into rapid joint-angle changes. Hemisphere
      // alignment avoids averaging q and -q to a degenerate quaternion.
      const output: number[] = []
      const center = new Quaternion(), sample = new Quaternion()
      for (let frame = 0; frame < times.length; frame++) {
        center.fromArray(values, frame * 4)
        const sum = new Quaternion(0, 0, 0, 0)
        // Neutral forearms increase the entry travel slightly. Smooth that
        // onset without smearing the held pose and its elbow-depth constraint.
        const sigma = 0.14 + 0.01 * (1 - ease((times[frame] - 0.6) / 0.3))
        for (let i = 0; i < times.length; i++) {
          const distance = Math.abs(times[i] - times[frame])
          if (distance > sigma * 3) continue
          sample.fromArray(values, i * 4)
          const weight = Math.exp(-0.5 * (distance / sigma) ** 2) * (center.dot(sample) < 0 ? -1 : 1)
          sum.x += sample.x * weight; sum.y += sample.y * weight
          sum.z += sample.z * weight; sum.w += sample.w * weight
        }
        const boundary = ease(times[frame] / 0.3) * ease((clip.duration - times[frame]) / 0.45)
        const node = nodes.get(track.name.split('.')[0])!
        output.push(...saved.get(node)!.clone().slerp(sum.normalize(), boundary).toArray())
      }
      return new QuaternionKeyframeTrack(track.name, times, output)
    })
    return clip
  } finally {
    for (const [node, q] of saved) node.quaternion.copy(q)
    root.updateWorldMatrix(true, true)
  }
}
