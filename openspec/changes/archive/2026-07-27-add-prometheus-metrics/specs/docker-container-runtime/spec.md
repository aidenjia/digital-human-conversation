## ADDED Requirements

### Requirement: Prometheus metrics share the application HTTP port

The running container SHALL expose Prometheus metrics on the same configured
HTTP port as the business API and health probes (default `8080`). The system
SHALL NOT require a separate management port for `/actuator/prometheus` in the
default runtime configuration covered by this capability.

#### Scenario: Metrics reachable on the published container port

- **WHEN** the container listens on the published application port (default 8080)
  and Actuator exposes `prometheus`
- **THEN** `GET /actuator/prometheus` on that port succeeds without requiring a
  second published management port
