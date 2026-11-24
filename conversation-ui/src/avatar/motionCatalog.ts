import type { VRMHumanBoneName } from '@pixiv/three-vrm'
import type { AvatarIntent } from './avatarEvents.ts'

export type RetargetStrategy = 'standard' | 'explanation' | 'right-arm'
export interface MotionAsset {
  id: string
  label: string
  intent: Exclude<AvatarIntent, 'none'> | null
  source: { kind: 'json'; path: string } | { kind: 'procedural' }
  admission: 'legacy' | 'preview' | 'approved'
  automatic: boolean
  loadPolicy: 'preload' | 'onDemand'
  requiredBones: readonly VRMHumanBoneName[]
  retargetStrategy: RetargetStrategy
  priority: number
}

export const MOTION_CATALOG: readonly MotionAsset[] = [
  { id: 'wave', label: '挥手', intent: 'greet', source: { kind: 'json', path: 'animations/wave.motion.json' },
    admission: 'approved', automatic: true, loadPolicy: 'preload', requiredBones: ['rightUpperArm', 'rightLowerArm', 'rightHand'], retargetStrategy: 'standard', priority: 0 },
  { id: 'nod', label: '点头', intent: 'agree', source: { kind: 'procedural' },
    admission: 'legacy', automatic: true, loadPolicy: 'preload', requiredBones: ['head'], retargetStrategy: 'standard', priority: 0 },
  { id: 'shake_head', label: '摇头', intent: 'deny', source: { kind: 'json', path: 'animations/shake_head.motion.json' },
    admission: 'approved', automatic: true, loadPolicy: 'preload', requiredBones: ['neck', 'head'], retargetStrategy: 'standard', priority: 0 },
  { id: 'explain', label: '解释', intent: 'explain', source: { kind: 'json', path: 'animations/explain.motion.json' },
    admission: 'approved', automatic: true, loadPolicy: 'preload',
    requiredBones: ['hips', 'leftUpperArm', 'leftLowerArm', 'leftHand', 'rightUpperArm', 'rightLowerArm', 'rightHand',
      'leftIndexProximal', 'leftLittleProximal', 'rightIndexProximal', 'rightLittleProximal'],
    retargetStrategy: 'explanation', priority: 0 },
  { id: 'dance', label: '摆动', intent: null, source: { kind: 'procedural' },
    admission: 'legacy', automatic: false, loadPolicy: 'preload', requiredBones: [], retargetStrategy: 'standard', priority: 0 },
  { id: 'heartbeat', label: '爱心', intent: null, source: { kind: 'procedural' },
    admission: 'legacy', automatic: false, loadPolicy: 'preload', requiredBones: [], retargetStrategy: 'standard', priority: 0 },
]

export const MOTION_ASSETS = MOTION_CATALOG.filter(asset => asset.source.kind === 'json')
export const motionAsset = (id: string) => MOTION_CATALOG.find(asset => asset.id === id)
