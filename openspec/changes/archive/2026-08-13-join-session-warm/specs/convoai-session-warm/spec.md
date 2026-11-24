## ADDED Requirements

### Requirement: Synchronous session warm after Agora join success

After a successful Agora ConvoAI REST join, when session warm is enabled, the system MUST synchronously call digital-human-agent `POST /v1/sessions/warm` on the join request thread before returning RTC credentials to the client. The warm request MUST use the same session identity fields already used in outbound Agora `llm.params`: `clientUserId`, `conversationId` (channel), `deviceId`, `digitalHumanType`, and optional `timezone` when present. The warm call MUST authenticate with the configured ConvoAI LLM API key as `Authorization: Bearer`. The HTTP client timeout MUST default to 5000 milliseconds and MUST be configurable. Warm MUST NOT run when Agora join fails.

#### Scenario: Warm invoked only after Agora join succeeds

- **WHEN** Agora REST join succeeds and session warm is enabled with a resolvable warm URL and API key
- **THEN** the system MUST issue `POST /v1/sessions/warm` before returning `ConvoaiJoinCredentials`
- **AND** the warm body MUST use `conversationId` equal to the join channel and `clientUserId` equal to the join RTC user id

#### Scenario: Warm skipped when Agora join fails

- **WHEN** Agora REST join fails
- **THEN** the system MUST NOT call session warm

#### Scenario: Warm skipped when disabled

- **WHEN** session warm is disabled via configuration
- **THEN** the system MUST NOT call session warm
- **AND** join MUST still return RTC credentials after Agora success

### Requirement: Best-effort warm must not fail join

Session warm failure, non-success agent response (`code != 0`), HTTP error, missing URL/key, or client timeout MUST NOT cause join to fail. The system MUST still return the same `ConvoaiJoinCredentials` shape as a successful join without warm. The system MUST NOT add warm-specific fields to the join HTTP response. The system MUST emit a WARN log for warm failure/timeout without logging the API key or Bearer token.

#### Scenario: Agent warm returns failure code

- **WHEN** agent warm responds with a non-zero `code` (for example 503)
- **THEN** join MUST still succeed with RTC credentials
- **AND** the system MUST log a WARN for the warm failure

#### Scenario: Warm HTTP timeout

- **WHEN** the warm HTTP call exceeds the configured timeout
- **THEN** join MUST still succeed with RTC credentials
- **AND** the system MUST log a WARN for the warm timeout

#### Scenario: Join response contract unchanged

- **WHEN** warm succeeds or fails
- **THEN** the join HTTP `data` payload MUST remain `appId` / `channel` / `token` / `userId` / `agentId` only
