## MODIFIED Requirements

### Requirement: Server-generated session and room identifier

The system SHALL generate `sessionId` and `strRoomId` on the server as a 32-character hexadecimal string without hyphen separators (UUID without dashes), without client input.

#### Scenario: UUID format

- **WHEN** the server assigns a new room id
- **THEN** `strRoomId` matches `[0-9a-f]{32}` and has length 32
