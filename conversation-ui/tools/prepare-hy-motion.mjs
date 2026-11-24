import fs from 'node:fs'
import path from 'node:path'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { FBXLoader } from 'three/addons/loaders/FBXLoader.js'
import { LoadingManager, Quaternion, Texture, TextureLoader } from 'three'

// This first gesture uses upper-body rotations only. Keep root translation, legs,
// head/eyes and template finger poses out of the live avatar's independent layers.
const mapping = {
  Spine1: 'spine', Spine2: 'chest', Spine3: 'upperChest',
  L_Collar: 'leftShoulder', R_Collar: 'rightShoulder',
  L_Shoulder: 'leftUpperArm', R_Shoulder: 'rightUpperArm',
  L_Elbow: 'leftLowerArm', R_Elbow: 'rightLowerArm',
  L_Wrist: 'leftHand', R_Wrist: 'rightHand',
}

export function prepareHyMotion(input, name = 'wave', includeHead = false, headOnly = false) {
  if (!/^[a-z][a-z0-9_]{0,47}$/.test(name)) throw new Error('Invalid motion name')
  const bytes = fs.readFileSync(input)
  const loader = new FBXLoader(new LoadingManager())
  // The source's embedded preview textures are not used by this animation-only export.
  const previousWindow = globalThis.window
  const previousLoad = TextureLoader.prototype.load
  const objectUrls = []
  globalThis.window = { URL: { createObjectURL: () => { const id = 'unused-texture-' + objectUrls.length; objectUrls.push(id); return id } } }
  TextureLoader.prototype.load = () => new Texture()
  let source
  try { source = loader.parse(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength), '') }
  finally {
    TextureLoader.prototype.load = previousLoad
    if (previousWindow === undefined) delete globalThis.window
    else globalThis.window = previousWindow
  }
  source.updateMatrixWorld(true)
  const clip = source.animations.find(animation => animation.duration > 0)
  if (!clip || clip.duration > 12.1) throw new Error('Expected a HY-Motion clip of at most 12 seconds')
  const quaternionTracks = new Map(clip.tracks.filter(track => track.ValueTypeName === 'quaternion').map(track => [track.name, track]))
  const frameCount = Math.round(clip.duration * 30) + 1
  const times = Array.from({ length: frameCount }, (_, index) => index * clip.duration / (frameCount - 1))
  const rotations = {}
  const channels = headOnly ? { Neck: 'neck', Head: 'head' }
    : includeHead ? { ...mapping, Neck: 'neck', Head: 'head' } : mapping
  for (const [sourceName, targetName] of Object.entries(channels)) {
    const bone = source.getObjectByName(sourceName)
    const track = quaternionTracks.get(sourceName + '.quaternion')
    if (!bone || !track) throw new Error('Missing HY-Motion bone or track: ' + sourceName)
    const inverseRest = bone.getWorldQuaternion(new Quaternion()).invert()
    const parentRest = bone.parent.getWorldQuaternion(new Quaternion())
    const interpolant = track.createInterpolant()
    const values = []
    for (const time of times) {
      const q = new Quaternion().fromArray(interpolant.evaluate(time))
      q.premultiply(parentRest).multiply(inverseRest).normalize()
      values.push(...q.toArray().map(value => Number(value.toFixed(7))))
    }
    rotations[targetName] = values
  }
  source.traverse(object => {
    object.geometry?.dispose()
    const materials = Array.isArray(object.material) ? object.material : [object.material]
    for (const material of materials) {
      if (!material) continue
      for (const value of Object.values(material)) if (value?.isTexture) value.dispose()
      material.dispose()
    }
  })
  return {
    version: 1, name, duration: clip.duration,
    source: { format: 'TokenHub HY-3D-Motion FBX', file: path.basename(input), sha256: createHash('sha256').update(bytes).digest('hex') },
    times: times.map(value => Number(value.toFixed(7))), rotations,
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const [input, output, name = 'wave', headOption] = process.argv.slice(2)
  if (!input || !output || (headOption && !['--include-head', '--head-only'].includes(headOption))) throw new Error('Usage: node tools/prepare-hy-motion.mjs input.fbx output.motion.json [name] [--include-head|--head-only]')
  const motion = prepareHyMotion(input, name, headOption === '--include-head', headOption === '--head-only')
  fs.mkdirSync(path.dirname(output), { recursive: true })
  fs.writeFileSync(output, JSON.stringify(motion))
  console.log(JSON.stringify({ output, duration: motion.duration, frames: motion.times.length, tracks: Object.keys(motion.rotations).length, bytes: fs.statSync(output).size }))
}
