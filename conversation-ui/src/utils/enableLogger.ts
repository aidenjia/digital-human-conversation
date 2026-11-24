import Debug from 'debug'
export function enableLogger() {
    if (import.meta.env.DEV) {
        Debug.enable('AGORA_AI_VOICE_ASSISTANT*')
    }
}