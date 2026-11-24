## ADDED Requirements

### Requirement: Prometheus metrics endpoint is exposed

The system SHALL expose Spring Boot Actuator Prometheus metrics at
`GET /actuator/prometheus` on the same HTTP server port as the business API
(default `8080`), without requiring an `/api` prefix. The web exposure include
list SHALL contain `prometheus` in addition to `health` and `info`, and SHALL
NOT use a wildcard that exposes all Actuator endpoints.

#### Scenario: Prometheus scrape path returns metrics text

- **WHEN** a client calls `GET /actuator/prometheus` on the application port
- **THEN** the response status is HTTP 200 and the body is Prometheus text
  exposition that includes JVM and/or HTTP server request metric series

#### Scenario: Actuator exposure remains a whitelist

- **WHEN** the application configuration for `management.endpoints.web.exposure.include` is reviewed
- **THEN** the configured values include `health`, `info`, and `prometheus`, and do not equal `*`

### Requirement: Common low-cardinality application metric tag

The system SHALL attach Micrometer common tags `application` (value =
Spring `spring.application.name`, e.g. `digital-human-conversation`) and
`env` (low-cardinality environment name such as `local`, `dev`, `test`, or
`prod`). The `env` tag SHALL default per Spring profile and MAY be overridden
by the `METRICS_ENV` environment variable. Session-scoped identifiers such as
`deviceId`, `channel`, `userId`, or `agentId` MUST NOT be used as Prometheus
metric label names or values in this capability's configuration.

#### Scenario: Application and env tags present on exported series

- **WHEN** a client retrieves `GET /actuator/prometheus`
- **THEN** exported series include the label `application` with the configured
  application name and the label `env` with the active environment tag

### Requirement: HTTP server request latency histograms are enabled

The system SHALL enable Micrometer percentile histograms for
`http.server.requests` so that Prometheus can scrape histogram buckets suitable
for latency dashboards.

#### Scenario: HTTP request histogram series are present

- **WHEN** the application has handled at least one HTTP request and a client
  calls `GET /actuator/prometheus`
- **THEN** the body includes histogram-related series for HTTP server requests
  (for example `http_server_requests_*_bucket` or equivalent Micrometer naming)

### Requirement: ServiceMonitor example and operator documentation

The repository SHALL include an example Kubernetes `ServiceMonitor` manifest
under `deploy/k8s/` whose filename indicates it is an example (e.g.
`servicemonitor.example.yaml`), targeting path `/actuator/prometheus` on the
application HTTP port. `README.md` SHALL document the scrape path, same-port
convention, and that public Ingress MUST NOT expose `/actuator/**` for metrics
scraping (cluster-internal scrape is the intended path).

#### Scenario: Example ServiceMonitor references prometheus path

- **WHEN** a reviewer opens the example ServiceMonitor file in `deploy/k8s/`
- **THEN** an endpoint path of `/actuator/prometheus` is specified (or clearly
  documented in comments adjacent to the endpoint)

#### Scenario: README documents scrape and exposure boundary

- **WHEN** an operator reads the monitoring section of `README.md`
- **THEN** the document states the Prometheus path, that metrics share the
  business port, and that `/actuator` metrics MUST NOT be published on the
  public Ingress
