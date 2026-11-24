## ADDED Requirements

### Requirement: Join is mutually exclusive per authenticated user while in progress

The system SHALL acquire a distributed lock via lock4j (Redisson executor) before executing `POST /api/convoai/agents` business logic. The lock key MUST be scoped to the authenticated JWT `sub` and a stable operation name for join. While one join for that user holds the lock, a concurrent or overlapping join for the same user MUST NOT execute the Agora join path.

#### Scenario: Concurrent join for same user is rejected

- **WHEN** an authenticated user has a join request still holding the lock
- **AND** the same user submits another join
- **THEN** the system SHALL respond with HTTP 409 and the standard `ApiResponse` failure envelope
- **AND** the system SHALL NOT invoke a second Agora join for that overlapping request

#### Scenario: Join without contention succeeds as today

- **WHEN** an authenticated user calls join and no lock is held for that user
- **THEN** the system SHALL execute join normally and return `ConvoaiJoinCredentials` in `ApiResponse`

### Requirement: Join lock uses Redisson watchdog (no finite expire)

The join `@Lock4j` MUST omit a positive `expire` so the Redisson executor receives leaseTime `-1` and watchdog renewal applies for the duration of the join method (`autoRelease` on completion). The system MUST NOT document `lock4j.expire` or `LOCK4J_EXPIRE_MS` as controlling the join lock. A finite annotation `expire` disables watchdog and MUST NOT be used on join.

#### Scenario: Join lock renews until the method completes

- **WHEN** join is annotated without `expire`
- **THEN** the lock SHALL be held until the method returns or the process dies (watchdog timeout)
- **AND** `LOCK4J_EXPIRE_MS` SHALL NOT change that lease behavior

### Requirement: Acquire timeout fails fast for duplicate clicks

The system SHALL use acquire timeout 0 by default for join locking so a second request does not wait in queue; it fails immediately with the lock-failure mapping (409).

#### Scenario: Duplicate click does not block waiting for the lock

- **WHEN** a second join arrives while the first holds the lock
- **THEN** the system SHALL NOT wait up to a multi-second acquire timeout before rejecting
- **AND** the client SHALL receive 409 promptly

### Requirement: Lock failure uses unified API envelope

Lock acquisition failure MUST surface as HTTP 409 with `ApiResponse` (`success=false`, `code=409`), not an unmapped framework error body.

#### Scenario: Lock failure response shape

- **WHEN** lock acquisition fails for join
- **THEN** the response body SHALL use the project `ApiResponse` failure shape
