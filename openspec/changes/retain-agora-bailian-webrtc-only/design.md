# Design

Agora join uses one configured session profile for UE5/MMD/Unity; the type still reaches the downstream Agent. No user configuration lookup is performed. Tencent credentials remain because Agora still uses its Tencent TTS vendor. Redis retains existing ownership, ticket and Lock4j responsibilities.

Bailian audio goes directly through browser WebRTC RTP. SDP exchange issues a single-use authenticated ticket. The events socket keeps session configuration, transcripts, text input, actions, greeting, interruption, heartbeat and drain, with pure JSON command construction instead of an upstream media WebSocket client. It never carries audio deltas.

The UI exposes only Agora and WebRTC. It removes database-backed configuration/catalog screens and the online motion generation panel while retaining local Avatar playback, local preparation tools and ignored VRM assets. Obsolete stored modes fall back to a retained mode.

Current specifications for removed persistence and business domains are deleted; archive files are not modified. API surface and no-DataSource regression tests prevent reintroduction. Existing API and Avatar behavior tests remain, with obsolete Echo fixtures moved to test-only coverage.

Deployment follow-up outside this repository: remove `SPRING_DATASOURCE_*`, PostgreSQL/pgpool assumptions, COS/VRS/voice-preview and motion-generation settings for this service. Keep Redis, Agora, Tencent TTS vendor, Agent and WebRTC settings. Never apply DROP or remove shared databases as part of this source cleanup.
