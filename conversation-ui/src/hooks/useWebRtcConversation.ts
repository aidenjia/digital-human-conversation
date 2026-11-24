import { useCallback, useEffect, useRef, useState } from 'react'
import { isAxiosError } from 'axios'
import { exchangeBailianWebRtc } from '@/apis/bailianWebRtc'
import { BailianWebRtcSession } from '@/services/bailianWebRtcSession'
import {
  BailianRealtimeEventsWs,
  shouldForwardDataChannelEvent,
} from '@/services/bailianRealtimeEventsWs'
import { type RealtimeModel } from '@/services/bailianRealtimeConfig'
import { formatRtcError } from '@/utils/mediaSupport'
import logger from '@/utils/logger'
import { type CaptionLine } from '@/services/captionLines'
import { AvatarRuntime } from '@/avatar/AvatarRuntime'
import { type InputMode } from '@/services/realtimeText'
import { appendCaptionDelta } from '@/services/captionLines'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

function apiErrorMessage(err: unknown): string {
  if (isAxiosError(err)) {
    const data = err.response?.data as api.BaseRes<unknown> | undefined
    if (data?.message) return data.message
    if (data?.msg) return data.msg
  }
  return formatRtcError(err)
}

export function useWebRtcConversation() {
  const [avatar] = useState(() => new AvatarRuntime())
  const [isStarting, setIsStarting] = useState(false)
  const [isConnected, setIsConnected] = useState(false)
  const [isMicEnabled, setIsMicEnabled] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [sessionMeta, setSessionMeta] = useState<{
    model: string
    voice: string
    enableAction: boolean
  } | null>(null)
  const [captions, setCaptions] = useState<CaptionLine[]>([])

  const mediaRef = useRef<BailianWebRtcSession | null>(null)
  const wsRef = useRef<BailianRealtimeEventsWs | null>(null)
  const runIdRef = useRef(0)
  const userLineIdRef = useRef<string | null>(null)
  const assistantLineIdRef = useRef<string | null>(null)
  const captionSeqRef = useRef(0)
  const inputModeRef = useRef<InputMode>('text')
  const sendingRef = useRef(false)

  const nextCaptionId = (role: string) => {
    captionSeqRef.current += 1
    return `${role}-${captionSeqRef.current}-${Date.now()}`
  }

  const appendCaption = useCallback((role: 'user' | 'assistant', delta: string) => {
    const lineIdRef = role === 'user' ? userLineIdRef : assistantLineIdRef
    const id = lineIdRef.current ?? nextCaptionId(role)
    lineIdRef.current = id
    setCaptions(prev => appendCaptionDelta(prev, { id, role, text: delta }))
  }, [])

  const cleanup = useCallback(() => {
    sendingRef.current = false
    avatar.reset()
    runIdRef.current += 1
    wsRef.current?.close()
    wsRef.current = null
    mediaRef.current?.close()
    mediaRef.current = null
    setIsStarting(false)
    setIsConnected(false)
    setIsMicEnabled(false)
    setSessionMeta(null)
    setCaptions([])
    userLineIdRef.current = null
    assistantLineIdRef.current = null
  }, [avatar])

  useEffect(() => {
    return () => {
      cleanup()
    }
  }, [cleanup])

  const startConversation = useCallback(
    async (model: RealtimeModel, persona: string, voice: string, inputMode: InputMode = 'text') => {
      inputModeRef.current = inputMode
      const personaText = persona.trim()
      const voiceName = voice.trim()
      if (!personaText) {
        setError('人设不能为空')
        return
      }
      if (!voiceName) {
        setError('音色不能为空')
        return
      }

      const runId = runIdRef.current + 1
      runIdRef.current = runId
      setIsStarting(true)
      setError(null)
      setCaptions([])
      userLineIdRef.current = null
      assistantLineIdRef.current = null

      const media = new BailianWebRtcSession()
      avatar.setAudioReader(media.getPlaybackLevel)
      const ws = new BailianRealtimeEventsWs()
      mediaRef.current = media
      wsRef.current = ws
      avatar.setGestureStartedHandler(event => {
        ws.sendText(JSON.stringify({ type: 'avatar.gesture.started', ...event }))
      })
      const stale = () => runId !== runIdRef.current

      try {
        const offerSdp = await media.createOffer((text) => {
          if (!shouldForwardDataChannelEvent(text)) return
          ws.sendText(text)
        }, inputMode)
        if (stale()) return

        const res = (await exchangeBailianWebRtc({
          offerSdp,
          model,
        })) as api.BaseRes<api.BailianWebRtcExchangeResponse>
        if (stale()) return
        if (!res.success || !res.data?.answerSdp || !res.data.relayId) {
          throw new Error(res.message || res.msg || 'WebRTC 信令失败')
        }

        await ws.connect(
          API_BASE,
          {
            onTextStarted: () => { media.startTextResponse(); avatar.stopListening() },
            onSessionReady: (meta) => {
              logger.info('[WebRTC] session.ready', meta)
              if (inputMode === 'voice') media.attachMicrophone()
              setSessionMeta(meta)
            },
            onSpeechStarted: () => {
              avatar.interrupt()
              logger.info('[WebRTC] speech.started')
              userLineIdRef.current = null
              assistantLineIdRef.current = null
            },
            onSpeechStopped: () => {
              avatar.stopListening()
              logger.info('[WebRTC] speech.stopped')
            },
            onAction: (action) => {
              avatar.act(action.command)
              logger.info('[WebRTC] action', { command: action.command })
            },
            onUserTranscriptDelta: (text) => {
              appendCaption('user', text)
            },
            onAvatarEvent: (event) => { avatar.receive(event) },
            onAssistantTranscriptDelta: (text, responseId) => {
              avatar.observeTranscript(text, responseId)
              appendCaption('assistant', text)
            },
            onDownstreamCommand: (event) => {
              media.sendCommand(event)
            },
            onServerDraining: () => {
              setError('服务正在下线，请重新开始对话')
              cleanup()
            },
            onError: (message) => {
              logger.error('[WebRTC] events error', { message })
              setError(message)
            },
            onClose: (code, reason) => {
              logger.info('[WebRTC] events closed', { code, reason })
              if (!stale()) { setError('连接已断开，请重新开始对话'); cleanup() }
            },
          },
          {
            relayId: res.data.relayId,
            model,
            persona: personaText,
            voice: voiceName,
            enableAction: true,
          },
        )
        if (stale()) return

        await media.acceptAnswer(res.data.answerSdp)
        await ws.waitForSessionReady()
        if (stale()) return

        setIsConnected(true)
        setIsMicEnabled(inputMode === 'voice')
        logger.info('[WebRTC] conversation live — please speak')
      } catch (err: unknown) {
        if (stale()) return
        const msg = apiErrorMessage(err)
        setError(msg)
        logger.error('[WebRTC] start failed', { message: msg })
        cleanup()
      } finally {
        if (!stale()) setIsStarting(false)
      }
    },
    [appendCaption, cleanup, avatar],
  )

  const endConversation = useCallback(() => {
    logger.info('[WebRTC] ending conversation')
    cleanup()
  }, [cleanup])

  const toggleMic = useCallback(() => {
    if (inputModeRef.current !== 'voice') return
    const media = mediaRef.current
    if (!media || !isConnected) return
    const next = !isMicEnabled
    media.setMicEnabled(next)
    setIsMicEnabled(next)
  }, [isConnected, isMicEnabled])

  const sendText = useCallback(async (text: string) => {
    if (!isConnected || !wsRef.current) throw new Error('连接尚未就绪')
    if (sendingRef.current) throw new Error('上一条提交仍在处理中')
    if (!text.trim() || text.trim().length > 4000) throw new Error('请输入 1–4000 字的问题')
    const runId = runIdRef.current
    sendingRef.current = true
    setError(null)
    const result = wsRef.current.submitText(text)
    mediaRef.current?.interruptTextPlayback()
    avatar.interrupt()
    avatar.stopListening()
    try {
      await result
      if (runId !== runIdRef.current) return
      userLineIdRef.current = null
      assistantLineIdRef.current = null
      appendCaption('user', text.trim())
      userLineIdRef.current = null
    } finally { sendingRef.current = false }
  }, [isConnected, appendCaption, avatar])

  return {
    avatar,
    isConnected,
    isMicEnabled,
    isStarting,
    error,
    sessionMeta,
    captions,
    startConversation,
    endConversation,
    toggleMic,
    sendText,
  }
}
