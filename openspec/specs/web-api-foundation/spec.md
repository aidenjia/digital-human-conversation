## Purpose

定义本服务作为 Spring Boot HTTP API 时的基线行为：字符编码与停机、JSON 与错误模型、Bean Validation、根路径探活，便于环境与监控对齐。
## Requirements
### Requirement: API baseline uses UTF-8 HTTP encoding

The system SHALL configure servlet HTTP encoding to UTF-8 for requests and responses so that JSON and form data use a consistent charset.

#### Scenario: Default encoding is UTF-8

- **WHEN** the application starts with default configuration
- **THEN** `server.servlet.encoding.charset` is set to `UTF-8` and `server.servlet.encoding.force` is enabled

### Requirement: Graceful shutdown and optional HTTP compression

The system SHALL enable graceful shutdown for the embedded web server and MAY enable HTTP response compression for compatible clients.

#### Scenario: Graceful shutdown is enabled

- **WHEN** the application receives a shutdown signal
- **THEN** the server completes in-flight requests according to `server.shutdown=graceful` before terminating

#### Scenario: Compression is enabled

- **WHEN** a client accepts gzip encoding and the response is compressible
- **THEN** `server.compression.enabled=true` allows the server to compress the response

### Requirement: JSON timestamps use a configured time zone

The system SHALL set Jackson default time zone via Spring configuration so that serialized date-time values are consistent across environments.

#### Scenario: Jackson time zone is set

- **WHEN** the application serializes Java date/time types to JSON
- **THEN** `spring.jackson.time-zone` is applied (default `UTC` unless overridden by profile)

### Requirement: RFC 9457 Problem Details are enabled for Spring MVC

The system SHALL enable Spring MVC Problem Details support so APIs can expose standardized error bodies where applicable.

#### Scenario: Problem details switch is on

- **WHEN** the application starts
- **THEN** `spring.mvc.problemdetails.enabled` is `true`

### Requirement: Bean Validation is on the classpath

The system SHALL include Jakarta Bean Validation on the classpath for validating request DTOs and method parameters.

#### Scenario: Validation starter present

- **WHEN** the application is built
- **THEN** `spring-boot-starter-validation` is declared as a dependency

### Requirement: Validation errors return HTTP 400 with structured body

The system SHALL map `MethodArgumentNotValidException` and `ConstraintViolationException` to HTTP 400 with a structured response body that describes field or constraint violations.

For endpoints under `/api/**`, the body SHALL be an `ApiResponse` envelope where `code` is 400, `message` summarizes the failure, and `data.errors` contains field or property paths mapped to violation messages.

#### Scenario: Invalid request body fields

- **WHEN** a client submits a JSON body that fails `@Valid` field constraints to `/api/**`
- **THEN** the response status is 400, `code` is 400, and `data.errors` contains violation details

#### Scenario: Invalid constrained parameters

- **WHEN** a client calls an endpoint under `/api/**` with parameters that violate validation annotations
- **THEN** the response status is 400, `code` is 400, and `data.errors` describes the constraint violations

### Requirement: Root GET provides service identity

The system SHALL expose `GET /` returning a small JSON document including the application name and a version string for human and load-balancer checks.

#### Scenario: Root probe

- **WHEN** a client performs `GET /`
- **THEN** the response is 200 JSON containing at least `name` and `version` keys derived from application configuration or build metadata

### Requirement: Business API errors prefer ApiResponse over ProblemDetail

For REST controllers under `/api/**`, application-handled errors SHALL use the `ApiResponse` JSON envelope as the primary client contract. Problem Details MAY remain enabled globally for unhandled framework paths outside this contract.

#### Scenario: Handled ConvoAI configuration error

- **WHEN** ConvoAI configuration is incomplete and a client calls `POST /api/convoai/agents` with valid authentication
- **THEN** the response body is `ApiResponse` with `code` 503, not a RFC 9457 `ProblemDetail` root object with `title` and `status` fields only

