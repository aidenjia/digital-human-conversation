# Bailian Realtime WS Relay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Add authenticated server-side WebSocket relay for Qwen-Audio Realtime, with heartbeats and graceful drain, while keeping Agora RTC credentials for client 3A.

**Architecture:** Thin WS handler → `BailianRealtimeRelaySession` → infrastructure Bailian WS client. Session state is connection-local. Drain iterates in-process sessions.

**Tech Stack:** Spring Boot 3.5 WebSocket, existing JWT resource server, OkHttp/Java WS client, `BailianRealtimeProperties`.

**Spec:** `openspec/changes/bailian-realtime-ws-relay/specs/**`

## Global Constraints

- Permanent DashScope key never in client responses or logs.
- Formal path: RTC HTTP + this relay WS; temporary-key direct Bailian is transitional.
- No cross-pod session resume; reconnect = new session.
- Heartbeat + drain are required, not optional polish.
- Follow project layering: api/websocket adapter thin; infrastructure owns I/O.

---

### Task 1: Properties & Security scaffolding

**Files:**
- Modify: `BailianRealtimeProperties.java`, `application.yml`
- Modify: Security config for `/ws/bailian/realtime`

- [ ] **Step 1:** Add config fields (heartbeat, drain, session defaults).
- [ ] **Step 2:** Wire Security for WS JWT.
- [ ] **Step 3:** Commit when requested.

### Task 2: Upstream client + relay session + endpoint

**Files:**
- Create: infrastructure Bailian WS client
- Create: relay session + handler
- Test: auth reject, mapping unit tests

- [ ] **Step 1:** Implement upstream client.
- [ ] **Step 2:** Implement relay session + simplified protocol.
- [ ] **Step 3:** Expose endpoint; write auth/mapping tests.
- [ ] **Step 4:** Commit when requested.

### Task 3: Heartbeat + drain + ops docs

**Files:**
- Relay session heartbeat
- Shutdown/drain component
- Modify: `docs/k8s-graceful-shutdown-requirements.md`

- [ ] **Step 1:** Heartbeat + idle timeout.
- [ ] **Step 2:** Drain path + tests.
- [ ] **Step 3:** Update ops docs with recommended grace periods.
- [ ] **Step 4:** Commit when requested.

### Task 4: README + conversation-ui cutover

**Files:**
- `README.md`
- `conversation-ui` WS client path

- [ ] **Step 1:** Document formal sequence.
- [ ] **Step 2:** Point UI at relay; remove Vite Bailian proxy dependency.
- [ ] **Step 3:** `mvn verify` + manual checklist.
- [ ] **Step 4:** Commit when requested.
