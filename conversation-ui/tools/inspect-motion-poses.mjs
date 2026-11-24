import fs from 'node:fs'
import { AnimationMixer, Vector3, TextureLoader, Texture } from 'three'
import { FBXLoader } from 'three/addons/loaders/FBXLoader.js'
import { MotionAnimation, parseMotion } from '../src/avatar/WaveAnimation.ts'
import { AvatarRuntime } from '../src/avatar/AvatarRuntime.ts'
import { createVrmRig } from '../tests/fixtures/vrm-rig.ts'

const [sourcePath, assetPath] = process.argv.slice(2)
const bytes = fs.readFileSync(sourcePath)
const previousWindow = globalThis.window
const previousLoad = TextureLoader.prototype.load
globalThis.window = { URL: { createObjectURL: () => 'unused' } }
TextureLoader.prototype.load = () => new Texture()
let source
try { source = new FBXLoader().parse(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength), '') }
finally {
  TextureLoader.prototype.load = previousLoad
  if (previousWindow === undefined) delete globalThis.window
  else globalThis.window = previousWindow
}
const motion = parseMotion(JSON.parse(fs.readFileSync(assetPath, 'utf8')))
const mixer = new AnimationMixer(source)
mixer.clipAction(source.animations[0]).play()
const { scene, humanoid } = createVrmRig()
const player = new MotionAnimation(motion, humanoid)
const runtime = new AvatarRuntime()
runtime.registerMotion(motion.name, motion.duration)
runtime.act(motion.name); runtime.motionStarted = 0
const sourceNames = { leftUpperArm: 'L_Shoulder', rightUpperArm: 'R_Shoulder', leftLowerArm: 'L_Elbow', rightLowerArm: 'R_Elbow', leftHand: 'L_Wrist', rightHand: 'R_Wrist', head: 'Head', hips: 'Pelvis' }
function measure(get) {
  const left = get('leftUpperArm').getWorldPosition(new Vector3())
  const right = get('rightUpperArm').getWorldPosition(new Vector3())
  const center = left.clone().add(right).multiplyScalar(0.5)
  const lateral = left.clone().sub(right).normalize()
  const up = new Vector3(0, 1, 0)
  const front = lateral.clone().cross(up).normalize()
  const width = left.distanceTo(right)
  const hips = get('hips').getWorldPosition(new Vector3())
  const torso = center.y - hips.y
  const result = { torsoInShoulderWidths: +(torso / width).toFixed(3) }
  for (const name of ['leftLowerArm', 'rightLowerArm', 'leftHand', 'rightHand']) {
    const relative = get(name).getWorldPosition(new Vector3()).sub(center)
    result[name] = { lateral: +(relative.dot(lateral) / width).toFixed(3), height: +(relative.y / width).toFixed(3), forward: +(relative.dot(front) / width).toFixed(3), aboveHipsInTorsoLengths: +((relative.y + torso) / torso).toFixed(3) }
  }
  return result
}
console.log(JSON.stringify({ sourceBones: Object.fromEntries(Object.entries(sourceNames).map(([k,v]) => [k, !!source.getObjectByName(v)])) }))
for (const time of [0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5].filter(t => t < motion.duration)) {
  mixer.setTime(time); source.updateMatrixWorld(true)
  player.restoreBase(); player.apply(runtime, time); scene.updateMatrixWorld(true)
  console.log(JSON.stringify({ time, source: measure(name => source.getObjectByName(sourceNames[name])), target: measure(name => humanoid.getNormalizedBoneNode(name)) }))
}
