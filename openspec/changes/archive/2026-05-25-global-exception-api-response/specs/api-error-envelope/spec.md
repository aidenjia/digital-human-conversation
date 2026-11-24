## ADDED Requirements

### Requirement: API error responses use ApiResponse envelope

For REST endpoints under `/api/v1/**`, the system SHALL return error responses as JSON `ApiResponse` objects with the same top-level fields as successful responses: `code`, `message`, and `data`.

#### Scenario: Error envelope shape

- **WHEN** a client calls `/api/v1/**` and the request fails with a handled application error
- **THEN** the response body is JSON containing integer `code`, string `message`, and `data` (which MAY be `null`)

### Requirement: Non-zero code indicates failure

On error responses, the system SHALL set `code` to a non-zero integer. The system SHALL NOT use `code == 0` for failure responses.

#### Scenario: Failure code is non-zero

- **WHEN** an error `ApiResponse` is returned for `/api/v1/**`
- **THEN** `code` is not equal to `0`

### Requirement: HTTP status and ApiResponse code alignment

For handled errors in this change, the system SHALL set the HTTP response status code to match the `ApiResponse.code` value for: 400 (validation), 401 (authentication), 502 (TRTC upstream), and 503 (TRTC configuration).

#### Scenario: Validation failure status

- **WHEN** request body validation fails on `/api/v1/echo`
- **THEN** HTTP status is 400 and `code` is 400

#### Scenario: TRTC upstream failure status

- **WHEN** a TRTC cloud API call fails and `TrtcUpstreamException` is thrown
- **THEN** HTTP status is 502 and `code` is 502

#### Scenario: TRTC configuration failure status

- **WHEN** required TRTC configuration is missing and `TrtcConfigurationException` is thrown
- **THEN** HTTP status is 503 and `code` is 503

### Requirement: Validation errors include structured data

For `MethodArgumentNotValidException` and `ConstraintViolationException`, the system SHALL include field-level violations under `data.errors` as a map of field or property path to message.

#### Scenario: Invalid echo body

- **WHEN** a client posts `{}` to `POST /api/v1/echo`
- **THEN** HTTP status is 400, `code` is 400, `message` describes validation failure, and `data.errors` contains at least one field entry

### Requirement: TRTC authentication filter uses ApiResponse

The TRTC authentication filter SHALL write HTTP 401 responses as `ApiResponse` JSON (not `ProblemDetail` or hand-built problem+json strings) for paths under `/api/v1/trtc/**`.

#### Scenario: Missing auth header

- **WHEN** a client calls `/api/v1/trtc/ai-conversations/start` without the configured auth header
- **THEN** HTTP status is 401, `code` is 401, and `message` describes the authentication failure

### Requirement: ApiResponse provides failure factory methods

The `ApiResponse` type SHALL expose static factory methods for failures that set non-zero `code`, the provided `message`, and optional `data`.

#### Scenario: Failure factory

- **WHEN** application code constructs `ApiResponse.fail(502, "TRTC upstream error")`
- **THEN** the resulting object has `code` 502, the given `message`, and `data` null unless a data overload is used
