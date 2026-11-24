## ADDED Requirements

### Requirement: ConvoAI routes use request-scoped MDC

For paths under `/api/convoai/**`, the application SHALL establish request-scoped SLF4J MDC correlators (`deviceId`, `digitalHumanType`, `userId`, `channel`, `agentId` as applicable) via a servlet filter and service-layer enrichment, and SHALL clear MDC before the request thread is returned to the container pool.

#### Scenario: Structured logs include MDC for ConvoAI

- **WHEN** `logging.structured.format.console` is `logstash` and a ConvoAI join request is processed
- **THEN** log events emitted on the request thread MAY include `mdc.deviceId` (and other bound keys) in structured output

#### Scenario: MDC does not replace stdout policy

- **WHEN** ConvoAI MDC is enabled
- **THEN** application logs SHALL continue to be written to standard output only, consistent with existing application-logging requirements
