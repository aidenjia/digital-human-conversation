## ADDED Requirements

### Requirement: API package uses a centralized path prefix

The system SHALL register a path prefix of `/api/v1` for handler types whose declaring class resides in the base package `com.jupiter.digital.human.api`, without requiring each controller to repeat that prefix in class-level `@RequestMapping`.

#### Scenario: Echo endpoint remains at the public path

- **WHEN** a client calls `POST /api/v1/echo` with a valid JSON body
- **THEN** the request is handled by the echo controller in the `api` package and returns HTTP 200

#### Scenario: Controllers outside the API package are not prefixed

- **WHEN** a client calls `GET /`
- **THEN** the response is HTTP 200 from the root controller and the path is not prefixed with `/api/v1`

---

### Requirement: Actuator endpoints are not hidden under the API prefix

The system SHALL expose Actuator web endpoints under their default base path (e.g. `/actuator/**`) without requiring the `/api/v1` prefix.

#### Scenario: Health without API prefix

- **WHEN** a client calls `GET /actuator/health`
- **THEN** the response is HTTP 200 and the path does not start with `/api/v1/actuator`

---

### Requirement: No broad Exception catch-all in global web advice

The global web exception advice SHALL NOT declare `@ExceptionHandler(Exception.class)` or `@ExceptionHandler(Throwable.class)`.

#### Scenario: Advice class structure

- **WHEN** inspecting the global exception advice type
- **THEN** it extends `ResponseEntityExceptionHandler` and does not contain `@ExceptionHandler` methods targeting `Exception` or `Throwable`

---

### Requirement: Bean validation errors on request bodies remain HTTP 400 with field details

The system SHALL map `MethodArgumentNotValidException` to HTTP 400 with a `ProblemDetail` body that includes a structured `errors` property describing field violations.

#### Scenario: Invalid JSON body for a validated endpoint

- **WHEN** a client posts invalid JSON to `/api/v1/echo` that fails `@Valid` constraints
- **THEN** the response status is 400 and the body is a Problem Detail that includes field-level violation information

---

### Requirement: Constraint violations on constrained parameters are HTTP 400

The system SHALL map `ConstraintViolationException` to HTTP 400 with a `ProblemDetail` body including violation paths and messages.

#### Scenario: Constraint violation on parameters

- **WHEN** a controller method violates declared constraint annotations and raises `ConstraintViolationException`
- **THEN** the response status is 400 with a Problem Detail describing the violations
