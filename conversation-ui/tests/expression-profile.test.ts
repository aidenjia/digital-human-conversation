import test from 'node:test'
import assert from 'node:assert/strict'
import { expressionTarget, expressionSpeed } from '../src/avatar/expressionProfile.ts'

test('sad and surprised are more visible while preserving zero and bounded weights', () => {
  assert.equal(expressionTarget('sad', 0, 0), 0)
  assert.ok(expressionTarget('sad', 0.35, 0) > expressionTarget('happy', 0.35, 0))
  assert.ok(expressionTarget('surprised', 0.35, 0) > expressionTarget('happy', 0.35, 0))
  assert.ok(expressionTarget('surprised', 1, 0) <= 0.85)
})

test('speech retains more expression than the former global attenuation', () => {
  for (const emotion of ['happy', 'sad', 'surprised'] as const) {
    const quiet = expressionTarget(emotion, 0.35, 0)
    const speaking = expressionTarget(emotion, 0.35, 1)
    assert.ok(speaking < quiet && speaking > quiet * 0.35)
  }
  assert.ok(expressionSpeed('surprised', true) > expressionSpeed('sad', true))
})

test('anger remains visibly stronger than a mild smile without exceeding the expression limit', () => {
  assert.ok(expressionTarget('angry', 0.35, 0) > expressionTarget('happy', 0.35, 0))
  assert.ok(expressionTarget('angry', 1, 0) <= 0.85)
})
