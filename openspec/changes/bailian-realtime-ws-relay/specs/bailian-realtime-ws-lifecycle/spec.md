## ADDED Requirements

### Requirement: Application-level heartbeat on client relay socket

The system MUST run an application-level heartbeat on each client relay WebSocket. Heartbeat interval and idle timeout MUST be configurable. If the idle timeout elapses without a successful heartbeat exchange, the system MUST close the client socket and MUST tear down the upstream Bailian connection for that session.

#### Scenario: Idle timeout closes both legs

- **WHEN** a relay session exceeds the configured idle timeout without heartbeat
- **THEN** the system MUST close the client WebSocket
- **AND** MUST close the upstream Bailian WebSocket for that session

### Requirement: Upstream Bailian connectivity failure ends the session

If the upstream Bailian WebSocket fails or times out, the system MUST close the client WebSocket with an error or terminal event indicating upstream failure, and MUST NOT leave an orphaned client session open indefinitely.

#### Scenario: Upstream drop notifies client

- **WHEN** the Bailian upstream connection closes unexpectedly during an active relay
- **THEN** the system MUST terminate the client relay session
- **AND** MUST emit a client-visible error or close reason suitable for reconnect UX

### Requirement: Graceful drain on process shutdown

During application shutdown or when the instance is marked not-ready for drain, the system MUST stop accepting new relay connections, MUST notify active clients with a draining signal, MUST close client sockets, and MUST close upstream Bailian sockets within a configured drain budget. Reconnect after drain MUST start a new session; the system MUST NOT promise cross-pod session resume.

#### Scenario: Active clients receive draining before force close

- **WHEN** the instance begins graceful shutdown while relay sessions are active
- **THEN** the system MUST send a draining notification to those clients
- **AND** MUST close sessions within the configured drain timeout
- **AND** MUST update operational docs so termination grace period covers the drain budget

#### Scenario: New connections rejected during drain

- **WHEN** drain has started
- **THEN** the system MUST NOT establish new relay sessions on that instance
