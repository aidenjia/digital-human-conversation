# bailian-webrtc-events

## Purpose

Define the authenticated WebRTC control channel for session setup, transcripts,
avatar decisions and lifecycle events while media remains on the WebRTC connection.

## Requirements

### Requirement: Separate realtime events WebSocket

The system MUST expose `WS /ws/bailian/realtime-events` for WebRTC control events. The handshake MUST authenticate a valid Users Bearer or enabled demo token. The handshake MUST require query `relayId`, `persona`, and `voice`. `enableAction` MAY be omitted and MUST default to false, accepting `true` / `1`. The handshake MUST NOT read query `model` to override the signaling ticket model. On success the system MUST load the signaling ticket, require its user id to match the authenticated user, then delete the ticket. Blank `persona` or `voice` after trim MUST reject the handshake and MUST NOT consume the ticket. The system MUST NOT open an upstream Bailian WebSocket for this connection. The system MUST NOT accept uplink PCM or emit downlink audio deltas. Logs MUST NOT contain the persona text, JWT, permanent API key, or audio payloads.

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

The events session MUST build Bailian `session.update` instructions as a `## Role` block containing the handshake persona, followed by the server classpath spoken-word rules. The session `voice` MUST be the handshake voice. The session model MUST be the model stored on the signaling ticket. The system MUST NOT substitute `RealtimePersonaPrompts.PERSONA` or `bailian.realtime.default-voice` on this path. When handshake `enableAction` is false, the system MUST NOT register action tools. When it is true, the system MUST register action tools only after the fixed greeting response has finished. The fixed greeting MUST NOT trigger automatic avatar gestures.

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

The client forwards Bailian JSON events from the WebRTC data channel, excluding `response.audio.delta`. The system MUST drop any forwarded `response.audio.delta` without logging its payload. Simplified downlink events MAY include `speech.started`, `speech.stopped`, transcripts, `action`, `error`, `pong`, and `server.draining`. The system MUST NOT send `audio.delta` on this socket. On drain, the system MUST emit `server.draining` and then close the events socket. The obsolete PCM endpoint SHALL NOT be registered.

#### Scenario: Audio delta is not relayed

- **WHEN** the client forwards an event whose type is `response.audio.delta`
- **THEN** the system MUST NOT send `audio.delta` to the client
- **AND** MUST NOT log the audio payload

#### Scenario: Obsolete media relay is unavailable

- **WHEN** a client tries the obsolete media relay endpoint
- **THEN** no PCM WebSocket handler SHALL be registered

### Requirement: Semantic explanation gesture

Automatic avatar decisions SHALL accept semantic intents `none`, `greet`, `agree`
and `explain`. A clear explanation of reasons, mechanisms, distinctions or steps
MAY produce `explain`; short acknowledgements, facts, translations, quoted examples,
refusals, reassurance and explicit physical actions MUST NOT alone qualify.
The classification MUST remain asynchronous and at most once per response.

The frontend SHALL preload the explanation asset only on compatible rigs and SHALL
start it only for a current, unexpired opening cue during detected playback, when
the body is available and cooldown permits. It SHALL NOT wait for a download or
block audio. Actual playback acknowledgements SHALL record cooldown. Interruption
SHALL discard pending cues and blend the body back to idle.

#### Scenario: Prepared explanation plays with speech

- **WHEN** the current response receives an eligible `explain` cue and a compatible asset is ready
- **THEN** the frontend SHALL wait for playback activity before starting the gesture
- **AND** SHALL acknowledge the response id, cue id, intent and chosen motion id once

#### Scenario: Explanation is unavailable or interrupted

- **WHEN** the asset is not prepared, the cue expires, or the user interrupts
- **THEN** the frontend SHALL skip or cancel the gesture without waiting on asset loading
- **AND** an interrupted or superseded cue SHALL NOT animate a later response
