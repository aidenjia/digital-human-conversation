export type InputMode = 'text' | 'voice'

export function shouldSendOnEnter(event: { key: string; shiftKey: boolean; isComposing: boolean; keyCode: number }): boolean {
  return event.key === 'Enter' && !event.shiftKey && !event.isComposing && event.keyCode !== 229
}

/** One in-flight submission; a receipt means accepted by our backend, not model completion. */
export class TextSubmissions {
  private pending: { id: string; resolve: () => void; reject: (error: Error) => void; timer: ReturnType<typeof setTimeout> } | null = null
  private sequence = 0

  submit(text: string, send: (event: Record<string, unknown>) => boolean): Promise<void> {
    const value = text.trim()
    if (!value) return Promise.reject(new Error('问题不能为空'))
    if (value.length > 4000) return Promise.reject(new Error('问题不能超过 4000 字'))
    if (this.pending) return Promise.reject(new Error('上一条提交仍在处理中'))
    return new Promise((resolve, reject) => {
      const id = `text-${Date.now()}-${++this.sequence}`
      const timer = setTimeout(() => {
        this.pending = null
        reject(new Error('提交回执超时，结果未知；请确认字幕后再发送，避免重复提问'))
      }, 15000)
      this.pending = { id, resolve, reject, timer }
      try {
        if (!send({ type: 'text.submit', id, text: value })) this.fail('连接尚未就绪')
      } catch {
        this.fail('连接已断开，未能发送')
      }
    })
  }

  receive(event: Record<string, unknown>): boolean {
    if (event.type !== 'text.accepted' && event.type !== 'text.rejected') return false
    const pending = this.pending
    if (!pending || event.id !== pending.id) return true
    this.pending = null
    clearTimeout(pending.timer)
    if (event.type === 'text.accepted') pending.resolve()
    else pending.reject(new Error(typeof event.message === 'string' ? event.message : '提交被拒绝'))
    return true
  }

  close(): void { this.fail('连接已断开') }

  private fail(message: string): void {
    const pending = this.pending
    this.pending = null
    if (!pending) return
    clearTimeout(pending.timer)
    pending.reject(new Error(message))
  }
}
