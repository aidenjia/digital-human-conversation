import type { DecisionCue, GestureStarted } from './avatarEvents.ts'
import { MOTION_CATALOG, type MotionAsset } from './motionCatalog.ts'

type PendingCue = { cue: DecisionCue; expires: number; expressed: boolean }
export type SkipReason = 'expired' | 'segment-passed' | 'unavailable' | 'cooldown'

/** Response-scoped cue queue. No network, loading or per-frame bone work here. */
export class MotionScheduler {
  private responseId = ''
  private queue: PendingCue[] = []
  private seen = new Set<string>()
  private segment = 'opening'
  private pastSegments = new Set<string>()
  private blocked = false
  private lastStart = -Infinity
  private lastByIntent = new Map<string, number>()
  private lastByIntentMotion = new Map<string, string>()
  readonly skipped: { cueId: string; reason: SkipReason }[] = []

  private catalog: readonly MotionAsset[]
  constructor(catalog: readonly MotionAsset[] = MOTION_CATALOG) { this.catalog = catalog }

  beginResponse(responseId: string) {
    this.clear()
    this.responseId = responseId
  }

  enqueue(cue: DecisionCue, now: number): boolean {
    if (this.blocked || cue.responseId !== this.responseId || this.seen.has(cue.cueId)
        || this.seen.size >= 256 || this.queue.length >= 32) return false
    this.seen.add(cue.cueId)
    if (this.pastSegments.has(cue.segmentId)) {
      this.skip(cue, 'segment-passed')
      return false
    }
    this.queue.push({ cue, expires: now + cue.expiresInMs / 1000, expressed: false })
    return true
  }

  markSpeechSegment(responseId: string, segmentId: string) {
    if (responseId !== this.responseId || !segmentId || this.pastSegments.has(segmentId)) return
    if (segmentId !== this.segment) {
      this.pastSegments.add(this.segment)
      this.segment = segmentId
    }
  }

  tick(now: number, speaking: boolean, idle: boolean, ready: ReadonlySet<string>) {
    const expressions: DecisionCue[] = []
    let started: GestureStarted | undefined
    this.queue = this.queue.filter(item => {
      if (now > item.expires) { this.skip(item.cue, 'expired'); return false }
      if (this.pastSegments.has(item.cue.segmentId)) { this.skip(item.cue, 'segment-passed'); return false }
      return true
    })
    if (!speaking || this.blocked) return { expressions, started }
    this.queue = this.queue.filter(item => {
      const cue = item.cue
      if (cue.segmentId !== this.segment) return true
      if (!item.expressed) { expressions.push(cue); item.expressed = true }
      if (cue.intent === 'none') return false
      const candidates = this.catalog.filter(asset => asset.intent === cue.intent && asset.automatic
        && asset.admission !== 'preview' && ready.has(asset.id))
        .sort((a, b) => a.priority - b.priority
          || Number(a.id === this.lastByIntentMotion.get(cue.intent)) - Number(b.id === this.lastByIntentMotion.get(cue.intent))
          || a.id.localeCompare(b.id))
      if (!candidates.length) { this.skip(cue, 'unavailable'); return false }
      if (now - this.lastStart < 8 || now - (this.lastByIntent.get(cue.intent) ?? -Infinity) < 30) {
        this.skip(cue, 'cooldown'); return false
      }
      if (!idle || started) return true
      const asset = candidates[0]
      started = { responseId: cue.responseId, cueId: cue.cueId, intent: cue.intent, motionId: asset.id }
      this.lastStart = now
      this.lastByIntent.set(cue.intent, now)
      this.lastByIntentMotion.set(cue.intent, asset.id)
      return false
    })
    return { expressions, started }
  }

  private skip(cue: DecisionCue, reason: SkipReason) {
    this.skipped.push({ cueId: cue.cueId, reason })
    if (this.skipped.length > 64) this.skipped.shift()
  }

  block() { this.queue = []; this.blocked = true }
  clear() {
    this.queue = []
    this.seen.clear()
    this.pastSegments.clear()
    this.segment = 'opening'
    this.responseId = ''
    this.blocked = false
    this.skipped.length = 0
  }
  reset() {
    this.clear()
    this.lastStart = -Infinity
    this.lastByIntent.clear()
    this.lastByIntentMotion.clear()
  }
}
