import { expressionFromTranscript, type AvatarEmotion, type AvatarEvent } from './avatarEvents.ts'
import { MotionScheduler } from './MotionScheduler.ts'
import { MOTION_CATALOG } from './motionCatalog.ts'
import type { GestureStarted } from './avatarEvents.ts'
export type { AvatarEmotion } from './avatarEvents.ts'
export type AvatarMotion = string
export type AvatarState = 'idle' | 'listening' | 'thinking' | 'speaking'

/** Mutable frame data stays outside React's render cycle. */
export class AvatarRuntime {
  private onGestureStarted: ((event: GestureStarted) => void) | null = null
  readonly scheduler = new MotionScheduler()
  private readyMotions = new Set(MOTION_CATALOG.filter(asset => asset.source.kind === 'procedural').map(asset => asset.id))
  private motionLoader: ((id: string) => Promise<boolean>) | null = null
  private previewSequence = 0
  readLevel: () => number = () => 0
  listening = false
  motion: AvatarMotion = 'idle'
  emotion: AvatarEmotion = 'neutral'
  motionStarted = 0
  motionSequence = 0
  private waveDuration = 2.5
  private importedDurations = new Map<AvatarMotion, number>()
  emotionUntil = 0
  zoom = 1
  scene = 0
  state: AvatarState = 'idle'
  emotionWeight = 0.4
  private responseId: string | null = null
  private cancelled = new Set<string>()
  private lastAudio = -Infinity
  private thinkingUntil = 0
  private transcript = ''
  private inferred = false
  private modelDecisions = false
  private autoMotionResponseId: string | null = null
  private emotionResponseId: string | null = null
  private pending: { name: AvatarEmotion; weight: number; duration: number; expires: number } | null = null
  private explicitExpression: { name: AvatarEmotion; sourceId: string; confirmationId: string | null; expires: number } | null = null

  setAudioReader(read: () => number) { this.readLevel = read }
  setGestureStartedHandler(handler: (event: GestureStarted) => void) {
    this.onGestureStarted = handler
  }
  stopListening() {
    this.listening = false
    this.thinkingUntil = performance.now() / 1000 + 8
    this.state = 'thinking'
  }

  receive(event: AvatarEvent, now = performance.now() / 1000): boolean {
    if (this.cancelled.has(event.responseId)) return false
    if (event.type === 'avatar.response' && event.phase === 'created') {
      if (this.listening) { this.rememberCancelled(event.responseId); return false }
      if (event.responseId === this.responseId) return true
      if (this.explicitExpression && event.responseId !== this.explicitExpression.sourceId) {
        this.explicitExpression.confirmationId = event.responseId
      }
      if (this.responseId) this.rememberCancelled(this.responseId)
      if (this.autoMotionResponseId) this.motion = 'idle'
      this.autoMotionResponseId = null
      this.scheduler.beginResponse(event.responseId)
      this.modelDecisions = event.decisionMode === 'model'
      this.lastAudio = -Infinity
      this.responseId = event.responseId
      this.transcript = ''
      this.inferred = false
      this.pending = null
      if (this.emotionResponseId) {
        this.emotion = 'neutral'
        this.emotionUntil = 0
        this.emotionResponseId = null
      }
      this.thinkingUntil = now + 8
      return true
    }
    if (event.responseId !== this.responseId || this.listening) return false
    if (event.type === 'avatar.decision') {
      if (!this.scheduler.enqueue(event, now)) return false
      this.inferred = true
      this.pending = null
    } else if (event.type === 'avatar.expression') {
      this.pending = { name: event.name, weight: event.weight, duration: event.durationMs, expires: now + 5 }
    } else {
      this.thinkingUntil = 0
      if (event.phase === 'cancelled') {
        this.rememberCancelled(event.responseId)
        this.pending = null
        this.scheduler.clear()
        if (this.autoMotionResponseId === event.responseId) {
          this.motion = 'idle'
          this.autoMotionResponseId = null
        }
        if (this.emotionResponseId === event.responseId) {
          this.emotion = 'neutral'
          this.emotionResponseId = null
        }
        // Tool actions cancel their original spoken response too. Only user barge-in
        // (interrupt) should stop an already dispatched body gesture.
      }
    }
    return true
  }

  observeTranscript(delta: string, responseId?: string) {
    if (!responseId || responseId !== this.responseId || this.listening || this.cancelled.has(responseId)) return
    if (this.inferred || this.modelDecisions) return
    this.transcript = (this.transcript + delta).slice(-600)
    const emotion = expressionFromTranscript(this.transcript)
    if (emotion) {
      this.inferred = true
      this.receive({ type: 'avatar.expression', responseId, name: emotion, weight: 0.35, durationMs: 2500 })
    }
  }

  update(now: number, level: number) {
    if (!this.listening && level > 0.012) this.lastAudio = now
    this.state = this.listening ? 'listening'
      : now - this.lastAudio < 0.35 ? 'speaking'
      : now < this.thinkingUntil ? 'thinking' : 'idle'
    const scheduled = this.scheduler.tick(now, this.state === 'speaking', this.motion === 'idle', this.readyMotions)
    if (this.explicitExpression && now > this.explicitExpression.expires) this.explicitExpression = null
    if (this.explicitExpression?.confirmationId === this.responseId && this.state === 'speaking') {
      this.express(this.explicitExpression.name, 0.4, 3000, now, this.responseId)
      this.explicitExpression = null
    }
    for (const cue of scheduled.expressions) this.express(cue.emotion, 0.35, 2500, now, cue.responseId)
    if (scheduled.started) {
      this.motion = scheduled.started.motionId
      this.motionStarted = now
      this.motionSequence++
      this.autoMotionResponseId = this.responseId
      this.onGestureStarted?.(scheduled.started)
    }
    if (this.pending) {
      if (now > this.pending.expires) this.pending = null
      else if (this.state === 'speaking') {
        this.express(this.pending.name, this.pending.weight, this.pending.duration, now, this.responseId)
        this.pending = null
      }
    }
    if (now > this.emotionUntil) {
      this.emotion = 'neutral'
      this.emotionResponseId = null
    }
  }

  private rememberCancelled(id: string) {
    this.cancelled.add(id)
    if (this.cancelled.size > 64) this.cancelled.delete(this.cancelled.values().next().value!)
  }

  markSpeechSegment(responseId: string, segmentId: string) { this.scheduler.markSpeechSegment(responseId, segmentId) }

  setMotionLoader(loader: ((id: string) => Promise<boolean>) | null) {
    this.previewSequence++
    this.motionLoader = loader
  }

  setProceduralReady(ids: string[]) {
    for (const asset of MOTION_CATALOG) if (asset.source.kind === 'procedural') this.readyMotions.delete(asset.id)
    for (const id of ids) this.readyMotions.add(id)
  }

  async previewMotion(id: string): Promise<boolean> {
    if (!MOTION_CATALOG.some(asset => asset.id === id)) return false
    this.scheduler.block()
    const sequence = ++this.previewSequence
    if (!this.readyMotions.has(id)) {
      if (!this.motionLoader || !await this.motionLoader(id)) return false
    }
    return sequence === this.previewSequence && this.act(id)
  }

  setWaveDuration(duration: number) {
    this.waveDuration = Number.isFinite(duration) && duration > 0 && duration <= 12.1 ? duration : 2.5
  }

  registerMotion(name: AvatarMotion, duration: number) {
    if (name === 'idle' || !Number.isFinite(duration) || duration <= 0 || duration > 12.1)
      throw new Error('Invalid motion registration')
    if (!/^[a-z][a-z0-9_]{0,47}$/.test(name)) throw new Error('Invalid motion name')
    this.importedDurations.set(name, duration)
    this.readyMotions.add(name)
  }

  clearImportedMotions() {
    for (const id of this.importedDurations.keys()) this.readyMotions.delete(id)
    this.importedDurations.clear()
    this.scheduler.clear()
    this.stopMotion()
  }

  hasPreparedMotion(name: AvatarMotion) { return this.importedDurations.has(name) }

  getMotionDuration() { return this.importedDurations.get(this.motion) ?? (this.motion === 'wave' ? this.waveDuration : this.motion === 'dance' ? 6 : 2.5) }
  stopMotion() { this.previewSequence++; this.scheduler.block(); this.motion = 'idle' }

  finishMotion(now: number) {
    if (now - this.motionStarted > this.getMotionDuration()) this.motion = 'idle'
  }

  interrupt() {
    this.previewSequence++
    if (this.responseId) this.rememberCancelled(this.responseId)
    this.scheduler.clear()
    this.autoMotionResponseId = null
    this.listening = true
    this.state = 'listening'
    this.motion = 'idle'
    this.emotion = 'neutral'
    this.emotionUntil = 0
    this.emotionResponseId = null
    this.pending = null
    this.explicitExpression = null
    this.lastAudio = -Infinity
    this.thinkingUntil = 0
  }

  reset() {
    this.previewSequence++
    this.onGestureStarted = null
    this.readLevel = () => 0
    this.listening = false
    this.motion = 'idle'
    this.emotion = 'neutral'
    this.emotionUntil = 0
    this.emotionResponseId = null
    this.motionStarted = 0
    this.zoom = 1
    this.scene = 0
    this.state = 'idle'
    this.responseId = null
    this.cancelled.clear()
    this.autoMotionResponseId = null
    this.modelDecisions = false
    this.scheduler.reset()
    this.pending = null
    this.explicitExpression = null
    this.transcript = ''
    this.inferred = false
    this.lastAudio = -Infinity
    this.thinkingUntil = 0
  }

  act(command: string): boolean {
    const now = performance.now() / 1000
    const expressions: Partial<Record<string, AvatarEmotion>> = {
      expression_happy: 'happy', expression_sad: 'sad', expression_angry: 'angry', expression_surprised: 'surprised',
    }
    const expression = expressions[command]
    if (expression) {
      this.scheduler.block()
      this.pending = null
      this.explicitExpression = this.responseId
        ? { name: expression, sourceId: this.responseId, confirmationId: null, expires: now + 10 }
        : null
      this.express(expression, 0.4, 3000, now)
      return true
    }
    if (['wave', 'nod', 'dance', 'heartbeat'].includes(command) || this.importedDurations.has(command as AvatarMotion)) {
      this.scheduler.clear()
      this.autoMotionResponseId = null
      this.scheduler.block()
      this.previewSequence++
      this.motion = command as AvatarMotion
      this.motionStarted = now
      this.motionSequence++
      if (command === 'heartbeat') this.express('happy')
      return true
    }
    if (command === 'zoom_in') this.zoom = Math.max(0.75, this.zoom - 0.1)
    else if (command === 'zoom_out') this.zoom = Math.min(1.4, this.zoom + 0.1)
    else if (command === 'change_scene') this.scene = (this.scene + 1) % 3
    else if (command === 'reset_default') {
      this.stopMotion()
      this.emotion = 'neutral'
      this.zoom = 1
      this.scene = 0
      this.pending = null
      this.emotionUntil = 0
      this.emotionResponseId = null
    } else return false
    return true
  }

  express(emotion: AvatarEmotion, weight = 0.4, durationMs = 3000, now = performance.now() / 1000, responseId: string | null = null) {
    this.emotion = emotion
    this.emotionWeight = weight
    this.emotionUntil = now + durationMs / 1000
    this.emotionResponseId = responseId
  }
}

export function mouthTarget(rms: number): number {
  return Math.min(1, Math.max(0, (rms - 0.008) * 9))
}

export function smooth(current: number, target: number, speed: number, dt: number) {
  return current + (target - current) * (1 - Math.exp(-speed * Math.min(dt, 0.1)))
}
