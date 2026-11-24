
declare global {
  namespace api {
    type DigitalHumanType = 'UE5' | 'Unity' | 'MMD'

    interface BaseRes<T> {
      success: boolean
      code: number
      data: T
      msg: string
      message?: string
    }

    interface GetConfigPayload {
      deviceId: string
      digitalHumanType: DigitalHumanType
    }

    interface ConfigResponse {
      appId: string
      channel: string
      token: string
      userId: string
      agentId: string
    }

    interface JoinConfig {
      appId: string
      channel: string
      token: string
      uid: number | string
    }

    interface LeavePayload {
      agentId: string
    }

    interface RtcCredentialsResponse {
      appId: string
      channel: string
      token: string
      userId: string
    }

    interface BailianWebRtcExchangeResponse {
      answerSdp: string
      relayId: string
      model: string
    }

  }
}

export default global
