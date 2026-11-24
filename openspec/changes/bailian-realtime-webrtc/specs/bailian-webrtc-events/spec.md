## ADDED Requirements

### Requirement: Separate realtime events WebSocket

The system MUST expose `WS /ws/bailian/realtime-events` for WebRTC control events. The handshake MUST authenticate the same Users Bearer or enabled demo token as the PCM relay. The handshake MUST require query `relayId`, `persona`, and `voice`. `enableAction` MAY be omitted and MUST default to false, using the same `true` / `1` parsing as the PCM relay. The handshake MUST NOT read query `model` to override the signaling ticket model. On success the system MUST load the signaling ticket, require its user id to match the authenticated user, then delete the ticket. Blank `persona` or `voice` after trim MUST reject the handshake and MUST NOT consume the ticket. The system MUST NOT open an upstream Bailian WebSocket for this connection. The system MUST NOT accept uplink PCM or emit downlink audio deltas. Logs MUST NOT contain the persona text, JWT, permanent API key, or audio payloads.

#### Scenario: Valid ticket and session query start an events session

- **WHEN** the same user who created a ticket connects with that `relayId`, a non-blank `persona`, and a non-blank `voice` before the ticket expires
- **THEN** the system MUST accept the WebSocket
- **AND** MUST delete the ticket so a second connection with the same `relayId` is rejected
- **AND** MUST NOT open a Bailian upstream WebSocket

#### Scenario: Blank persona or voice is rejected

- **WHEN** an authenticated client connects with a valid unused `relayId` but omits `persona` or `voice`, or sends a blank value after trim
- **THEN** the system MUST reject the handshake
- **AND** MUST NOT consume the ticket
- **AND** MUST NOT open a Bailian upstream WebSocket

#### Scenario: Expired, missing, or foreign ticket is rejected

- **WHEN** `relayId` is missing, unknown, expired, or bound to a different user id
- **THEN** the system MUST reject the handshake
- **AND** MUST NOT open a Bailian upstream WebSocket

#### Scenario: Missing or invalid token is rejected

- **WHEN** the client connects without a valid Users token and without the enabled demo token
- **THEN** the system MUST reject the handshake
- **AND** MUST NOT consume a ticket

### Requirement: Persona and voice come from the events handshake

The events session MUST build Bailian `session.update` instructions as a `## Role` block containing the handshake persona, followed by the server classpath spoken-word rules. The session `voice` MUST be the handshake voice. The session model MUST be the model stored on the signaling ticket. The system MUST NOT substitute `RealtimePersonaPrompts.PERSONA` or `bailian.realtime.default-voice` on this path. When handshake `enableAction` is false, the system MUST NOT register action tools. When it is true, the system MUST register action tools only after the fixed greeting response has finished, matching the PCM relay ordering.

#### Scenario: Session update uses the WebSocket persona and voice

- **WHEN** the client forwards a Bailian `session.created` event after the events socket is up
- **THEN** the system MUST return a downstream command whose `session.update` instructions contain the handshake persona and whose `voice` equals the handshake voice

#### Scenario: Ready event echoes the ticket model and handshake voice

- **WHEN** the client forwards the first Bailian `session.updated` event
- **THEN** the system MUST send `session.ready` including the ticket `model` and the handshake `voice`

#### Scenario: Query model does not override the ticket

- **WHEN** the events handshake includes a `model` query different from the ticket model, together with a valid ticket, persona, and voice
- **THEN** the system MUST still apply the ticket model in `session.ready`

### Requirement: Audio stays off the events socket

The client forwards Bailian JSON events from the WebRTC data channel, excluding `response.audio.delta`. The system MUST drop any forwarded `response.audio.delta` without logging its payload. Simplified downlink events MAY include `speech.started`, `speech.stopped`, transcripts, `action`, `error`, `pong`, and `server.draining`. The system MUST NOT send `audio.delta` on this socket. On drain, the system MUST emit `server.draining` and then close the events socket. The PCM endpoint `WS /ws/bailian/realtime` MUST keep its existing audio relay behavior.

#### Scenario: Audio delta is not relayed

- **WHEN** the client forwards an event whose type is `response.audio.delta`
- **THEN** the system MUST NOT send `audio.delta` to the client
- **AND** MUST NOT log the audio payload

#### Scenario: PCM relay path is unchanged

- **WHEN** a client connects to `WS /ws/bailian/realtime` with its existing handshake query
- **THEN** the system MUST continue to open an upstream Bailian WebSocket and relay PCM
- **AND** MUST NOT require `relayId`
