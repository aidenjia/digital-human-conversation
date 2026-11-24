/** Silent analysis branch: never connects another audible output. */
export class AudioLevel {
  private analyser: AnalyserNode
  private samples = new Float32Array(512)
  private context: AudioContext

  constructor(context: AudioContext) {
    this.context = context
    this.analyser = context.createAnalyser()
    this.analyser.fftSize = this.samples.length
    this.analyser.smoothingTimeConstant = 0
  }

  connect(source: AudioNode) {
    source.connect(this.analyser)
  }

  read = (): number => {
    if (this.context.state !== 'running') return 0
    this.analyser.getFloatTimeDomainData(this.samples)
    let sum = 0
    for (const value of this.samples) sum += value * value
    return Math.sqrt(sum / this.samples.length)
  }

  disconnect() {
    this.analyser.disconnect()
  }
}
