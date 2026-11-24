## ADDED Requirements

### Requirement: Generate motion assets asynchronously
The system SHALL accept an authenticated user's prompt of 1–128 characters and a duration of 1–12 seconds, submit HY-Motion-1.0 generation, and return a project task ID in the common API envelope.

#### Scenario: Valid submission
- **WHEN** an authenticated user submits valid input while the feature and credentials are configured
- **THEN** the service submits a Tencent job and persists its project ID, owner and upstream ID

#### Scenario: Disabled or invalid submission
- **WHEN** the feature is disabled or input is invalid
- **THEN** no cloud task is submitted

### Requirement: Isolate task access
The system SHALL require the current JWT subject to own a task before querying or downloading it.

#### Scenario: Another user requests a task
- **WHEN** a different authenticated user supplies an existing task ID
- **THEN** the service responds with 404 without calling the upstream provider

### Requirement: Retrieve and retain generated FBX
The system SHALL map provider states WAIT, RUN, FAIL and DONE, permit downloads only for completed tasks, and cache downloads in configurable persistent storage.

#### Scenario: Successful download
- **WHEN** the owner downloads a completed task
- **THEN** the system saves its FBX and serves it as an authenticated attachment

#### Scenario: Repeated download
- **WHEN** an owner downloads an already saved asset after provider expiry
- **THEN** the system serves the saved FBX without calling Tencent

#### Scenario: Untrusted or oversized download
- **WHEN** a provider URL is outside the allowed HTTPS COS hosts or its response exceeds the configured size
- **THEN** the system rejects the download without keeping an incomplete FBX

### Requirement: Provide an asset-production UI
The realtime page SHALL provide an authenticated form to generate, query and download motion assets before connecting a conversation.

#### Scenario: Resume a task
- **WHEN** the owner enters a saved project task ID
- **THEN** the UI retrieves its state and offers FBX download after completion

### Requirement: Use TokenHub motion generation
The system SHALL authenticate upstream requests with a server-only TokenHub API Key, submit to /v1/api/3d/submit using model hy-3d-motion, and query /v1/api/3d/query using the upstream id.

#### Scenario: Provider status mapping
- **WHEN** TokenHub returns queued, in_progress, completed or failed
- **THEN** the existing API exposes WAIT, RUN, DONE or FAIL respectively

#### Scenario: Legacy task compatibility
- **WHEN** a historical task has no provider marker and no cached FBX
- **THEN** it is never queried through TokenHub and the owner is instructed to create a new task

#### Scenario: Server authentication failure
- **WHEN** TokenHub rejects the server's API Key
- **THEN** the client receives an upstream failure with sanitized diagnostics and no API Key or raw provider body
