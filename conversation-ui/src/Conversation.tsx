import { useState } from 'react';
import { VoiceButton } from './components';
import { useConversation } from './hooks';
import './Conversation.css';

interface ConversationProps {
  onLogout?: () => void;
  onBackToMode?: () => void;
}

function Conversation({ onLogout, onBackToMode }: ConversationProps) {
  const [digitalHumanType, setDigitalHumanType] = useState<api.DigitalHumanType>('UE5');
  const {
    isConnected,
    isMicEnabled,
    isStarting,
    error,
    startConversation,
    endConversation,
    toggleMic,
  } = useConversation(digitalHumanType);

  const handleMainButtonClick = () => {
    if (isConnected) {
      toggleMic();
    } else {
      startConversation();
    }
  };

  const handleEndClick = () => {
    if (isConnected) {
      endConversation();
    }
  };

  return (
    <div className="app">
      <div className="app-container">
        <div className="app-header">
          <div className="app-header-left">
            <span className="app-header-title">AI 实时对话</span>
            <span className="app-header-badge">{digitalHumanType}</span>
          </div>
          <div className="app-header-actions">
            {onBackToMode && (
              <button className="back-config-button" onClick={onBackToMode}>
                返回模式选择
              </button>
            )}
            {onLogout && (
              <button className="logout-button" onClick={onLogout}>
                退出登录
              </button>
            )}
          </div>
        </div>
        <div className="main-section">
          {!isConnected && (
            <label>
              数字人类型
              <select
                aria-label="数字人类型"
                value={digitalHumanType}
                disabled={isStarting}
                onChange={event => setDigitalHumanType(event.target.value as api.DigitalHumanType)}
              >
                <option value="UE5">UE5</option>
                <option value="Unity">Unity</option>
                <option value="MMD">MMD</option>
              </select>
            </label>
          )}
          <div className="empty-icon">🎙️</div>
          <div className="empty-text">
            {isConnected
              ? '正在与 AI 对话中'
              : '点击下方按钮开始对话\nAI 会实时识别并回复您'}
          </div>
        </div>
        <div className="control-section">
          {error && (
            <div className="error-message">
              <span>⚠️ {error}</span>
              <button onClick={() => window.location.reload()}>重试</button>
            </div>
          )}
          <div className="button-group">
            <VoiceButton
              onClick={handleMainButtonClick}
              isActive={isConnected && isMicEnabled}
              isLoading={isStarting}
              disabled={isStarting}
              label="开始对话"
              activeLabel="关闭麦克风"
            />
            {isConnected && (
              <button className="end-button" onClick={handleEndClick}>
                结束对话
              </button>
            )}
          </div>
          {isConnected && !isMicEnabled && (
            <div className="info-tip">🔇 麦克风已关闭，点击「关闭麦克风」按钮可重新开启</div>
          )}
          {isConnected && isMicEnabled && (
            <div className="info-tip">🎤 正在通话中，AI 会实时识别并回复您</div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Conversation;
