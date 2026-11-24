## MODIFIED Requirements

### Requirement: Validation errors return HTTP 400 with structured body

The system SHALL map `MethodArgumentNotValidException` and `ConstraintViolationException` to HTTP 400 with a structured response body that describes field or constraint violations.

For endpoints under `/api/v1/**`, the body SHALL be an `ApiResponse` envelope where `code` is 400, `message` summarizes the failure, and `data.errors` contains field or property paths mapped to violation messages.

#### Scenario: Invalid request body fields

- **WHEN** a client submits a JSON body that fails `@Valid` field constraints to `/api/v1/**`
- **THEN** the response status is 400, `code` is 400, and `data.errors` contains violation details

#### Scenario: Invalid constrained parameters

- **WHEN** a client calls an endpoint under `/api/v1/**` with parameters that violate validation annotations
- **THEN** the response status is 400, `code` is 400, and `data.errors` describes the constraint violations

## ADDED Requirements

### Requirement: Business API errors prefer ApiResponse over ProblemDetail

For REST controllers under `/api/v1/**`, application-handled errors SHALL use the `ApiResponse` JSON envelope as the primary client contract. Problem Details MAY remain enabled globally for unhandled framework paths outside this contract.

#### Scenario: Handled TRTC configuration error

- **WHEN** TRTC configuration is incomplete and a client calls `/api/v1/trtc/ai-conversations/start` with valid authentication
- **THEN** the response body is `ApiResponse` with `code` 503, not a RFC 9457 `ProblemDetail` root object with `title` and `status` fields only
