# Bailian Realtime WebRTC Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Add a browser WebRTC path to Bailian Qwen-Audio where this service exchanges SDP and orchestrates persona, voice, greeting, and actions, while PCM stays on the existing relay.

**Architecture:** `POST /api/bailian/realtime/webrtc` validates the caller and `model`, posts the Offer with the permanent key, and stores a 60s Redis ticket of userId plus model. `WS /ws/bailian/realtime-events` consumes that ticket and reads `persona`, `voice`, and `enableAction` from the query, the same way the PCM relay does. It reuses the current session.update / greeting / action JSON builders and sends commands back to the client DataChannel. Audio never enters the Pod. `WS /ws/bailian/realtime` is unchanged.

**Tech Stack:** Spring Boot 3.5, existing JWT resource server, OkHttp, Redisson (`DigitalHumanRedisNameMapper`), `BailianRealtimeModels`.

**Spec:** `openspec/changes/bailian-realtime-webrtc/specs/**`

**Design:** `openspec/changes/bailian-realtime-webrtc/design.md`

## Global Constraints

- Permanent DashScope key never in responses or logs. Do not log persona text, JWT, full SDP, or audio payloads.
- Logical Redis key is `bailian-realtime-webrtc:{relayId}`. Do not prefix `digital-human:` in code; `DigitalHumanRedisNameMapper` does that.
- `persona`, `voice`, and `enableAction` are required on the events WebSocket query, with the same blank/default rules as the PCM handshake. `model` is fixed on the SDP POST and stored on the ticket. The events socket MUST NOT let query `model` override it.
- Do not open a Bailian upstream WebSocket from the events socket.
- Do not change the PCM relay handshake or audio forwarding.
- WebRTC path must not fall back to `RealtimePersonaPrompts.PERSONA` or `default-voice`.

## File structure

- Create: `api/bailian/webrtc/BailianRealtimeWebRtcController.java` plus request/response records under `api/bailian/webrtc/dto/`
- Create: `bailian/webrtc/` orchestration (ticket issue, events session)
- Create: `infrastructure/bailian/BailianRealtimeWebRtcSignalingClient.java` for the SDP POST
- Create: `infrastructure/redis/` ticket store, logical prefix `bailian-realtime-webrtc:`
- Modify: WebSocket registration for `/ws/bailian/realtime-events` with its own handler; reuse JWT checks and the PCM parsing of `persona`, `voice`, and `enableAction`; also require `relayId` and ignore query `model`
- Modify: message builders so session.update / greeting / action JSON can be returned as text, not only sent on an upstream `WebSocket`
- Modify: drain registry so events sessions receive `server.draining`
- Test: controller/handshake tests beside existing `BailianRealtimeHandshakeInterceptorTest`
- Modify: `README.md` Bailian Realtime section

---

### Task 1: SDP exchange and ticket

**Files:** Controller, signaling client, Redis ticket store, unit tests.

- [ ] **Step 1:** Write failing tests for missing token, invalid token, and illegal model. Assert the signaling client is not called. A body with only `offerSdp` (no persona or voice) MUST still be valid.
- [ ] **Step 2:** Write a failing test that a blank `model` resolves to `qwen-audio-3.0-realtime-flash`, and an explicit plus model is the upstream query value.
- [ ] **Step 3:** Implement the thin controller, signaling client, and ticket save (TTL 60s). Run the new tests.
- [ ] **Step 4:** Commit when requested.

### Task 2: Events WebSocket

**Files:** New handler, handshake, events session, builder refactor, drain hook, tests.

- [ ] **Step 1:** Write failing handshake tests: matching ticket plus non-blank persona and voice is consumed; second use, expired ticket, mismatched user, and blank persona or voice are rejected; a blank persona MUST NOT consume the ticket; no upstream WebSocket is opened; query `model` does not override the ticket model.
- [ ] **Step 2:** Write a failing test that a forwarded `session.created` produces `session.update` containing the handshake persona and voice, and that `response.audio.delta` does not produce `audio.delta`.
- [ ] **Step 3:** Implement the events socket and JSON command path. Keep greeting-then-tools order when `enableAction` is true.
- [ ] **Step 4:** Add a regression that `WS /ws/bailian/realtime` still requires its own query and still starts the PCM upstream.
- [ ] **Step 5:** Commit when requested.

### Task 3: Docs and verify

**Files:** `README.md`.

- [ ] **Step 1:** Document `POST /api/bailian/realtime/webrtc`, then `WS /ws/bailian/realtime-events`, then `setRemoteDescription`, DataChannel name `oai-events`, and that PCM relay remains.
- [ ] **Step 2:** Run `mvn spotless:apply` and `mvn verify`.
- [ ] **Step 3:** During manual call, confirm flash and plus accept `smart_turn` on WebRTC. If Bailian rejects it, switch only this path to `server_vad` and note it in this change.
- [ ] **Step 4:** Commit when requested.
