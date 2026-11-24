## ADDED Requirements

### Requirement: Issue Bailian realtime temporary API key

The system MUST expose `POST /api/bailian/realtime-tokens` authenticated with Users OpenID Bearer. Using a server-side permanent DashScope API key, the system MUST request a temporary token from Bailian's tokens API and MUST return `ApiResponse.data` containing at least: `token` (temporary key), `expiresAt` (unix seconds), `wsUrl` (WebSocket URL including workspace host and `model` query), and `model` equal to `qwen-audio-3.0-realtime-flash`. The permanent API key MUST NOT appear in any response field. Logs MUST NOT contain the permanent key or the temporary `token` value.

#### Scenario: Authenticated caller receives temporary key metadata

- **WHEN** Bailian credentials are configured and an authenticated client calls `POST /api/bailian/realtime-tokens`
- **AND** Bailian tokens API succeeds
- **THEN** the system MUST return HTTP 200 with `data.token`, `data.expiresAt`, `data.wsUrl`, and `data.model=qwen-audio-3.0-realtime-flash`
- **AND** `data` MUST NOT contain the permanent DashScope API key

#### Scenario: Missing configuration fails closed

- **WHEN** the permanent DashScope API key or workspace id is missing
- **AND** an authenticated client calls `POST /api/bailian/realtime-tokens`
- **THEN** the system MUST fail with a configuration-style error (service unavailable / configuration envelope)
- **AND** the system MUST NOT return a permanent key as a fallback

#### Scenario: Upstream tokens failure is mapped

- **WHEN** Bailian tokens API returns a non-success HTTP status or unusable body
- **THEN** the system MUST fail with an upstream-style error envelope
- **AND** the system MUST NOT log the request Authorization secret or response temporary token

#### Scenario: Auth failures match other protected APIs

- **WHEN** the Bearer token is missing or invalid
- **THEN** the system MUST reject with the standard auth failure envelope
- **AND** the system MUST NOT call Bailian tokens API

### Requirement: Temporary key TTL MUST be bounded

The system MUST request Bailian temporary keys with `expire_in_seconds` taken from configuration. The configured TTL MUST be clamped to the Bailian-allowed range \[1, 1800\]. The default configured TTL MUST be 1800 seconds unless overridden by environment.

#### Scenario: Default TTL uses Bailian maximum

- **WHEN** no TTL override is set and tokens issuance succeeds
- **THEN** the outbound Bailian request MUST use `expire_in_seconds=1800`

### Requirement: Session hints for client session.update

Successful `POST /api/bailian/realtime-tokens` MUST include session hint fields for the client to send in `session.update`: `instructions` (non-blank string) and `voice` (Bailian system voice name). In this change, both MUST come from configured defaults (`bailian.realtime.default-instructions` / `bailian.realtime.default-voice`) and MUST NOT be resolved from companion/dimension preference tables. The response MUST NOT include `greetingText`. The system MUST NOT map Tencent TTS `voice_type` into `voice`.

#### Scenario: Fixed instructions and voice from configuration

- **WHEN** Bailian tokens API succeeds for an authenticated caller
- **THEN** `data.instructions` MUST equal the configured default instructions
- **AND** `data.voice` MUST equal the configured Bailian default voice
- **AND** the response MUST NOT contain `greetingText`

### Requirement: Request body for realtime token is minimal

`POST /api/bailian/realtime-tokens` MUST NOT require a request body. The endpoint MUST NOT require ConvoAI join fields such as Agora agent parameters. The system MUST NOT start an Agora ConvoAI agent as part of this request.

#### Scenario: Token request does not join ConvoAI

- **WHEN** realtime token issuance succeeds
- **THEN** the system MUST NOT call Agora conversational-ai join
- **AND** the response MUST NOT include `agentId`
