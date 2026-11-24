## ADDED Requirements

### Requirement: Authenticated Bailian realtime relay WebSocket

The system MUST expose an authenticated WebSocket endpoint for Bailian Qwen-Audio Realtime relay (path agreed in design, e.g. `/ws/bailian/realtime`). The connection MUST authenticate the Users OpenID access token before accepting application traffic. On auth failure the system MUST close the socket without opening an upstream Bailian connection. The permanent DashScope API key MUST NOT be sent to the client. Logs MUST NOT contain the permanent key, client JWT, or full PCM payloads.

#### Scenario: Valid token establishes relay session

- **WHEN** a client connects with a valid Bearer token and Bailian realtime is configured
- **THEN** the system MUST accept the WebSocket
- **AND** MUST open an upstream Bailian Realtime WebSocket using the server-side permanent key
- **AND** MUST apply server-configured session parameters (including turn detection mode and history bounds from configuration)
- **AND** MUST open the upstream session with query `model` equal to the client handshake `model`, which MUST be exactly `qwen-audio-3.0-realtime-flash` or `qwen-audio-3.0-realtime-plus`

#### Scenario: Model query is required and allowlisted

- **WHEN** an authenticated client connects without `model`, or with any value other than the two allowlisted model ids
- **THEN** the system MUST reject the handshake with HTTP 400
- **AND** MUST NOT open an upstream Bailian connection

#### Scenario: Missing or invalid token is rejected

- **WHEN** the client connects without a valid Users token
- **THEN** the system MUST reject or close the connection
- **AND** MUST NOT call Bailian with the permanent key for that attempt

### Requirement: Simplified client event contract

Successful relay sessions MUST speak a simplified JSON event protocol with the client. The client MUST be able to send uplink PCM audio chunks and heartbeats without emitting Bailian-native `session.update` frames. The server MUST forward uplink audio to Bailian and MUST forward downlink audio deltas and transcript deltas to the client in the simplified schema. The system MUST NOT require the client to hold a Bailian temporary API key for the formal path.

#### Scenario: Uplink audio is relayed

- **WHEN** an authenticated client sends an uplink audio append event with PCM payload
- **THEN** the system MUST forward corresponding audio to the upstream Bailian session
- **AND** MUST NOT log the raw audio payload

#### Scenario: Downlink audio and captions are relayed

- **WHEN** Bailian emits audio or transcript delta events
- **THEN** the system MUST send corresponding simplified events to the client WebSocket

### Requirement: Formal path supersedes client-direct temporary key

The formal client integration path for Qwen-Audio Realtime with Shengwang 3A MUST be: obtain RTC credentials via existing HTTP API, join Agora for 3A/playback reference, and converse via this relay WebSocket. Documentation MUST describe the temporary-key HTTP API as transitional or test-only once the relay is available.

#### Scenario: README documents relay as formal path

- **WHEN** this change is implemented
- **THEN** README MUST document the relay WebSocket sequence as the formal client path
- **AND** MUST state that direct Bailian WebSocket with temporary keys is not the formal path
