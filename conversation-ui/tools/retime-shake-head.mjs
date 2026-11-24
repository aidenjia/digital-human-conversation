import fs from 'node:fs'
import { QuaternionLinearInterpolant } from 'three'
import { prepareHyMotion } from './prepare-hy-motion.mjs'

const source = '../data/motion/4475eaf9-6348-4135-ad2f-906b1145e3f6.fbx'
const data = prepareHyMotion(source, 'shake_head', true, true)
const start = 0.35, end = 2.4, duration = 1.35
const times = Array.from({ length: 42 }, (_, i) => i * duration / 41)
for (const [bone, values] of Object.entries(data.rotations)) {
  const sample = new QuaternionLinearInterpolant(Float32Array.from(data.times), Float32Array.from(values), 4)
  data.rotations[bone] = times.flatMap(time => Array.from(sample.evaluate(start + time / duration * (end - start))))
}
data.times = times
data.duration = duration
data.adaptation = { operation: 'trim-and-retime', originalDuration: 2.9666666984558105, sourceStart: start, sourceEnd: end }
fs.writeFileSync('public/animations/shake_head.motion.json', JSON.stringify(data))
console.log(JSON.stringify({ duration, frames: times.length, sourceStart: start, sourceEnd: end }))
