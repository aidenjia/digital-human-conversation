import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync, existsSync } from 'node:fs'
import { getVoiceMode, setVoiceMode } from '../src/utils/session.ts'

test('only retained modes restore; removed saved modes return to selection', (t) => {
  const priorStorage = Object.getOwnPropertyDescriptor(globalThis, 'sessionStorage')
  t.after(() => {
    if (priorStorage) {
      Object.defineProperty(globalThis, 'sessionStorage', priorStorage)
    } else {
      Reflect.deleteProperty(globalThis, 'sessionStorage')
    }
  })
  const values = new Map<string, string>()
  Object.defineProperty(globalThis, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => values.set(key, value),
    },
  })
  for (const mode of ['realtime', 'legacy', 'unknown']) {
    values.set('test_voice_mode', mode)
    assert.equal(getVoiceMode(), null)
  }
  for (const mode of ['agora', 'webrtc'] as const) {
    setVoiceMode(mode)
    assert.equal(getVoiceMode(), mode)
  }
})

test('selection contains exactly the two retained conversation paths', () => {
  const source = readFileSync(new URL('../src/pages/ModeSelect.tsx', import.meta.url), 'utf8')
  const modes = [...source.matchAll(/onSelect\('([^']+)'\)/g)]
    .map(match => match[1])
    .sort()
  assert.deepEqual(modes, ['agora', 'webrtc'])
})

test('WebRTC page uses its own hook and independent configuration', () => {
  const page = new URL('../src/BailianWebRtcConversation.tsx', import.meta.url)
  assert.ok(existsSync(page), 'dedicated WebRTC page exists')
  const source = readFileSync(page, 'utf8')
  assert.match(source, /useWebRtcConversation\(\)/)
  assert.doesNotMatch(source, /useRealtimeConversation|bailianRealtimeWs|MotionGenerationPanel|transport/)
  for (const file of ['hooks/useWebRtcConversation.ts', 'services/bailianRealtimeEventsWs.ts']) {
    const module = readFileSync(new URL(`../src/${file}`, import.meta.url), 'utf8')
    assert.doesNotMatch(module, /bailianRealtimeWs|hooks\/useRealtimeConversation/)
  }
})
