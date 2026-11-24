# Voice API Surface Specification

## Purpose

Expose Agora ConvoAI/RTC and Bailian WebRTC conversation capabilities.

## Requirements

### Requirement: Retained business endpoints

The application SHALL expose only these business HTTP mappings: POST and DELETE /api/convoai/agents, POST /api/rtc/credentials, POST /api/bailian/realtime/webrtc. Root health and actuator operations remain infrastructure endpoints.

#### Scenario: Discover business handlers

- WHEN application handler mappings are inspected
- THEN no catalog, user configuration, cloning, preview, scene image, echo or motion generation controller is registered.

### Requirement: WebRTC control connection

The application SHALL retain /ws/bailian/realtime-events for WebRTC session commands and events, and SHALL NOT create an upstream Bailian WebSocket connection or relay audio on that control connection.

#### Scenario: WebRTC conversation

- WHEN the browser connects with a valid one-time ticket
- THEN captions, text turns, greetings, action acknowledgement and interruption use the control connection while audio uses WebRTC.

### Requirement: No relational persistence

The runtime and tests SHALL have no relational datasource, mapper, database driver or database initialization requirement.

#### Scenario: Application context starts

- WHEN test credentials are configured
- THEN context startup does not create a DataSource or require a database.

### Requirement: Two browser modes

The browser SHALL offer only Agora and Bailian WebRTC conversation modes. Obsolete saved modes SHALL return to mode selection. Agora SHALL NOT require a server-backed configuration screen.

#### Scenario: Old stored mode

- WHEN obsolete session storage is loaded
- THEN the UI shows mode selection without invoking deleted configuration APIs.
