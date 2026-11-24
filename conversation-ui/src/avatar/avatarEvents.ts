export const AVATAR_EMOTIONS = ['neutral', 'happy', 'sad', 'angry', 'surprised'] as const
export type AvatarEmotion = (typeof AVATAR_EMOTIONS)[number]
export const AVATAR_INTENTS = ['none', 'greet', 'agree', 'deny', 'explain'] as const
export type AvatarIntent = (typeof AVATAR_INTENTS)[number]
export type DecisionCue = { type: 'avatar.decision'; responseId: string; cueId: string; segmentId: string; intent: AvatarIntent; emotion: AvatarEmotion; expiresInMs: number }
export type GestureStarted = { responseId: string; cueId: string; intent: Exclude<AvatarIntent, 'none'>; motionId: string }
export type AvatarEvent =
  | { type: 'avatar.response'; responseId: string; phase: 'created' | 'done' | 'cancelled'; decisionMode?: 'model' | 'local' }
  | DecisionCue
  | { type: 'avatar.expression'; responseId: string; name: AvatarEmotion; weight: number; durationMs: number }

export function parseAvatarEvent(value: unknown): AvatarEvent | null {
  if (!value || typeof value !== 'object') return null
  const event = value as Record<string, unknown>
  if (typeof event.responseId !== 'string' || !event.responseId || event.responseId.length > 256) return null
  if (event.type === 'avatar.response' && ['created', 'done', 'cancelled'].includes(String(event.phase))) {
    return { type: event.type, responseId: event.responseId, phase: event.phase as 'created' | 'done' | 'cancelled',
      ...(event.decisionMode === 'model' || event.decisionMode === 'local' ? { decisionMode: event.decisionMode } : {}) }
  }
  if (event.type === 'avatar.decision') {
    if (!AVATAR_INTENTS.includes(event.intent as AvatarIntent) || !AVATAR_EMOTIONS.includes(event.emotion as AvatarEmotion)) return null
    if (![event.cueId, event.segmentId].every(id => typeof id === 'string' && id.length > 0 && id.length <= 128)) return null
    const expires = event.expiresInMs ?? 1500
    if (typeof expires !== 'number' || !Number.isFinite(expires)) return null
    return { type: event.type, responseId: event.responseId, cueId: event.cueId as string, segmentId: event.segmentId as string, intent: event.intent as AvatarIntent,
      emotion: event.emotion as AvatarEmotion, expiresInMs: Math.max(100, Math.min(1500, expires)) }
  }
  if (event.type !== 'avatar.expression' || !AVATAR_EMOTIONS.includes(event.name as AvatarEmotion)) return null
  const weight = event.weight ?? 0.35
  const duration = event.durationMs ?? 2500
  if (typeof weight !== 'number' || !Number.isFinite(weight) || typeof duration !== 'number' || !Number.isFinite(duration)) return null
  return { type: event.type, responseId: event.responseId, name: event.name as AvatarEmotion,
    weight: Math.max(0, Math.min(0.7, weight)), durationMs: Math.max(300, Math.min(5000, duration)) }
}

/** Small, explicit first-person phrases; not a general sentiment classifier. */
export function expressionFromTranscript(text: string): AvatarEmotion | null {
  const rules: [AvatarEmotion, RegExp][] = [
    ['happy', /(?:^|[。！？!?\n，,])\s*(?:我)?(?:真为你高兴|替你开心|很高兴见到你|很高兴认识你|为你感到骄傲|[Ii]['’]m (?:so )?happy for you)/],
    ['surprised', /(?:^|[。！？!?\n，,])\s*(?:真没想到|太令人惊讶了|这太意外了|[Ii]['’]m surprised)/],
    ['sad', /(?:^|[。！？!?\n，,])\s*(?:我也有些难过|听到这个消息我很难过|[Ii]['’]m sad to hear)/],
  ]
  return rules.find(([, pattern]) => pattern.test(text))?.[0] ?? null
}
