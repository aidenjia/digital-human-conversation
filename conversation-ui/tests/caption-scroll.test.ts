import assert from 'node:assert/strict'
import test from 'node:test'
import { scrollCaptionsToEnd } from '../src/services/captionScroll.ts'

test('caption scroll touches only its container, never a surrounding page', () => {
  const page = { scrollTop: 17 }
  const panel = { scrollTop: 0, scrollHeight: 900 }
  scrollCaptionsToEnd(panel)
  assert.equal(panel.scrollTop, 900)
  assert.equal(page.scrollTop, 17)
  panel.scrollHeight = 1200
  scrollCaptionsToEnd(panel)
  assert.equal(panel.scrollTop, 1200)
})

test('caption scroll tolerates a disconnected/unmounted panel', () => {
  assert.doesNotThrow(() => scrollCaptionsToEnd(null))
})
