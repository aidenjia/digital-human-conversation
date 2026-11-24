import { AnimationClip, Quaternion, QuaternionKeyframeTrack, QuaternionLinearInterpolant, type Object3D } from 'three'
import type { VRMHumanoid, VRMHumanBoneName } from '@pixiv/three-vrm'
import type { AvatarRuntime } from './AvatarRuntime.ts'
import { adaptExplanationClip } from './explanationRetarget.ts'
import { motionAsset, type RetargetStrategy } from './motionCatalog.ts'

export const WAVE_BONES = ['spine', 'chest', 'upperChest', 'leftShoulder', 'rightShoulder',
  'leftUpperArm', 'rightUpperArm', 'leftLowerArm', 'rightLowerArm', 'leftHand', 'rightHand'] as const
export const MOTION_BONES = [...WAVE_BONES, 'neck', 'head'] as const
type MotionBone = typeof MOTION_BONES[number]
export interface MotionData {
  version: 1
  name: string
  duration: number
  times: number[]
  rotations: Partial<Record<MotionBone, number[]>>
}
export interface WaveMotion {
  version: 1
  name: 'wave'
  duration: number
  times: number[]
  rotations: Record<typeof WAVE_BONES[number], number[]>
}

export function parseMotion(value: unknown): MotionData {
  const data = value as Partial<MotionData> | null
  const finite = (array: unknown): array is number[] => Array.isArray(array) && array.every(item => typeof item === 'number' && Number.isFinite(item))
  if (!data || data.version !== 1 || typeof data.name !== 'string' || !/^[a-z][a-z0-9_]{0,47}$/.test(data.name) || typeof data.duration !== 'number'
      || !Number.isFinite(data.duration) || data.duration <= 0 || data.duration > 12.1
      || !finite(data.times) || data.times.length < 2 || data.times.length > 400 || data.times[0] !== 0
      || Math.abs(data.times.at(-1)! - data.duration) > 0.001
      || data.times.some((time, index) => index > 0 && time <= data.times![index - 1])
      || !data.rotations || typeof data.rotations !== 'object' || Array.isArray(data.rotations)
      || Object.keys(data.rotations).length === 0) throw new Error('动作动画数据格式不支持')
  for (const [bone, values] of Object.entries(data.rotations)) {
    if (!(MOTION_BONES as readonly string[]).includes(bone)) throw new Error('动作包含不支持的骨骼轨道')
    if (!finite(values) || values.length !== data.times.length * 4) throw new Error('挥手动画缺少骨骼轨道')
    for (let index = 0; index < values.length; index += 4) {
      const norm = Math.hypot(...values.slice(index, index + 4))
      if (Math.abs(norm - 1) > 0.01) throw new Error('挥手动画包含无效旋转')
    }
  }
  return data as MotionData
}

export function parseWaveMotion(value: unknown): WaveMotion {
  const data = parseMotion(value)
  if (data.name !== 'wave' || WAVE_BONES.some(bone => !data.rotations[bone])
      || Object.keys(data.rotations).length !== WAVE_BONES.length) throw new Error('挥手动画缺少骨骼轨道')
  return data as WaveMotion
}

/** HY-Motion's canonical left arm points +X; normalized VRM 0.x often points -X. */
export function createMotionClip(data: MotionData, humanoid: VRMHumanoid, strategy: RetargetStrategy = motionAsset(data.name)?.retargetStrategy ?? 'standard'): AnimationClip {
  const groups = new Map<VRMHumanBoneName, MotionBone[]>()
  for (const source of MOTION_BONES) {
    if (!data.rotations[source]) continue
    // The supplied dress uses a side-resting idle. Keep the non-waving arm and
    // torso on that base instead of blending through the source's forward rest.
    if (strategy === 'right-arm' && !source.startsWith('right')) continue
    let target: VRMHumanBoneName = source
    if (!humanoid.getNormalizedBoneNode(target)) {
      if (source === 'upperChest') target = humanoid.getNormalizedBoneNode('chest') ? 'chest' : 'spine'
      else if (source === 'chest') target = 'spine'
      else if (source === 'leftShoulder') target = 'leftUpperArm'
      else if (source === 'rightShoulder') target = 'rightUpperArm'
    }
    if (!humanoid.getNormalizedBoneNode(target)) continue
    if (!groups.has(target)) groups.set(target, [])
    groups.get(target)!.push(source)
  }
  const flip = (humanoid.getNormalizedBoneNode('leftLowerArm')?.position.x ?? 1) < 0
  const tracks: QuaternionKeyframeTrack[] = []
  const combined = new Quaternion()
  const part = new Quaternion()
  for (const [boneName, sources] of groups) {
    const node = humanoid.getNormalizedBoneNode(boneName)!
    const values: number[] = []
    for (let frame = 0; frame < data.times.length; frame++) {
      combined.identity()
      for (const source of sources) combined.multiply(part.fromArray(data.rotations[source]!, frame * 4))
      combined.normalize()
      if (flip) combined.set(-combined.x, combined.y, -combined.z, combined.w)
      values.push(...combined.toArray())
    }
    tracks.push(new QuaternionKeyframeTrack(node.uuid + '.quaternion', data.times, values))
  }
  const required = motionAsset(data.name)?.requiredBones ?? []
  if (required.some(bone => !humanoid.getNormalizedBoneNode(bone))) throw new Error('当前角色缺少动作所需骨骼')
  if (!tracks.length) throw new Error('当前角色缺少动作所需骨骼')
  const clip = new AnimationClip('hy-motion-' + data.name, data.duration, tracks)
  return strategy === 'explanation' ? adaptExplanationClip(clip, humanoid) : clip
}

export const createWaveClip = (data: WaveMotion, humanoid: VRMHumanoid) => createMotionClip(data, humanoid)

const ease = (value: number) => { const x = Math.max(0, Math.min(1, value)); return x * x * (3 - 2 * x) }

/** Owns only upper-body rotation tracks. Procedural idle remains a separate base pose. */
export class MotionAnimation {
  readonly duration: number
  private clips = new Map<string, { duration: number; samplers: Map<string, QuaternionLinearInterpolant> }>()
  private entries: { node: Object3D; owned: boolean; rest: Quaternion; base: Quaternion; output: Quaternion; from: Quaternion }[]
  private sampled = new Quaternion()
  private playing = false
  private sequence = -1
  private activeName: string | null = null
  private stoppingAt = -Infinity

  constructor(data: MotionData | MotionData[], humanoid: VRMHumanoid) {
    const motions = Array.isArray(data) ? data : [data]
    // Capture every supported node while the rig is idle, before incremental loads.
    const nodes = MOTION_BONES.flatMap(bone => {
      const node = humanoid.getNormalizedBoneNode(bone)
      return node ? [node] : []
    })
    this.entries = nodes.map(node => ({ node, owned: false, rest: node.quaternion.clone(),
      base: node.quaternion.clone(), output: node.quaternion.clone(), from: node.quaternion.clone() }))
    this.duration = motions[0]?.duration ?? 0
    for (const motion of motions) this.registerMotion(motion, humanoid)
  }

  registerMotion(motion: MotionData, humanoid: VRMHumanoid, strategy: RetargetStrategy = motionAsset(motion.name)?.retargetStrategy ?? 'standard') {
    if (this.clips.has(motion.name)) throw new Error('动作名称重复')
    const displayed = this.entries.map(entry => entry.node.quaternion.clone())
    try {
      for (const entry of this.entries) entry.node.quaternion.copy(entry.rest)
      humanoid.normalizedHumanBonesRoot.updateMatrixWorld(true)
      const clip = createMotionClip(parseMotion(motion), humanoid, strategy)
      const samplers = new Map<string, QuaternionLinearInterpolant>()
      for (const track of clip.tracks) samplers.set(track.name.split('.')[0],
        new QuaternionLinearInterpolant(track.times, track.values, 4))
      this.clips.set(motion.name, { duration: motion.duration, samplers })
      this.entries.forEach((entry, index) => {
        if (!entry.owned && samplers.has(entry.node.uuid)) {
          entry.base.copy(displayed[index])
          entry.output.copy(displayed[index])
          entry.from.copy(displayed[index])
          entry.owned = true
        }
      })
    } finally {
      this.entries.forEach((entry, index) => entry.node.quaternion.copy(displayed[index]))
      humanoid.normalizedHumanBonesRoot.updateMatrixWorld(true)
    }
  }

  getDuration(name: string) { return this.clips.get(name)?.duration }

  // Call before procedural posing so last frame's clip cannot feed back into the idle layer.
  restoreBase() { for (const entry of this.entries) if (entry.owned) entry.node.quaternion.copy(entry.base) }

  apply(runtime: AvatarRuntime, now: number) {
    for (const entry of this.entries) if (entry.owned) entry.base.copy(entry.node.quaternion)
    const clip = this.clips.get(runtime.motion)
    if (clip) {
      if (!this.playing || this.sequence !== runtime.motionSequence || this.activeName !== runtime.motion) {
        for (const entry of this.entries) if (entry.owned) entry.from.copy(this.playing || Number.isFinite(this.stoppingAt) ? entry.output : entry.base)
        this.sequence = runtime.motionSequence
        this.activeName = runtime.motion
      }
      const elapsed = Math.max(0, now - runtime.motionStarted)
      const fadeIn = ease(elapsed / 0.22)
      const fadeOut = ease((elapsed - Math.max(0.22, clip.duration - 0.35)) / 0.35)
      for (const entry of this.entries) {
        if (!entry.owned) continue
        const sampler = clip.samplers.get(entry.node.uuid)
        if (sampler) this.sampled.fromArray(sampler.evaluate(Math.min(elapsed, clip.duration))).normalize()
        else this.sampled.copy(entry.base)
        entry.node.quaternion.copy(entry.from).slerp(this.sampled, fadeIn).slerp(entry.base, fadeOut)
      }
      this.playing = true
      this.stoppingAt = -Infinity
    } else {
      if (this.playing) {
        this.stoppingAt = now
        for (const entry of this.entries) if (entry.owned) entry.from.copy(entry.output)
      }
      this.playing = false
      if (Number.isFinite(this.stoppingAt)) {
        const blend = ease((now - this.stoppingAt) / 0.25)
        for (const entry of this.entries) if (entry.owned) entry.node.quaternion.copy(entry.from).slerp(entry.base, blend)
        if (blend === 1) this.stoppingAt = -Infinity
      }
    }
    for (const entry of this.entries) if (entry.owned) entry.output.copy(entry.node.quaternion)
  }

  dispose() { this.restoreBase() }
}

/** Compatibility for the shipped single-wave integration. */
export class WaveAnimation extends MotionAnimation {}
