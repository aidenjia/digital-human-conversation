# api-error-envelope Specification

## Purpose
Unified error envelope for the retained conversation APIs.
## Requirements
### Requirement: API error responses use ApiResponse envelope

For REST endpoints under `/api/**`, the system SHALL return error responses as JSON `ApiResponse` objects including `code`, `success=false`, `message`, `data` and `error`.

#### Scenario: Error envelope shape

- **WHEN** a client calls `/api/**` and the request fails with a handled application error
- **THEN** the response body is JSON containing integer `code`, string `message`, and `data` (which MAY be `null`)

### Requirement: Non-zero code indicates failure

On error responses, the system SHALL set `code` to a non-zero integer. The system SHALL NOT use `code == 0` for failure responses.

#### Scenario: Failure code is non-zero

- **WHEN** an error `ApiResponse` is returned for `/api/**`
- **THEN** `code` is not equal to `0`

### Requirement: HTTP status and ApiResponse code alignment

对于已处理的 `/api/**` 错误，系统 SHALL 返回统一 `ApiResponse` 信封。默认情况下，常见错误（如校验 400、上游 502、配置 503）的 HTTP 状态码 SHOULD 与 `ApiResponse.code` 相同。

当能力显式定义**独立业务码**（例如鉴权的 4010 / 4030）时，系统 MUST 允许 `ApiResponse.code` 与 HTTP 状态码分离：HTTP 使用标准状态（如 401 / 403），业务语义以响应体 `code` 为准。系统 MUST NOT 将非标准 HTTP 数值（如四位业务码）直接用作 HTTP 状态码。

#### Scenario: Validation error keeps aligned codes

- **WHEN** 请求体校验失败且无独立业务码
- **THEN** HTTP status is 400 and `code` is 400

#### Scenario: Auth invalid token keeps separated codes

- **WHEN** Bearer token 无效
- **THEN** HTTP status is 401 and `code` is 4010

### Requirement: Validation errors include structured data

For `MethodArgumentNotValidException` and `ConstraintViolationException`, the system SHALL include field-level violations under `data.errors` as a map of field or property path to message.

#### Scenario: Invalid join body

- **WHEN** an authenticated client posts `{}` to `POST /api/convoai/agents`
- **THEN** HTTP status is 400, `code` is 400, `message` describes validation failure, and `data.errors` contains at least one field entry

### Requirement: API authentication uses ApiResponse

The API security handlers SHALL write HTTP 401 responses as `ApiResponse` JSON (not `ProblemDetail` or hand-built problem+json strings) for paths under `/api/**`.

#### Scenario: Missing auth header

- **WHEN** a client calls `POST /api/convoai/agents` without a valid Bearer token and with demo bypass disabled
- **THEN** HTTP status is 401, `code` is 401, and `message` describes the authentication failure

### Requirement: ApiResponse provides failure factory methods

The `ApiResponse` type SHALL expose static factory methods for failures that set non-zero `code`, the provided `message`, and optional `data`.

#### Scenario: Failure factory

- **WHEN** application code constructs `ApiResponse.fail(502, "Upstream error")`
- **THEN** the resulting object has `code` 502, the given `message`, and `data` null unless a data overload is used

