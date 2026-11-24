# convoai-join Specification

## Purpose

HTTP gateway for Agora ConvoAI join (`POST /api/convoai/agents`): create agent instance, return RTC credentials, and forward client context in `llm.params` for the downstream digital-human-agent LLM callback.

## Requirements

### Requirement: Config-only session profile for every digital human type

All supported `digitalHumanType` values SHALL use the configured default session profile without relational persistence or user catalog/config lookups. The type SHALL still be forwarded to the downstream agent. Existing default greeting, persona, Tencent TTS settings, voice rate/volume and reply-language behavior SHALL be preserved.

#### Scenario: UE5, MMD and Unity use configured defaults

- **WHEN** an authenticated client joins with any supported digital human type
- **THEN** the outgoing join body SHALL be assembled from application configuration
- **AND** the client SHALL NOT need to create a user configuration first

### Requirement: Optional timezone on ConvoAI join

The system SHALL accept an optional `timezone` field on `POST /api/convoai/agents` as an IANA time zone ID string. The field SHALL be optional and MAY be omitted or null.

#### Scenario: Omit timezone defaults to passthrough off

- **WHEN** the client sends join without `timezone` or with null/blank
- **THEN** the outbound Agora `llm.params` SHALL NOT contain a `timezone` key

#### Scenario: Valid timezone forwarded to agent params

- **WHEN** the client sends `"timezone": "America/New_York"` with required join fields
- **THEN** `ConvoaiService` SHALL include `timezone=America/New_York` in `llm.params` passed to Agora ConvoAI

#### Scenario: No IANA validation in conversation

- **WHEN** the client sends a non-parseable timezone string
- **THEN** conversation SHALL forward the string as-is and SHALL NOT return HTTP 400 for invalid timezone alone

### Requirement: Trigger session warm after successful Agora join

When session warm is enabled, `POST /api/convoai/agents` MUST trigger the session-warm capability after Agora ConvoAI REST join succeeds and before returning credentials. The join HTTP request and success response shapes MUST remain unchanged. Warm outcome MUST NOT change join success/failure relative to Agora join result alone.

#### Scenario: Successful Agora join still returns credentials when warm fails

- **WHEN** Agora join succeeds and the subsequent session warm fails or times out
- **THEN** the API MUST still return HTTP success with `ConvoaiJoinCredentials`
- **AND** the response MUST NOT include warm-specific fields

#### Scenario: Disabled warm leaves join behavior otherwise unchanged

- **WHEN** session warm is disabled and Agora join succeeds
- **THEN** the API MUST return `ConvoaiJoinCredentials` without calling warm

### Requirement: Join is mutually exclusive per authenticated user while in progress

The system SHALL acquire a process-local exclusion guard before executing `POST /api/convoai/agents` business logic. The guard MUST be scoped to the authenticated JWT `sub`. While one join for that user holds the lock, a concurrent or overlapping join for the same user MUST NOT execute the Agora join path.

#### Scenario: Concurrent join for same user is rejected

- **WHEN** an authenticated user has a join request still holding the lock
- **AND** the same user submits another join
- **THEN** the system SHALL respond with HTTP 409 and the standard `ApiResponse` failure envelope
- **AND** the system SHALL NOT invoke a second Agora join for that overlapping request

### Requirement: Join exclusion lasts until completion

The process-local guard SHALL hold the user while the join request runs, without a finite lease. A finally block SHALL release the user on success and on any exception. The guard SHALL NOT retain user keys after completion.

#### Scenario: Failed join permits retry

- **WHEN** join fails upstream
- **THEN** the user's next request SHALL be able to acquire the guard

### Requirement: Acquire timeout fails fast for duplicate clicks

The system SHALL use acquire timeout 0 by default for join locking so a second request does not wait in queue; it fails immediately with the standard API conflict envelope (409).

#### Scenario: Duplicate click does not block waiting for the lock

- **WHEN** a second join arrives while the first holds the lock
- **THEN** the system SHALL NOT wait up to a multi-second acquire timeout before rejecting
- **AND** the client SHALL receive 409 promptly

### Requirement: Lock failure uses unified API envelope

Lock acquisition failure MUST surface as HTTP 409 with `ApiResponse` (`success=false`, `code=409`), not an unmapped framework error body.

#### Scenario: Lock failure response shape

- **WHEN** lock acquisition fails for join
- **THEN** the response body SHALL use the project `ApiResponse` failure shape
