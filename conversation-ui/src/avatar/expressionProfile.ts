import type { AvatarEmotion } from './avatarEvents.ts'

const profiles: Record<AvatarEmotion, { gain: number; attack: number; release: number }> = {
  neutral: { gain: 0, attack: 7, release: 7 },
  happy: { gain: 1.15, attack: 7, release: 5 },
  sad: { gain: 1.65, attack: 4, release: 3 },
  angry: { gain: 1.55, attack: 8, release: 5 },
  surprised: { gain: 1.55, attack: 10, release: 5 },
}

export function expressionTarget(emotion: AvatarEmotion, weight: number, mouth: number): number {
  const base = Math.max(0, Math.min(0.85, weight * profiles[emotion].gain))
  return base * (1 - Math.max(0, Math.min(1, mouth)) * 0.3)
}

export function expressionSpeed(emotion: AvatarEmotion, entering: boolean): number {
  return entering ? profiles[emotion].attack : profiles[emotion].release
}
