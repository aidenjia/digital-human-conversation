const VOICE_MODE_KEY = 'test_voice_mode'

export type VoiceMode = 'agora' | 'webrtc'

export function getVoiceMode(): VoiceMode | null {
  const value = sessionStorage.getItem(VOICE_MODE_KEY)
  return value === 'agora' || value === 'webrtc' ? value : null
}

export function setVoiceMode(mode: VoiceMode): void {
  sessionStorage.setItem(VOICE_MODE_KEY, mode)
}

export function clearTestSession(): void {
  sessionStorage.removeItem('test_digital_human_type')
  sessionStorage.removeItem('test_config_saved')
  sessionStorage.removeItem(VOICE_MODE_KEY)
}
