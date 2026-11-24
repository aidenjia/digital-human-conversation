import type { AvatarState } from './AvatarRuntime'

export class GazeController {
  private random: () => number
  private nextChange = 0
  private lastState: AvatarState = 'idle'
  private target = { x: 0, y: 0 }

  constructor(random: () => number = Math.random) { this.random = random }

  update(state: AvatarState, now: number) {
    if (state !== this.lastState || now >= this.nextChange) {
      const awayChance = { idle: 0.35, listening: 0.08, thinking: 0.7, speaking: 0.2 }[state]
      const away = this.random() < awayChance
      this.target = away
        ? { x: (this.random() < 0.5 ? -1 : 1) * (0.12 + this.random() * 0.12), y: state === 'thinking' ? 0.06 : -0.025 }
        : { x: (this.random() - 0.5) * 0.025, y: (this.random() - 0.5) * 0.015 }
      this.nextChange = now + (away ? 0.45 + this.random() * 0.6 : 1.8 + this.random() * 2.5)
      this.lastState = state
    }
    return this.target
  }
}
