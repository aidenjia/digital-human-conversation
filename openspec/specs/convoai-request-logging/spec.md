# convoai-request-logging Specification

## Purpose

为 `/api/convoai/**` 请求建立 SLF4J MDC 关联字段，使 Logstash 结构化日志可通过 `mdc.deviceId` 等键检索 join/leave 全链路。

## Requirements

### Requirement: ConvoAI MDC keys

The system SHALL write the following SLF4J MDC keys for `/api/convoai/**` requests when the corresponding values are known: `deviceId`, `digitalHumanType`, `userId`, `channel`, `agentId`. Keys SHALL be omitted when the value is null or blank.

#### Scenario: Join request exposes device context in MDC

- **WHEN** an authenticated client sends `POST /api/convoai/agents` with a valid JSON body containing `deviceId` and `digitalHumanType`
- **THEN** before the controller returns, MDC SHALL contain `deviceId` and `digitalHumanType` for the request thread

#### Scenario: Join success enriches channel and userId

- **WHEN** `ConvoaiService.join` generates `channel` and `userId`
- **THEN** MDC SHALL be updated to include `channel` and `userId` for the remainder of that request

#### Scenario: Join success records agentId

- **WHEN** the upstream join call returns a non-blank `agentId`
- **THEN** MDC SHALL contain `agentId` before the HTTP response is written

#### Scenario: Leave request exposes agentId in MDC

- **WHEN** an authenticated client sends `DELETE /api/convoai/agents` with a valid JSON body containing `agentId`
- **THEN** MDC SHALL contain `agentId` for the request thread during leave processing

### Requirement: ConvoAI MDC filter scope and ordering

The system SHALL register a servlet filter that applies only to paths under `/api/convoai/**` and SHALL execute after authentication filters in the filter chain. The filter SHALL skip `OPTIONS` requests without modifying MDC.

#### Scenario: Non-ConvoAI routes unaffected

- **WHEN** a client calls `POST /api/bailian/realtime/webrtc`
- **THEN** the ConvoAI MDC filter SHALL NOT run and SHALL NOT set ConvoAI MDC keys

#### Scenario: Authenticated ConvoAI request passes through MDC filter

- **WHEN** a client calls `POST /api/convoai/agents` with a valid Bearer token
- **THEN** the ConvoAI MDC filter SHALL run after authentication succeeds

### Requirement: ConvoAI MDC cleanup

The system SHALL clear all SLF4J MDC entries for the request thread in a `finally` block after `/api/convoai/**` processing completes, including error responses.

#### Scenario: MDC cleared after successful join

- **WHEN** `POST /api/convoai/agents` completes with HTTP 200
- **THEN** the request thread SHALL have no ConvoAI MDC keys remaining

#### Scenario: MDC cleared after validation failure

- **WHEN** `POST /api/convoai/agents` completes with HTTP 400
- **THEN** the request thread SHALL have no ConvoAI MDC keys remaining

### Requirement: ConvoAI MDC must not log secrets

The ConvoAI MDC filter and `ConvoaiLogContext` SHALL NOT write Authorization headers, Bearer tokens, RTC tokens, LLM API keys, or full request bodies to logs or MDC.

#### Scenario: Join body secrets stay out of MDC

- **WHEN** the MDC filter parses a join request body
- **THEN** MDC SHALL NOT contain keys named `token`, `api_key`, `apiKey`, or `Authorization`
