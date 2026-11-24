export function formatRtcError(err: unknown): string {
  if (!(err instanceof Error)) {
    return '操作失败，请重试';
  }

  const msg = err.message;
  if (/DEVICE_NOT_FOUND|NotFoundError|Requested device not found/i.test(msg)) {
    return '未找到麦克风设备：请确认电脑已接入麦克风，并在 Windows「设置 → 隐私和安全性 → 麦克风」中允许浏览器访问';
  }
  if (/getUserMedia|NOT_SUPPORTED/i.test(msg)) {
    if (typeof window !== 'undefined' && !window.isSecureContext) {
      return '无法访问麦克风：请使用 https:// 或 http://localhost 访问';
    }
    return '无法访问麦克风：浏览器不支持或未授权麦克风权限';
  }
  if (/Permission denied|NotAllowedError|permission/i.test(msg)) {
    return '麦克风权限被拒绝，请在浏览器地址栏左侧允许麦克风访问';
  }
  return msg;
}
