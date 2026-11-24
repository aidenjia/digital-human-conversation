# convoai-leave Specification

## Purpose

`DELETE /api/convoai/agents`（leave）停止 Agora ConvoAI 智能体。除 JWT 认证外，MUST 校验该 `agentId` 由当前用户在 join 时创建。

## ADDED Requirements

### Requirement: Leave MUST verify agent ownership

Before invoking Agora leave, the system MUST resolve the authenticated JWT `sub` and MUST verify that the request `agentId` is bound to that user in the ownership store. The system MUST NOT call Agora leave when ownership cannot be verified.

#### Scenario: Owner leave succeeds

- **WHEN** an authenticated user leaves an `agentId` that was bound to their `sub` at join time
- **THEN** the system MUST call Agora leave
- **AND** MUST return HTTP success with `LeaveConvoaiResult` (`status=STOPPED`)
- **AND** MUST remove the ownership binding after a successful Agora leave

#### Scenario: Other user leave is denied

- **WHEN** an authenticated user leaves an `agentId` bound to a different `sub`
- **THEN** the system MUST respond with HTTP 404 and the standard `ApiResponse` failure envelope
- **AND** the system MUST NOT invoke Agora leave

#### Scenario: Unknown agentId is denied

- **WHEN** an authenticated user leaves an `agentId` with no ownership binding
- **THEN** the system MUST respond with HTTP 404 and the standard `ApiResponse` failure envelope
- **AND** the system MUST NOT invoke Agora leave

### Requirement: Join MUST bind agent ownership after Agora success

After a successful Agora ConvoAI REST join, the system MUST record a binding from the returned `agentId` to the authenticated JWT `sub` in the ownership store with a configurable TTL that MUST be at least the configured ConvoAI idle timeout in intent (default MAY be one day). Failure to persist the binding MUST NOT change the join HTTP success response shape or prevent returning `ConvoaiJoinCredentials`.

#### Scenario: Successful join still returns credentials when ownership bind fails

- **WHEN** Agora join succeeds and the subsequent ownership bind fails
- **THEN** the API MUST still return HTTP success with `ConvoaiJoinCredentials`
- **AND** a later leave for that `agentId` MAY be denied with 404 until idle timeout clears the upstream agent

### Requirement: Ownership store is shared across replicas

The ownership binding MUST be stored in Redis via the existing Redisson client and key NameMapper prefix so that any replica can authorize leave. When Redisson is unavailable, leave MUST deny (404) rather than fail-open.

#### Scenario: Leave denies when ownership store cannot verify

- **WHEN** the ownership store cannot confirm ownership (missing binding, mismatch, or read failure / NoOp)
- **THEN** leave MUST return HTTP 404 without calling Agora
