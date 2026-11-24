## ADDED Requirements

### Requirement: Issue RTC credentials without ConvoAI agent

The system MUST expose `POST /api/rtc/credentials` authenticated with the same Users OpenID Bearer rules as other `/api/**` endpoints. On success the system MUST return `ApiResponse` whose `data` contains `appId`, `channel`, `token` (null when RTC token auth is disabled), and `userId` equal to JWT `sub`. The response MUST NOT include `agentId`. The system MUST NOT call Agora ConvoAI join, MUST NOT require LLM/TTS configuration, and MUST NOT write ConvoAI agent ownership keys.

#### Scenario: Authenticated caller receives channel credentials

- **WHEN** a client with a valid Bearer token calls `POST /api/rtc/credentials`
- **THEN** the system MUST return HTTP 200 with `success=true` and `data` containing `appId`, a newly generated `channel`, `userId` equal to JWT `sub`, and `token` per RTC certificate configuration
- **AND** the system MUST NOT invoke Agora conversational-ai join

#### Scenario: Missing bearer is rejected

- **WHEN** a client calls `POST /api/rtc/credentials` without `Authorization: Bearer`
- **THEN** the system MUST reject the request with the project's standard unauthenticated envelope
- **AND** the system MUST NOT generate a channel or RTC token

#### Scenario: Invalid bearer is rejected

- **WHEN** a client calls `POST /api/rtc/credentials` with an invalid or expired Bearer token
- **THEN** the system MUST reject the request with the project's standard authentication-failure envelope
- **AND** the system MUST NOT generate a channel or RTC token

### Requirement: RTC credentials endpoint MUST stay thin

`POST /api/rtc/credentials` MUST accept an empty body or omit business fields required by ConvoAI join (`deviceId`, `digitalHumanType` MUST NOT be required). The system MAY ignore unknown JSON fields. The system MUST generate `channel` via the existing room-id generator and MUST build the user token via the existing Agora RTC token service.

#### Scenario: Call without ConvoAI join fields succeeds

- **WHEN** an authenticated client posts `{}` or no body fields used by ConvoAI join
- **THEN** the system MUST still return RTC credentials as specified
- **AND** the system MUST NOT fail due to missing `deviceId` or `digitalHumanType`
