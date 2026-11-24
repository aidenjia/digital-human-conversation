import type { VoiceMode } from '@/utils/session'
import './ModeSelect.css'

interface ModeSelectProps {
  onSelect: (mode: VoiceMode) => void
  onLogout?: () => void
}

export default function ModeSelect({ onSelect, onLogout }: ModeSelectProps) {
  return (
    <div className="mode-page">
      <div className="mode-container">
        <div className="mode-header">
          <h1 className="mode-title">选择通话模式</h1>
          {onLogout && (
            <button type="button" className="mode-logout" onClick={onLogout}>
              退出登录
            </button>
          )}
        </div>
        <p className="mode-subtitle">请选择本次使用的语音链路</p>
        <div className="mode-cards">
          <button
            type="button"
            className="mode-card mode-card-webrtc"
            onClick={() => onSelect('webrtc')}
          >
            <span className="mode-card-label">百炼 WebRTC</span>
            <span className="mode-card-desc">
              Qwen-Audio 实时对话，支持文字、语音和数字人动作
            </span>
          </button>
          <button
            type="button"
            className="mode-card mode-card-agora"
            onClick={() => onSelect('agora')}
          >
            <span className="mode-card-label">Agora ConvoAI</span>
            <span className="mode-card-desc">声网实时语音，与远端 Agent 通话</span>
          </button>
        </div>
      </div>
    </div>
  )
}
