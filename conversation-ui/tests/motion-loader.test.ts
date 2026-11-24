import assert from 'node:assert/strict'
import test from 'node:test'
import { MotionAssetLoader } from '../src/avatar/MotionAssetLoader.ts'
import { MOTION_CATALOG } from '../src/avatar/motionCatalog.ts'

test('preload loads only high frequency assets and merges concurrent requests', async () => {
  const requested: string[] = [], installed: string[] = []
  const loader = new MotionAssetLoader({
    fetchMotion: async asset => { requested.push(asset.id); return { name: asset.id } },
    install: (asset) => { installed.push(asset.id) },
    supports: () => true,
  })
  await Promise.all([loader.preload(), loader.load('wave')])
  assert.deepEqual(requested, ['wave', 'shake_head', 'explain'])
  assert.deepEqual(installed, ['wave', 'shake_head', 'explain'])
  assert.equal(MOTION_CATALOG.find(a => a.id === 'explain')!.automatic, true)
  await loader.load('explain')
  assert.deepEqual(installed, ['wave', 'shake_head', 'explain'])
  assert.equal(MOTION_CATALOG.find(a => a.id === 'shake_head')!.automatic, true)
})

test('explanation preload skips an incompatible rig without blocking the wave', async () => {
  const requested: string[] = []
  const loader = new MotionAssetLoader({
    fetchMotion: async asset => { requested.push(asset.id); return {} },
    install: () => {},
    supports: asset => asset.id !== 'explain',
  })
  assert.deepEqual(await loader.preload(), [true, true, false])
  assert.deepEqual(requested, ['wave', 'shake_head'])
})

test('failed downloads retry and disposed model never installs stale results', async () => {
  let attempts = 0, installs = 0
  let finish: (value: unknown) => void = () => {}
  const loader = new MotionAssetLoader({
    fetchMotion: async () => {
      if (++attempts === 1) throw new Error('network')
      return new Promise(resolve => { finish = resolve })
    },
    install: () => { installs++ },
    supports: () => true,
  })
  assert.equal(await loader.load('wave'), false)
  const pending = loader.load('wave')
  await Promise.resolve()
  loader.dispose()
  finish({})
  assert.equal(await pending, false)
  assert.equal(installs, 0)
})

test('unsupported skeletons and unknown actions never download assets', async () => {
  let downloads = 0
  const loader = new MotionAssetLoader({
    fetchMotion: async () => { downloads++; return {} },
    install: () => {},
    supports: () => false,
  })
  assert.equal(await loader.load('wave'), false)
  assert.equal(await loader.load('unknown'), false)
  assert.equal(downloads, 0)
})
