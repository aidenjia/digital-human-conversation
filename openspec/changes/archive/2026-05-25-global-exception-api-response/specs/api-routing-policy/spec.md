## MODIFIED Requirements

### Requirement: Bean validation errors on request bodies return HTTP 400 with ProblemDetail and errors property

The system SHALL map `MethodArgumentNotValidException` to HTTP 400 with an `ApiResponse` body where `code` is 400, `message` summarizes validation failure, and `data.errors` describes field violations.

#### Scenario: Invalid JSON body for a validated endpoint

- **WHEN** a client posts invalid JSON to `/api/v1/echo` that fails `@Valid` constraints
- **THEN** the response status is 400 and the body is `ApiResponse` with `data.errors` containing field-level violation information

### Requirement: Constraint violations on constrained parameters are HTTP 400 with ProblemDetail

The system SHALL map `ConstraintViolationException` to HTTP 400 with an `ApiResponse` body where `code` is 400, `message` summarizes the failure, and `data.errors` describes violation paths and messages.

#### Scenario: Constraint violation on parameters

- **WHEN** a controller method violates declared constraint annotations and raises `ConstraintViolationException`
- **THEN** the response status is 400 with `ApiResponse` describing the violations in `data.errors`

## ADDED Requirements

### Requirement: Global exception advice returns ApiResponse for domain errors

The global web exception advice SHALL map `TrtcConfigurationException` and `TrtcUpstreamException` (or a shared `ApiException` base type) to `ResponseEntity` bodies of type `ApiResponse` with appropriate HTTP status and matching `code`.

#### Scenario: TRTC upstream handler response type

- **WHEN** `TrtcUpstreamException` is raised from a `/api/v1/trtc/**` controller
- **THEN** the advice returns HTTP 502 with JSON `ApiResponse` where `code` is 502

#### Scenario: TRTC configuration handler response type

- **WHEN** `TrtcConfigurationException` is raised from a `/api/v1/trtc/**` controller
- **THEN** the advice returns HTTP 503 with JSON `ApiResponse` where `code` is 503
