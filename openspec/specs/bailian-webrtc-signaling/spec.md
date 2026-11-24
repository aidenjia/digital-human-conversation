# bailian-webrtc-signaling

## Requirements

### Requirement: Authenticated WebRTC SDP exchange

The system MUST expose `POST /api/bailian/realtime/webrtc` authenticated with a Users OpenID Bearer token. When demo-token bypass is enabled, the configured demo token MUST be accepted here, and the ticket user id MUST be the configured demo user id. The request JSON MUST include `offerSdp`. `model` MAY be omitted. The request MUST NOT require `persona`, `voice`, or `enableAction`. The response MUST use the `ApiResponse` envelope. The permanent DashScope API key MUST NOT appear in the response. Logs MUST NOT contain the permanent key, the client JWT, or the full Offer or Answer SDP.

#### Scenario: Legal flash exchange returns answer and ticket

- **WHEN** an authenticated client posts a non-blank `offerSdp` and omits `model`
- **THEN** the system MUST return HTTP 200 with `data.answerSdp`, `data.relayId`, and `data.model` equal to `qwen-audio-3.0-realtime-flash`
- **AND** MUST store a single-use ticket binding that user and model only

#### Scenario: Exchange does not require Agora credentials

- **WHEN** an authenticated client posts a non-blank `offerSdp` without first calling `POST /api/rtc/credentials`
- **THEN** the system MUST still complete signaling
- **AND** the response data MUST NOT include Agora `appId`, `channel`, or `token`

#### Scenario: Plus model is forwarded upstream

- **WHEN** an authenticated client posts `model` equal to `qwen-audio-3.0-realtime-plus` and a non-blank `offerSdp`
- **THEN** the system MUST call Bailian WebRTC signaling with query `model=qwen-audio-3.0-realtime-plus`
- **AND** MUST echo that model in `data.model`

#### Scenario: Missing token is rejected

- **WHEN** the client calls the endpoint without a Bearer token
- **THEN** the system MUST reject the call
- **AND** MUST NOT call Bailian signaling

#### Scenario: Invalid token is rejected

- **WHEN** the client calls the endpoint with a Bearer token that fails Users JWT validation and is not the enabled demo token
- **THEN** the system MUST reject the call
- **AND** MUST NOT call Bailian signaling

#### Scenario: Illegal model is rejected before upstream

- **WHEN** an authenticated client posts a `model` other than `qwen-audio-3.0-realtime-flash` or `qwen-audio-3.0-realtime-plus`
- **THEN** the system MUST reject the call with HTTP 400
- **AND** MUST NOT call Bailian signaling
- **AND** MUST NOT create a ticket

### Requirement: Server-side signaling uses the permanent key

The system MUST exchange SDP with Bailian at `https://{workspaceId}.cn-beijing.maas.aliyuncs.com/api/v1/webrtc/realtime?model={model}` using `Content-Type: application/sdp` and `Authorization: Bearer` set to the server-side permanent API key. The Offer body MUST be the client `offerSdp`. A successful upstream body MUST be returned as `data.answerSdp`. Upstream failure MUST surface as the service's existing upstream or configuration error and MUST NOT return the permanent key.

#### Scenario: Upstream rejection does not leak the key

- **WHEN** Bailian signaling returns a non-success status
- **THEN** the system MUST fail the client request without `answerSdp`
- **AND** MUST NOT include the permanent API key in the response or in logs
