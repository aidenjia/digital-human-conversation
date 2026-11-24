# API routing policy

## Requirements

### Requirement: Explicit unversioned business paths

Business controllers SHALL explicitly map `/api/...`; they SHALL NOT use servlet context-path, package prefix injection, or URL version segments. Retained mappings SHALL match `voice-api-surface`. All current paths are implicit v1 and no production path is registered as multi-version.

#### Scenario: Authenticated SDP exchange

- **WHEN** an authenticated client sends a valid request to `POST /api/bailian/realtime/webrtc`
- **THEN** the signaling controller handles it without a version prefix

### Requirement: Infrastructure paths remain independent

Root GET and enabled Actuator endpoints SHALL NOT require an `/api` prefix.

#### Scenario: Health probe

- **WHEN** a client requests `/actuator/health`
- **THEN** the health endpoint remains reachable under its infrastructure path

### Requirement: Unified structured errors

Global web advice SHALL extend `ResponseEntityExceptionHandler` without broad `Exception`/`Throwable` handlers. Business errors SHALL use `ApiResponse` with matching HTTP status and code. Validation failures SHALL use HTTP 400 with field violations in `data.errors`.

#### Scenario: Invalid validated join body

- **WHEN** an authenticated client posts an invalid join body
- **THEN** HTTP 400 and the validation envelope are returned before an upstream join

#### Scenario: Upstream or configuration failure

- **WHEN** a retained business service raises an upstream or configuration exception
- **THEN** advice maps it to the existing API envelope and appropriate status without exposing credentials
