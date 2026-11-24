import { useCallback, useEffect, useState } from 'react'
import Conversation from './Conversation'
import BailianWebRtcConversation from './BailianWebRtcConversation'
import Login from './pages/Login'
import ModeSelect from './pages/ModeSelect'
import { AUTH_UNAUTHORIZED_EVENT, clearToken, isAuthenticated } from '@/utils/auth'
import { clearTestSession, getVoiceMode, setVoiceMode, type VoiceMode } from '@/utils/session'

type AppStage = 'login' | 'mode' | 'conversation-agora' | 'conversation-webrtc'

function resolveInitialStage(): AppStage {
  if (!isAuthenticated()) return 'login'
  const mode = getVoiceMode()
  return mode ? `conversation-${mode}` : 'mode'
}

export default function App() {
  const [stage, setStage] = useState<AppStage>(resolveInitialStage)
  const handleLogout = useCallback(() => {
    clearToken()
    clearTestSession()
    setStage('login')
  }, [])
  const handleModeSelect = useCallback((mode: VoiceMode) => {
    setVoiceMode(mode)
    setStage(`conversation-${mode}`)
  }, [])
  useEffect(() => {
    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleLogout)
    return () => window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleLogout)
  }, [handleLogout])
  if (stage === 'login') {
    return <Login onSuccess={() => setStage('mode')} />
  }
  if (stage === 'mode') {
    return <ModeSelect onSelect={handleModeSelect} onLogout={handleLogout} />
  }
  const props = { onLogout: handleLogout, onBackToMode: () => setStage('mode') }
  return stage === 'conversation-webrtc'
    ? <BailianWebRtcConversation {...props} />
    : <Conversation {...props} />
}
