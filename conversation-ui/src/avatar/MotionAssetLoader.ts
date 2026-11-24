import { MOTION_CATALOG, type MotionAsset } from './motionCatalog.ts'

interface LoaderOptions {
  fetchMotion: (asset: MotionAsset) => Promise<unknown>
  install: (asset: MotionAsset, value: unknown) => void
  supports: (asset: MotionAsset) => boolean
}

/** Per-model lifetime; raw downloads never install into a replaced rig. */
export class MotionAssetLoader {
  private options: LoaderOptions
  private catalog: readonly MotionAsset[]
  private pending = new Map<string, Promise<boolean>>()
  private loaded = new Set<string>()
  private disposed = false

  constructor(options: LoaderOptions, catalog: readonly MotionAsset[] = MOTION_CATALOG) {
    this.options = options
    this.catalog = catalog
  }

  load(id: string): Promise<boolean> {
    const asset = this.catalog.find(item => item.id === id)
    if (this.disposed || !asset || !this.options.supports(asset)) return Promise.resolve(false)
    if (this.loaded.has(id)) return Promise.resolve(true)
    const existing = this.pending.get(id)
    if (existing) return existing
    const task = (async () => {
      try {
        if (asset.source.kind === 'procedural') return true
        const value = await this.options.fetchMotion(asset)
        if (this.disposed) return false
        this.options.install(asset, value)
        this.loaded.add(id)
        return true
      } catch {
        return false
      } finally {
        this.pending.delete(id)
      }
    })()
    this.pending.set(id, task)
    return task
  }

  preload() {
    return Promise.all(this.catalog.filter(asset => asset.source.kind === 'json' && asset.loadPolicy === 'preload')
      .map(asset => this.load(asset.id)))
  }

  dispose() { this.disposed = true; this.loaded.clear(); this.pending.clear() }
}
