import request from '@/utils/request'

export function exchangeBailianWebRtc(body: {
  offerSdp: string
  model?: string
}): Promise<api.BaseRes<api.BailianWebRtcExchangeResponse>> {
  return request.post('/api/bailian/realtime/webrtc', body)
}
