## MODIFIED Requirements

### Requirement: Server-generated session and room identifier

The system SHALL generate `sessionId` and `strRoomId` on the server using the current millisecond timestamp concatenated with an unsigned random long from `ThreadLocalRandom`, without client input.

#### Scenario: Unique high-throughput generation

- **WHEN** the server generates 10,000 room identifiers within one second
- **THEN** all identifiers SHALL be unique
