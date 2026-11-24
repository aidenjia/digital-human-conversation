## MODIFIED Requirements

### Requirement: Business API under /api/v1 with explicit mapping

The system SHALL expose REST controllers in `com.jupiter.digital.human.api` under the path prefix `/api/v1` using explicit Spring MVC class-level mappings (e.g. `@RequestMapping("/api/v1")` on `@RestController` types in that package). The system SHALL NOT use `PathMatchConfigurer#addPathPrefix` registered solely by base package to attach `/api/v1`.

#### Scenario: Echo endpoint remains at the public path

- **WHEN** a client calls `POST /api/v1/echo` with a valid JSON body
- **THEN** the request is handled by the echo controller in the `api` package and returns HTTP 200

#### Scenario: Controllers outside the API package are not prefixed

- **WHEN** a client calls `GET /`
- **THEN** the response is HTTP 200 from the root controller and the path is not prefixed with `/api/v1`
