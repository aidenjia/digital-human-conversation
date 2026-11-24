# Retain Agora and Bailian WebRTC only

## Why

Reduce this service to realtime conversation. Remove the obsolete PCM WebSocket transport, PostgreSQL persistence and all catalog/custom voice/configuration/preview/scene/online-motion business capabilities.

## Scope

Retain the four HTTP method/path mappings and WebRTC event control socket specified in `../../specs/voice-api-surface/spec.md`. Preserve JWT validation, API envelope, logging, metrics, shutdown, Redis ownership/tickets/locking and the existing WebRTC Avatar playback/interrupt protections.

This is a breaking removal: old clients must move to Agora or WebRTC and cannot use obsolete configuration or PCM pages. Archived changes remain historical evidence, not current API contracts. Older non-archived transport proposals are superseded by this change where they require retaining PCM.

No database contents or cloud resources are deleted. External deployment cleanup requires a separate change in `digital-human-cd`.
