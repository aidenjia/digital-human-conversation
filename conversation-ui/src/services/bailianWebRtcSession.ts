import logger from '../utils/logger.ts'
import { AudioLevel } from '../avatar/AudioLevel.ts'
import { type InputMode } from './realtimeText.ts'

const ICE_GATHER_TIMEOUT_MS = 5000

function waitForIceGathering(pc: RTCPeerConnection, timeoutMs: number): Promise<void> {
  if (pc.iceGatheringState === 'complete') return Promise.resolve()
  return new Promise((resolve) => {
    const done = () => {
      window.clearTimeout(timer)
      pc.removeEventListener('icegatheringstatechange', onChange)
      resolve()
    }
    const onChange = () => {
      if (pc.iceGatheringState === 'complete') done()
    }
    const timer = window.setTimeout(() => {
      logger.warn('[WebRTC] ICE gather timed out', { state: pc.iceGatheringState })
      done()
    }, timeoutMs)
    pc.addEventListener('icegatheringstatechange', onChange)
  })
}

/** 百炼把会话事件推到服务端创建的 DataChannel `txt`，应答也要写回这条通道。 */
const SERVER_EVENT_CHANNEL = 'txt'

function normalizeAnswerSdp(sdp: string): string {
  let normalized = sdp.trim().replace(/\r?\n/g, '\r\n')
  if (!normalized.endsWith('\r\n')) normalized += '\r\n'
  return normalized
}

async function dcDataToText(data: unknown): Promise<string | null> {
  if (typeof data === 'string') return data
  if (data instanceof ArrayBuffer) return new TextDecoder().decode(data)
  if (ArrayBuffer.isView(data)) return new TextDecoder().decode(data)
  if (data instanceof Blob) return data.text()
  return null
}

/** 浏览器与百炼之间的 PeerConnection。音频走 RTP，事件走服务端 DataChannel `txt`。 */
export class BailianWebRtcSession {
  private pc: RTCPeerConnection | null = null
  private localStream: MediaStream | null = null
  private dc: RTCDataChannel | null = null
  private commandChannel: RTCDataChannel | null = null
  private audioSender: RTCRtpSender | null = null
  private micTrack: MediaStreamTrack | null = null
  private audio: HTMLAudioElement | null = null
  private pendingCommands: string[] = []
  private onDcMessage: ((text: string) => void) | null = null
  private epoch = 0
  private analysisContext: AudioContext | null = null
  private analysisSource: MediaStreamAudioSourceNode | null = null
  private level: AudioLevel | null = null
  private remoteStream: MediaStream | null = null
  private suppressTextAudio = false
  private waitingTextResponse = false
  private playbackDetected = false

  getPlaybackLevel = (): number => {
    const level = this.audio?.paused || this.suppressTextAudio ? 0 : this.level?.read() ?? 0
    if (level > 0.012 && !this.playbackDetected) {
      this.playbackDetected = true
      logger.info('[WebRTC] remote speech playback detected')
    }
    return level
  }

  async createOffer(onDcMessage: (text: string) => void, inputMode: InputMode = 'voice'): Promise<string> {
    const epoch = ++this.epoch
    this.release()
    this.onDcMessage = onDcMessage
    // Resume during the user gesture; failure only disables facial analysis.
    try {
      this.analysisContext = new AudioContext()
      void this.analysisContext.resume().catch(() => {})
    } catch { /* audio playback remains independent */ }

    const stream = inputMode === 'voice' ? await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
        channelCount: 1,
      },
    }) : null
    if (epoch !== this.epoch) {
      stream?.getTracks().forEach((track) => track.stop())
      throw new Error('WebRTC 会话已取消')
    }
    this.localStream = stream

    const pc = new RTCPeerConnection({ iceServers: [] })
    this.pc = pc
    pc.onconnectionstatechange = () => {
      logger.info('[WebRTC] connection state', { state: pc.connectionState })
    }
    pc.oniceconnectionstatechange = () => {
      logger.info('[WebRTC] ice state', { state: pc.iceConnectionState })
    }
    pc.ondatachannel = (event) => {
      this.attachChannel(event.channel)
    }
    const audio = new Audio()
    audio.autoplay = true
    this.audio = audio
    pc.ontrack = (event) => {
      const remote = event.streams[0]
      if (!remote || !this.audio) return
      this.remoteStream = remote
      if (!this.suppressTextAudio) this.audio.srcObject = remote
      if (this.analysisContext) {
        this.analysisSource?.disconnect()
        this.level?.disconnect()
        this.level = new AudioLevel(this.analysisContext)
        this.analysisSource = this.analysisContext.createMediaStreamSource(remote)
        this.level.connect(this.analysisSource)
      }
      if (!this.suppressTextAudio) void this.audio.play().catch((err: unknown) => {
        logger.warn('[WebRTC] remote audio play rejected', {
          message: err instanceof Error ? err.message : 'play failed',
        })
      })
    }

    if (stream) stream.getTracks().forEach((track) => pc.addTrack(track, stream))
    else pc.addTransceiver('audio', { direction: 'recvonly' })
    this.audioSender = stream ? pc.getSenders().find((sender) => sender.track?.kind === 'audio') ?? null : null
    this.micTrack = stream?.getAudioTracks()[0] ?? null
    if (this.audioSender) {
      await this.audioSender.replaceTrack(null)
    }
    const dc = pc.createDataChannel('oai-events')
    this.dc = dc
    this.attachChannel(dc)

    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    if (epoch !== this.epoch) {
      throw new Error('WebRTC 会话已取消')
    }
    await waitForIceGathering(pc, ICE_GATHER_TIMEOUT_MS)
    if (epoch !== this.epoch) {
      throw new Error('WebRTC 会话已取消')
    }
    const sdp = pc.localDescription?.sdp
    if (!sdp) {
      throw new Error('未能生成 WebRTC Offer')
    }
    return sdp
  }

  async acceptAnswer(answerSdp: string): Promise<void> {
    if (!this.pc) {
      throw new Error('PeerConnection 未建立')
    }
    await this.pc.setRemoteDescription({
      type: 'answer',
      sdp: normalizeAnswerSdp(answerSdp),
    })
  }

  sendCommand(event: unknown): void {
    const text = JSON.stringify(event)
    const target = this.openCommandChannel()
    if (!target) {
      this.pendingCommands.push(text)
      return
    }
    target.send(text)
  }

  /** session.update（含 turn_detection）被接受后再送麦，避免上游拒绝修改轮次检测。 */
  attachMicrophone(): void {
    const track = this.micTrack
    const sender = this.audioSender
    if (!track || !sender) return
    track.enabled = true
    void sender.replaceTrack(track)
  }

  interruptTextPlayback(): void {
    this.playbackDetected = false
    this.suppressTextAudio = true
    this.waitingTextResponse = false
    this.audio?.pause()
    if (this.audio) this.audio.srcObject = null
  }

  startTextResponse(): void {
    this.waitingTextResponse = true
  }

  setMicEnabled(enabled: boolean): void {
    const track = this.micTrack
    if (!track) return
    track.enabled = enabled
    if (!enabled) {
      void this.audioSender?.replaceTrack(null)
      return
    }
    void this.audioSender?.replaceTrack(track)
  }

  close(): void {
    this.epoch += 1
    this.release()
  }

  private attachChannel(channel: RTCDataChannel): void {
    channel.binaryType = 'arraybuffer'
    if (channel.label === SERVER_EVENT_CHANNEL) {
      this.commandChannel = channel
    }
    channel.onopen = () => {
      if (channel.label === SERVER_EVENT_CHANNEL) {
        this.commandChannel = channel
      }
      logger.info('[WebRTC] datachannel open', { label: channel.label })
      this.flushCommands()
    }
    channel.onmessage = (event) => {
      void this.onChannelMessage(channel, event.data)
    }
  }

  private async onChannelMessage(channel: RTCDataChannel, data: unknown): Promise<void> {
    if (channel.label === SERVER_EVENT_CHANNEL || !this.commandChannel) {
      this.commandChannel = channel
    }
    const text = await dcDataToText(data)
    if (!text) return
    let type: string
    let parsed: Record<string, unknown>
    try {
      parsed = JSON.parse(text) as Record<string, unknown>
      type = typeof parsed.type === 'string' ? parsed.type : ''
    } catch {
      return
    }
    if (type === 'response.created') {
      if (this.waitingTextResponse) {
        this.waitingTextResponse = false
        this.suppressTextAudio = false
        // Old generation has finished cancellation before the backend requests this response.
        // Recreate the element rather than resuming a paused player with stale buffered media.
        if (this.remoteStream) {
          this.audio = new Audio()
          this.audio.autoplay = true
          this.audio.srcObject = this.remoteStream
          void this.audio.play().catch(() => logger.warn('[WebRTC] reply playback rejected'))
        }
      }
    }
    if (type === 'response.audio.delta') return // WebRTC audio is RTP, not DataChannel PCM
    logger.info('[WebRTC] dc message', { label: channel.label, type })
    this.onDcMessage?.(text)
  }

  private openCommandChannel(): RTCDataChannel | null {
    if (this.commandChannel?.readyState === 'open') return this.commandChannel
    return null
  }

  private release(): void {
    this.playbackDetected = false
    this.remoteStream = null
    this.suppressTextAudio = false
    this.waitingTextResponse = false
    this.analysisSource?.disconnect()
    this.analysisSource = null
    this.level?.disconnect()
    this.level = null
    if (this.analysisContext) void this.analysisContext.close().catch(() => {})
    this.analysisContext = null
    this.pendingCommands = []
    this.onDcMessage = null
    this.commandChannel = null
    this.audioSender = null
    this.micTrack = null
    if (this.dc) {
      try {
        this.dc.close()
      } catch {
        // ignore
      }
      this.dc = null
    }
    if (this.pc) {
      this.pc.ontrack = null
      try {
        this.pc.close()
      } catch {
        // ignore
      }
      this.pc = null
    }
    this.localStream?.getTracks().forEach((track) => track.stop())
    this.localStream = null
    if (this.audio) {
      this.audio.srcObject = null
      this.audio = null
    }
  }

  private flushCommands(): void {
    const target = this.openCommandChannel()
    if (!target) return
    const queued = this.pendingCommands.splice(0, this.pendingCommands.length)
    queued.forEach((text) => target.send(text))
  }
}
