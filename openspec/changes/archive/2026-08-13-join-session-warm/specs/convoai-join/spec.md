## ADDED Requirements

### Requirement: Trigger session warm after successful Agora join

When session warm is enabled, `POST /api/convoai/agents` MUST trigger the session-warm capability after Agora ConvoAI REST join succeeds and before returning credentials. The join HTTP request and success response shapes MUST remain unchanged. Warm outcome MUST NOT change join success/failure relative to Agora join result alone.

#### Scenario: Successful Agora join still returns credentials when warm fails

- **WHEN** Agora join succeeds and the subsequent session warm fails or times out
- **THEN** the API MUST still return HTTP success with `ConvoaiJoinCredentials`
- **AND** the response MUST NOT include warm-specific fields

#### Scenario: Disabled warm leaves join behavior otherwise unchanged

- **WHEN** session warm is disabled and Agora join succeeds
- **THEN** the API MUST return `ConvoaiJoinCredentials` without calling warm
