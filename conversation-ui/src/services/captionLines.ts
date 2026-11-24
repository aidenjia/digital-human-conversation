export type CaptionLine = { id: string; role: 'user' | 'assistant'; text: string }

/** Pure updater: React may replay it; response identity is captured before setState. */
export function appendCaptionDelta(lines: CaptionLine[], delta: CaptionLine): CaptionLine[] {
  if (!lines.some(line => line.id === delta.id)) return [...lines, delta]
  return lines.map(line => line.id === delta.id ? { ...line, text: line.text + delta.text } : line)
}
