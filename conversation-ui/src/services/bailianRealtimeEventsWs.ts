import { parseAvatarEvent, type AvatarEvent } from '@/avatar/avatarEvents'
import { getToken } from '@/utils/auth'
import logger from '@/utils/logger'
import { type RealtimeModel } from '@/services/bailianRealtimeConfig'
import { TextSubmissions } from './realtimeText'

export type EventsSessionMeta = { model: string; voice: string; enableAction: boolean }

export type BailianEventsHandlers = {
  onTextStarted?: () => void
  onSessionReady?: (meta: EventsSessionMeta) => void
  onSpeechStarted?: () => void
  onSpeechStopped?: (reason?: string) => void
  onAction?: (action: { command: string; displayName: string; confirmation: string }) => void
  onUserTranscriptDelta?: (text: string) => void
  onAssistantTranscriptDelta?: (text: string, responseId?: string) => void
  onAvatarEvent?: (event: AvatarEvent) => void
  onServerDraining?: () => void
  onError?: (message: string) => void
  onDownstreamCommand?: (event: unknown) => void
  onClose?: (code: number, reason: string) => void
}

type EventsJson = { type?: string; [key: string]: unknown }

/** DataChannel 上的音频增量留在 RTP，不转发给控制连接。 */
export function shouldForwardDataChannelEvent(text: string): boolean {
  let msg: EventsJson
  try {
    msg = JSON.parse(text) as EventsJson
  } catch {
    return false
  }
  return msg.type !== 'response.audio.delta'
}

export function buildEventsWsUrl(
  apiBase: string,
  accessToken: string,
  options: { relayId: string; persona: string; voice: string; enableAction?: boolean },
): string {
  const base = apiBase.replace(/\/$/, '')
  const u = new URL(base)
  u.protocol = u.protocol === 'https:' ? 'wss:' : 'ws:'
  u.pathname = '/ws/bailian/realtime-events'
  u.search = ''
  u.hash = ''
  u.searchParams.set('access_token', accessToken)
  u.searchParams.set('relayId', options.relayId)
  u.searchParams.set('persona', options.persona)
  u.searchParams.set('voice', options.voice)
  if (options.enableAction) {
    u.searchParams.set('enableAction', 'true')
  }
  return u.toString()
}

/**
 * WebRTC 控制通道。下行 `downstream.command.event` 交给 DataChannel，其余为页面事件。
 */
export class BailianRealtimeEventsWs {
  private submissions = new TextSubmissions()
  private suppressTranscript = false
  private ws: WebSocket | null = null
  private handlers: BailianEventsHandlers = {}
  private sessionReady = false
  private sessionReadyWaiters: Array<{
    resolve: () => void
    reject: (err: Error) => void
  }> = []
  private pingTimer: number | null = null

  async connect(
    apiBase: string,
    handlers: BailianEventsHandlers,
    options: {
      relayId: string
      model: RealtimeModel
      persona: string
      voice: string
      enableAction?: boolean
    },
  ): Promise<void> {
    this.close()
    this.handlers = handlers
    this.sessionReady = false
    this.sessionReadyWaiters = []

    const token = getToken()
    if (!token) {
      throw new Error('未登录，无法连接 WebRTC 控制通道')
    }

    const url = buildEventsWsUrl(apiBase, token, options)
    logger.info('[WebRTC] events connecting', {
      model: options.model,
      voice: options.voice,
      personaLength: options.persona.trim().length,
      enableAction: !!options.enableAction,
    })

    await new Promise<void>((resolve, reject) => {
      const ws = new WebSocket(url)
      this.ws = ws

      const onOpen = () => {
        cleanupConnect()
        this.startHeartbeat()
        resolve()
      }
      const onError = () => {
        cleanupConnect()
        reject(new Error('WebRTC 控制通道连接失败'))
      }
      const cleanupConnect = () => {
        ws.removeEventListener('open', onOpen)
        ws.removeEventListener('error', onError)
      }

      ws.addEventListener('open', onOpen)
      ws.addEventListener('error', onError)
      ws.addEventListener('message', (ev) => this.onMessage(ev))
      ws.addEventListener('close', (ev) => {
        this.stopHeartbeat()
        this.handlers.onClose?.(ev.code, ev.reason)
        this.ws = null
      })
    })
  }

  waitForSessionReady(timeoutMs = 15000): Promise<void> {
    if (this.sessionReady) return Promise.resolve()
    return new Promise((resolve, reject) => {
      const waiter = {
        resolve: () => {
          window.clearTimeout(timer)
          resolve()
        },
        reject: (err: Error) => {
          window.clearTimeout(timer)
          reject(err)
        },
      }
      const timer = window.setTimeout(() => {
        const idx = this.sessionReadyWaiters.indexOf(waiter)
        if (idx >= 0) this.sessionReadyWaiters.splice(idx, 1)
        reject(new Error('Timeout waiting for session.ready'))
      }, timeoutMs)
      this.sessionReadyWaiters.push(waiter)
    })
  }

  sendText(payload: string): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return
    this.ws.send(payload)
  }

  submitText(text: string): Promise<void> {
    return this.submissions.submit(text, event => {
      if (!this.sessionReady || !this.ws || this.ws.readyState !== WebSocket.OPEN) return false
      this.ws.send(JSON.stringify(event))
      this.suppressTranscript = true
      return true
    })
  }

  close(): void {
    this.submissions.close()
    this.suppressTranscript = false
    this.stopHeartbeat()
    if (this.ws) {
      try {
        this.ws.close()
      } catch {
        // ignore
      }
      this.ws = null
    }
    this.sessionReady = false
    const pending = this.sessionReadyWaiters.splice(0, this.sessionReadyWaiters.length)
    pending.forEach((waiter) => waiter.reject(new Error('WebRTC 会话已取消')))
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.pingTimer = window.setInterval(() => {
      this.sendText(JSON.stringify({ type: 'ping' }))
    }, 15000)
  }

  private stopHeartbeat(): void {
    if (this.pingTimer != null) {
      window.clearInterval(this.pingTimer)
      this.pingTimer = null
    }
  }

  private onMessage(ev: MessageEvent): void {
    if (typeof ev.data !== 'string') return
    let msg: EventsJson
    try {
      msg = JSON.parse(ev.data) as EventsJson
    } catch {
      logger.warn('[WebRTC] non-JSON events message')
      return
    }

    if (this.submissions.receive(msg)) return
    switch (msg.type) {
      case 'text.started':
        this.suppressTranscript = false
        this.handlers.onTextStarted?.()
        break
      case 'downstream.command': {
        const event = msg.event
        if (!event || typeof event !== 'object') return
        const eventType =
          'type' in event && typeof event.type === 'string' ? event.type : ''
        logger.info('[WebRTC] downstream.command', { eventType })
        this.handlers.onDownstreamCommand?.(event)
        break
      }
      case 'session.ready': {
        this.sessionReady = true
        const model = typeof msg.model === 'string' ? msg.model : ''
        const voice = typeof msg.voice === 'string' ? msg.voice : ''
        const enableAction = msg.enableAction === true
        this.flushWaiters(this.sessionReadyWaiters)
        this.handlers.onSessionReady?.({ model, voice, enableAction })
        break
      }
      case 'avatar.response':
      case 'avatar.expression':
      case 'avatar.decision': {
        const event = parseAvatarEvent(msg)
        if (event) this.handlers.onAvatarEvent?.(event)
        break
      }
      case 'pong':
        break
      case 'speech.started':
        this.handlers.onSpeechStarted?.()
        break
      case 'speech.stopped': {
        const reason = typeof msg.reason === 'string' ? msg.reason : undefined
        if (reason !== 'turn_invalid') {
          this.handlers.onSpeechStopped?.(reason)
        }
        break
      }
      case 'action': {
        const command = typeof msg.command === 'string' ? msg.command : ''
        const displayName = typeof msg.displayName === 'string' ? msg.displayName : ''
        const confirmation = typeof msg.confirmation === 'string' ? msg.confirmation : ''
        logger.info('[WebRTC] action', { command })
        this.handlers.onAction?.({ command, displayName, confirmation })
        break
      }
      case 'transcript.user': {
        const text = msg.text
        if (typeof text === 'string' && text.length > 0) {
          this.handlers.onUserTranscriptDelta?.(text)
        }
        break
      }
      case 'transcript.assistant': {
        if (this.suppressTranscript) break
        const text = msg.text
        if (typeof text === 'string' && text.length > 0) {
          this.handlers.onAssistantTranscriptDelta?.(text, typeof msg.responseId === 'string' ? msg.responseId : undefined)
        }
        break
      }
      case 'server.draining':
        logger.info('[WebRTC] server.draining')
        this.handlers.onServerDraining?.()
        break
      case 'error': {
        const message = typeof msg.message === 'string' ? msg.message : 'WebRTC 控制通道错误'
        logger.error('[WebRTC] error event', { message })
        this.handlers.onError?.(message)
        break
      }
      default:
        break
    }
  }

  private flushWaiters(
    waiters: Array<{ resolve: () => void; reject: (err: Error) => void }>,
  ): void {
    const copy = waiters.splice(0, waiters.length)
    copy.forEach((waiter) => waiter.resolve())
  }
}
