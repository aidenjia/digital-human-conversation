import assert from 'node:assert/strict'
import test from 'node:test'
import { BailianWebRtcSession } from '../src/services/bailianWebRtcSession.ts'

test('text offer is receive-only and never requests a microphone', async () => {
  let requests = 0
  const directions: unknown[] = []
  const previous = { navigator: Object.getOwnPropertyDescriptor(globalThis, 'navigator'), rtc: globalThis.RTCPeerConnection, audio: globalThis.Audio }
  Object.defineProperty(globalThis, 'navigator', { configurable: true, value: { mediaDevices: { getUserMedia() { requests++; throw new Error('microphone denied') } } } })
  class Peer {
    iceGatheringState = 'complete'
    localDescription = { sdp: 'offer' }
    addTransceiver(_kind: string, options: unknown) { directions.push(options) }
    createDataChannel() { return { label: 'oai-events', readyState: 'connecting', close() {} } }
    async createOffer() { return { sdp: 'offer' } }
    async setLocalDescription() {}
    close() {}
  }
  Object.assign(globalThis, { RTCPeerConnection: Peer, Audio: class { autoplay = false; srcObject = null } })
  try {
    const media = new BailianWebRtcSession()
    await media.createOffer(() => {}, 'text')
    assert.equal(requests, 0)
    assert.deepEqual(directions, [{ direction: 'recvonly' }])
    media.close()
  } finally {
    if (previous.navigator) Object.defineProperty(globalThis, 'navigator', previous.navigator)
    Object.assign(globalThis, { RTCPeerConnection: previous.rtc, Audio: previous.audio })
  }
})

test('text RTP playback detaches on interruption and resumes only for the new response', async () => {
  let paused = 0
  let played = 0
  const audios: Array<{ srcObject: unknown }> = []
  let peer: Record<string, unknown> = {}
  const previous = { context: globalThis.AudioContext, rtc: globalThis.RTCPeerConnection, audio: globalThis.Audio }
  class Context {
    state = 'running'; currentTime = 0; destination = {}
    createAnalyser() { return { fftSize: 0, smoothingTimeConstant: 0, disconnect() {}, getFloatTimeDomainData() {} } }
    createBuffer() { return { duration: 0.1, copyToChannel() {} } }
    createMediaStreamSource() { return { connect() {}, disconnect() {} } }
    async close() {}
  }
  class Peer {
    iceGatheringState = 'complete'; localDescription = { sdp: 'offer' }
    constructor() { peer = this as unknown as Record<string, unknown> }
    addTransceiver() {}
    createDataChannel() { return { label: 'oai-events', readyState: 'connecting', close() {} } }
    async createOffer() { return { sdp: 'offer' } }
    async setLocalDescription() {}
    close() {}
  }
  Object.assign(globalThis, { AudioContext: Context, RTCPeerConnection: Peer, Audio: class {
    autoplay = false; srcObject: unknown = null
    constructor() { audios.push(this) }
    async play() { played++ }
    pause() { paused++ }
  } })
  try {
    const media = new BailianWebRtcSession()
    await media.createOffer(() => {}, 'text')
    const channel: Record<string, unknown> = { label: 'txt', readyState: 'open', send() {} }
    ;(peer.ondatachannel as (event: unknown) => void)({ channel })
    const receive = async (message: unknown) => {
      await (channel.onmessage as (event: unknown) => void)({ data: JSON.stringify(message) })
    }
    const stream = { id: 'remote' }
    ;(peer.ontrack as (event: unknown) => void)({ streams: [stream] })
    assert.equal(played, 1)
    media.interruptTextPlayback()
    assert.equal(paused, 1)
    assert.equal(audios.at(-1)?.srcObject, null)
    await receive({ type: 'response.created', response: { id: 'old' } })
    assert.equal(played, 1)
    media.startTextResponse()
    await receive({ type: 'response.created', response: { id: 'new' } })
    assert.equal(played, 2)
    assert.equal(audios.at(-1)?.srcObject, stream)
    media.close()
  } finally { Object.assign(globalThis, { AudioContext: previous.context, RTCPeerConnection: previous.rtc, Audio: previous.audio }) }
})
