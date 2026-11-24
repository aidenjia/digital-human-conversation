import { lazy, Suspense, useEffect, useRef, useState } from 'react'
import { VoiceButton } from './components'
import { useWebRtcConversation } from './hooks/useWebRtcConversation'
import { REALTIME_MODELS, DEFAULT_REALTIME_PERSONA, DEFAULT_REALTIME_VOICE, type RealtimeModel } from './services/bailianRealtimeConfig'
import './Conversation.css'
import './ConversationRealtime.css'
import { shouldSendOnEnter, type InputMode } from './services/realtimeText'
import { scrollCaptionsToEnd } from './services/captionScroll'

const AvatarStage = lazy(() => import('./avatar/AvatarStage'))

interface BailianWebRtcConversationProps {
  onLogout?: () => void
  onBackToMode?: () => void
}

export default function BailianWebRtcConversation({
  onLogout,
  onBackToMode,
}: BailianWebRtcConversationProps) {
  const {
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
    avatar,
  } = useWebRtcConversation()

  const [model, setModel] = useState<RealtimeModel>(REALTIME_MODELS[0])
  const [persona, setPersona] = useState(DEFAULT_REALTIME_PERSONA)
  const [voice, setVoice] = useState(DEFAULT_REALTIME_VOICE)
  const [inputMode, setInputMode] = useState<InputMode>('text')
  const [draft, setDraft] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [sendError, setSendError] = useState<string | null>(null)
  const inputRef = useRef<HTMLTextAreaElement | null>(null)
  const sendLockRef = useRef(false)
  const captionPanelRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    scrollCaptionsToEnd(captionPanelRef.current)
  }, [captions])

  useEffect(() => {
    if (isConnected && inputMode === 'text' && !isSending) inputRef.current?.focus()
  }, [isConnected, inputMode, isSending])

  const handleMainButtonClick = () => {
    if (isConnected) {
      if (inputMode === 'voice') void toggleMic()
    } else {
      setSendError(null)
      void startConversation(model, persona, voice, inputMode)
    }
  }

  const handleSend = async () => {
    const question = draft.trim()
    if (!question || !isConnected || sendLockRef.current) return
    sendLockRef.current = true
    setIsSending(true)
    setSendError(null)
    setDraft('')
    try { await sendText(question) }
    catch (err) {
      setDraft(current => current || question)
      setSendError(err instanceof Error ? err.message : '发送失败')
    } finally {
      sendLockRef.current = false
      setIsSending(false)
    }
  }

  const handleEndClick = () => {
    if (isConnected) {
      void endConversation()
    }
  }

  return (
    <div className="app realtime-app">
      <div className="app-container">
        <div className="app-header">
          <div className="app-header-left">
            <span className="app-header-title">
              Qwen-Audio WebRTC
            </span>
            <span className="app-header-badge">WebRTC</span>
          </div>
          <div className="app-header-actions">
            {onBackToMode && (
              <button className="back-config-button" type="button" onClick={onBackToMode}>
                返回模式选择
              </button>
            )}
            {onLogout && (
              <button className="logout-button" type="button" onClick={onLogout}>
                退出登录
              </button>
            )}
          </div>
        </div>
        <div className="realtime-workspace">
          <div className="realtime-avatar-panel">
          <Suspense fallback={<div className="info-tip">正在加载 3D 场景…</div>}>
            <AvatarStage runtime={avatar} />
          </Suspense>
          </div>
          <section className="realtime-conversation-panel" aria-label="对话">
          <div className="main-section realtime-main">
          {!isConnected && (
            <>
              <div className="empty-icon">🎙️</div>
              <div className="empty-text">
                填写人设和音色后开始对话
              </div>
              <label className="model-select">
                提问方式
                <select value={inputMode} disabled={isStarting} onChange={event => setInputMode(event.target.value as InputMode)}>
                  <option value="text">文字输入（无需麦克风）</option>
                  <option value="voice">语音输入</option>
                </select>
              </label>
              <label className="model-select">
                人设
                <textarea
                  value={persona}
                  disabled={isStarting}
                  rows={5}
                  onChange={(event) => setPersona(event.target.value)}
                />
              </label>
              <label className="model-select">
                音色
                <input
                  value={voice}
                  disabled={isStarting}
                  onChange={(event) => setVoice(event.target.value)}
                />
              </label>
              <label className="model-select">
                模型
                <select
                  value={model}
                  disabled={isStarting}
                  onChange={(event) => setModel(event.target.value as RealtimeModel)}
                >
                  {REALTIME_MODELS.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
            </>
          )}
          {isConnected && (
            <div className="caption-panel" ref={captionPanelRef} aria-label="对话字幕" role="log">
              {captions.length === 0 ? (
                <div className="caption-placeholder">{inputMode === 'text' ? '请输入问题，数字人将用语音和字幕回复…' : '字幕将显示在这里，请开始说话…'}</div>
              ) : (
                captions.map((line) => (
                  <div
                    key={line.id}
                    className={
                      line.role === 'user' ? 'caption-line caption-user' : 'caption-line caption-ai'
                    }
                  >
                    <span className="caption-role">{line.role === 'user' ? '我' : 'AI'}</span>
                    <span className="caption-text">{line.text}</span>
                  </div>
                ))
              )}
            </div>
          )}
          {sessionMeta && (
            <div className="info-tip" style={{ marginTop: 12 }}>
              model={sessionMeta.model} · voice={sessionMeta.voice}
            </div>
          )}
        </div>
        <div className="control-section">
          {isConnected && inputMode === 'text' && (
            <div className="text-question-panel">
              <textarea ref={inputRef} disabled={isSending} aria-label="输入问题" placeholder="输入问题，Enter 发送，Shift+Enter 换行" rows={3} maxLength={4000}
                value={draft} onChange={event => setDraft(event.target.value)}
                onKeyDown={event => {
                  if (shouldSendOnEnter({ key: event.key, shiftKey: event.shiftKey, isComposing: event.nativeEvent.isComposing, keyCode: event.nativeEvent.keyCode })) {
                    event.preventDefault()
                    void handleSend()
                  }
                }} />
              <button type="button" disabled={!draft.trim() || isSending} onClick={() => { void handleSend() }}>{isSending ? '发送中…' : '发送'}</button>
              {sendError && <div className="error-message" role="alert">{sendError}</div>}
            </div>
          )}
          {error && (
            <div className="error-message">
              <span>⚠️ {error}</span>
              <button type="button" onClick={() => window.location.reload()}>
                重试
              </button>
            </div>
          )}
          <div className="button-group">
            {(!isConnected || inputMode === 'voice') && <VoiceButton
              onClick={handleMainButtonClick}
              isActive={isConnected && isMicEnabled}
              isLoading={isStarting}
              disabled={isStarting}
              label="开始对话"
              activeLabel="关闭麦克风"
            />}
            {isConnected && (
              <button className="end-button" type="button" onClick={handleEndClick}>
                结束对话
              </button>
            )}
          </div>
          <div className="info-tip">
            {isConnected
              ? inputMode === 'text' ? '文字提问 · 语音和字幕回复 · 麦克风未启用' : isMicEnabled
                ? '麦克风已开启，请说话'
                : '麦克风已关闭'
              : '准备就绪'}
          </div>
        </div>
          </section>
        </div>
      </div>
    </div>
  )
}
