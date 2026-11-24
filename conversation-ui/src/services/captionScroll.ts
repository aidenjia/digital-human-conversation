/** Scroll only the transcript, not its ancestors or the question composer. */
export function scrollCaptionsToEnd(panel: Pick<HTMLElement, 'scrollTop' | 'scrollHeight'> | null): void {
  if (panel) panel.scrollTop = panel.scrollHeight
}
