## ADDED Requirements

### Requirement: Conversation-only capability surface

The service SHALL expose only the retained method/path mappings, events WebSocket and infrastructure endpoints defined in `../../../../specs/voice-api-surface/spec.md`. Removed domain implementations, relational database wiring and PCM media transport SHALL NOT remain in runtime code.

#### Scenario: Clean startup and route inspection

- **WHEN** the application context starts with test-local infrastructure
- **THEN** no DataSource SHALL be required and the business mappings SHALL equal the retained allowlist

## REMOVED Requirements

### Requirement: Relational and non-conversation business capabilities

**Reason:** PostgreSQL, codegen, companion/dimension catalogs and user configuration, custom voice production, voice preview, scene images, Echo and online motion generation are removed.

**Migration:** Call Agora join directly using configured defaults, or use Bailian WebRTC. Preset WebRTC voice strings remain session parameters, not catalog API resources.

### Requirement: PCM media relay

**Reason:** Browser WebRTC replaces WebSocket PCM media transport.

**Migration:** Exchange SDP using the retained HTTP endpoint and attach the WebRTC events control socket. Re-establish signaling after a control disconnect; never send PCM to that socket.
